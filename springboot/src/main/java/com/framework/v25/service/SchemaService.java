package com.framework.v25.service;

import com.framework.v25.dto.ColumnDto;
import com.framework.v25.dto.ProjectDbConfigDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SchemaService {

    private final JdbcTemplate             platformJdbc;
    private final ProjectDbConfigService   dbConfigService;

    @Autowired
    public SchemaService(JdbcTemplate platformJdbc, ProjectDbConfigService dbConfigService) {
        this.platformJdbc    = platformJdbc;
        this.dbConfigService = dbConfigService;
    }

    /**
     * Fetch columns for a table.
     * If projectId is provided and the project has a DB config, use
     * the project's own database. Otherwise fall back to the platform DB.
     */
    public List<ColumnDto> getColumns(String schema, String table, UUID projectId) {
        JdbcTemplate jdbc = resolveJdbc(projectId);
        return queryColumns(jdbc, schema, table);
    }

    // ── Resolve which DB to query ─────────────────────────────

    private JdbcTemplate resolveJdbc(UUID projectId) {
        if (projectId == null) return platformJdbc;

        Optional<ProjectDbConfigDto.Response> cfg = dbConfigService.getByProject(projectId);
        if (cfg.isEmpty()) return platformJdbc;

        ProjectDbConfigDto.Response c = cfg.get();
        try {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("org.postgresql.Driver");
            ds.setUrl(String.format(
                "jdbc:postgresql://%s:%d/%s%s",
                c.getDbHost(), c.getDbPort(), c.getDbName(),
                Boolean.TRUE.equals(c.getSslEnabled()) ? "?sslmode=require" : ""
            ));
            ds.setUsername(c.getDbUsername());
            // password fetched separately — not in response DTO
            ds.setPassword(dbConfigService.getPasswordForJdbc(
                    UUID.fromString(c.getProjectId().toString())));
            log.info("Using project DB for schema query: {}:{}/{}",
                    c.getDbHost(), c.getDbPort(), c.getDbName());
            return new JdbcTemplate(ds);
        } catch (Exception e) {
            log.warn("Failed to connect to project DB, falling back to platform DB: {}", e.getMessage());
            return platformJdbc;
        }
    }

    // ── Column query ──────────────────────────────────────────

    private List<ColumnDto> queryColumns(JdbcTemplate jdbc, String schema, String table) {
        String sql = """
            SELECT
                column_name,
                data_type,
                is_nullable
            FROM information_schema.columns
            WHERE table_schema = ?
              AND table_name   = ?
            ORDER BY ordinal_position
            """;

        return jdbc.query(sql, new Object[]{schema, table}, (rs, i) ->
                ColumnDto.builder()
                        .name(rs.getString("column_name"))
                        .type(simplifyType(rs.getString("data_type")))
                        .nullable("YES".equals(rs.getString("is_nullable")))
                        .build()
        );
    }

    // ── Type simplifier ───────────────────────────────────────

    private String simplifyType(String pgType) {
        if (pgType == null) return "unknown";
        return switch (pgType.toLowerCase()) {
            case "integer","bigint","smallint","serial","bigserial" -> "integer";
            case "numeric","decimal","real","double precision"      -> "numeric";
            case "boolean"                                          -> "boolean";
            case "date"                                             -> "date";
            case "timestamp without time zone",
                 "timestamp with time zone", "timestamp"           -> "timestamp";
            case "uuid"                                             -> "uuid";
            case "text","character varying","varchar",
                 "char","character","name"                         -> "varchar";
            case "jsonb","json"                                     -> "jsonb";
            default                                                 -> pgType;
        };
    }
}
