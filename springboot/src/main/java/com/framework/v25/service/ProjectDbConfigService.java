package com.framework.v25.service;

import com.framework.v25.dto.ProjectDbConfigDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProjectDbConfigService {

    private final RestTemplate restTemplate;
    private final String       baseUrl;

    @Autowired
    public ProjectDbConfigService(
            RestTemplate restTemplate,
            @Value("${postgrest.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl      = baseUrl;
    }

    // ── Get config for a project ──────────────────────────────

    public Optional<ProjectDbConfigDto.Response> getByProject(UUID projectId) {
        String url = baseUrl + "/project_db_configs?project_id=eq." + projectId + "&limit=1";
        List<Map<String, Object>> rows = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers()),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
        if (rows == null || rows.isEmpty()) return Optional.empty();
        return Optional.of(toResponse(rows.get(0)));
    }

    // ── Save (create or update) ───────────────────────────────

    public ProjectDbConfigDto.Response save(UUID projectId, ProjectDbConfigDto.Request req) {
        Optional<ProjectDbConfigDto.Response> existing = getByProject(projectId);

        if (existing.isPresent()) {
            return update(existing.get().getId(), projectId, req);
        } else {
            return create(projectId, req);
        }
    }

    private ProjectDbConfigDto.Response create(UUID projectId, ProjectDbConfigDto.Request req) {
        HttpHeaders h = headers();
        h.set("Prefer", "return=representation");

        Map<String, Object> payload = buildPayload(projectId, req, true);
        ResponseEntity<Map[]> res = restTemplate.exchange(
                baseUrl + "/project_db_configs", HttpMethod.POST,
                new HttpEntity<>(payload, h), Map[].class);

        Map[] body = res.getBody();
        if (body == null || body.length == 0) throw new RuntimeException("Failed to save DB config");
        return toResponse(body[0]);
    }

    private ProjectDbConfigDto.Response update(UUID id, UUID projectId, ProjectDbConfigDto.Request req) {
        HttpHeaders h = headers();
        h.set("Prefer", "return=representation");

        Map<String, Object> payload = buildPayload(projectId, req, false);
        // Only update password if a new one was provided
        if (req.getDbPassword() == null || req.getDbPassword().isBlank()) {
            payload.remove("db_password");
        }

        ResponseEntity<Map[]> res = restTemplate.exchange(
                baseUrl + "/project_db_configs?id=eq." + id, HttpMethod.PATCH,
                new HttpEntity<>(payload, h), Map[].class);

        Map[] body = res.getBody();
        if (body == null || body.length == 0) throw new RuntimeException("DB config not found");
        return toResponse(body[0]);
    }

    // ── Delete ────────────────────────────────────────────────

    public void delete(UUID projectId) {
        restTemplate.exchange(
                baseUrl + "/project_db_configs?project_id=eq." + projectId,
                HttpMethod.DELETE, new HttpEntity<>(headers()), Void.class);
    }

    // ── Test connection ───────────────────────────────────────

    public ProjectDbConfigDto.TestResult testConnection(UUID projectId, ProjectDbConfigDto.Request req) {
        long start = System.currentTimeMillis();

        // Determine host, port, name — prefer from request, fallback to saved config
        String  host     = req.getDbHost();
        int     port     = req.getDbPort() != null ? req.getDbPort() : 5432;
        String  name     = req.getDbName();
        String  user     = req.getDbUsername();
        String  password = req.getDbPassword();
        boolean ssl      = Boolean.TRUE.equals(req.getSslEnabled());

        // If password not provided, load from saved config
        if (password == null || password.isBlank()) {
            password = getPasswordFromDb(projectId);
        }

        String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%d/%s%s",
                host, port, name, ssl ? "?sslmode=require" : ""
        );

        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
                long latency = System.currentTimeMillis() - start;
                // Update test status in DB
                updateTestStatus(projectId, "SUCCESS");
                return ProjectDbConfigDto.TestResult.builder()
                        .success(true)
                        .message("Connection successful — " + conn.getMetaData().getDatabaseProductVersion())
                        .latencyMs(latency)
                        .build();
            }
        } catch (Exception e) {
            log.warn("DB connection test failed for project {}: {}", projectId, e.getMessage());
            updateTestStatus(projectId, "FAILED");
            return ProjectDbConfigDto.TestResult.builder()
                    .success(false)
                    .message("Connection failed: " + simplifyError(e.getMessage()))
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private void updateTestStatus(UUID projectId, String status) {
        try {
            Map<String, Object> patch = new LinkedHashMap<>();
            patch.put("test_status", status);
            patch.put("last_tested", OffsetDateTime.now().toString());
            restTemplate.exchange(
                    baseUrl + "/project_db_configs?project_id=eq." + projectId,
                    HttpMethod.PATCH, new HttpEntity<>(patch, headers()), Void.class);
        } catch (Exception e) {
            log.warn("Failed to update test status: {}", e.getMessage());
        }
    }

    private String getPasswordFromDb(UUID projectId) {
        try {
            String url = baseUrl + "/project_db_configs?project_id=eq." + projectId
                    + "&select=db_password&limit=1";
            List<Map<String, Object>> rows = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            if (rows != null && !rows.isEmpty()) {
                Object pw = rows.get(0).get("db_password");
                return pw != null ? pw.toString() : "";
            }
        } catch (Exception e) {
            log.warn("Failed to load saved password: {}", e.getMessage());
        }
        return "";
    }

    private Map<String, Object> buildPayload(UUID projectId, ProjectDbConfigDto.Request req, boolean includePassword) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("project_id",  projectId.toString());
        m.put("db_host",     req.getDbHost());
        m.put("db_port",     req.getDbPort() != null ? req.getDbPort() : 5432);
        m.put("db_name",     req.getDbName());
        m.put("db_username", req.getDbUsername());
        m.put("db_schema",   req.getDbSchema() != null ? req.getDbSchema() : "public");
        m.put("ssl_enabled", Boolean.TRUE.equals(req.getSslEnabled()));
        m.put("is_active",   Boolean.TRUE.equals(req.getIsActive()));
        m.put("test_status", "UNTESTED");
        if (includePassword && req.getDbPassword() != null) {
            m.put("db_password", req.getDbPassword());
        }
        return m;
    }

    private String simplifyError(String msg) {
        if (msg == null) return "Unknown error";
        if (msg.contains("Connection refused"))  return "Connection refused — check host and port";
        if (msg.contains("password"))            return "Authentication failed — check username/password";
        if (msg.contains("does not exist"))      return "Database does not exist";
        if (msg.contains("timeout"))             return "Connection timed out — check host reachability";
        return msg.length() > 120 ? msg.substring(0, 120) + "…" : msg;
    }

    @SuppressWarnings("unchecked")
    private ProjectDbConfigDto.Response toResponse(Map<String, Object> row) {
        return ProjectDbConfigDto.Response.builder()
                .id(uuid(row.get("id")))
                .projectId(uuid(row.get("project_id")))
                .dbHost(str(row.get("db_host")))
                .dbPort(row.get("db_port") instanceof Number n ? n.intValue() : 5432)
                .dbName(str(row.get("db_name")))
                .dbUsername(str(row.get("db_username")))
                .dbSchema(str(row.get("db_schema")))
                .sslEnabled(row.get("ssl_enabled") instanceof Boolean b ? b : false)
                .isActive(row.get("is_active") instanceof Boolean b ? b : true)
                .testStatus(str(row.get("test_status")))
                .lastTested(time(row.get("last_tested")))
                .createdAt(time(row.get("created_at")))
                .updatedAt(time(row.get("updated_at")))
                .build();
    }

    private UUID uuid(Object v) { return v != null ? UUID.fromString(v.toString()) : null; }
    private String str(Object v) { return v != null ? v.toString() : null; }
    private OffsetDateTime time(Object v) {
        try { return v != null ? OffsetDateTime.parse(v.toString()) : null; }
        catch (Exception e) { return null; }
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        return h;
    }

    /**
     * Returns the plain-text password for a project DB config.
     * Used internally by SchemaService for JDBC connections.
     * Never exposed via API response.
     */
    public String getPasswordForJdbc(UUID projectId) {
        return getPasswordFromDb(projectId);
    }
}
// This method is called by SchemaService to get plain password for JDBC
// (Response DTO never exposes the password)
