package com.framework.v25.service;

import com.framework.v25.dto.BuildLogDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BuildLogService {

    private final RestTemplate restTemplate;
    private final String       baseUrl;

    @Autowired
    public BuildLogService(
            RestTemplate restTemplate,
            @Value("${postgrest.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl      = baseUrl;
    }

    /**
     * Returns last N builds for a project, newest first.
     * PostgREST query: /build_logs?project_id=eq.{id}&order=started_at.desc&limit=5
     */
    public List<BuildLogDto> getLastBuilds(UUID projectId, int limit) {
        String url = baseUrl
                + "/build_logs?project_id=eq." + projectId
                + "&order=started_at.desc"
                + "&limit=" + Math.min(limit, 20)
                + "&select=id,project_id,status,commit_hash,branch,logs,triggered_by,started_at,finished_at";

        List<Map<String, Object>> rows = restTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();

        return rows == null ? List.of()
                : rows.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Single build log by ID.
     */
    public BuildLogDto getById(UUID id) {
        String url = baseUrl + "/build_logs?id=eq." + id + "&limit=1";
        List<Map<String, Object>> rows = restTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
        if (rows == null || rows.isEmpty()) throw new RuntimeException("Build log not found");
        return toDto(rows.get(0));
    }

    // ── Mapper ────────────────────────────────────────────────

    private BuildLogDto toDto(Map<String, Object> row) {
        return BuildLogDto.builder()
                .id(uuid(row.get("id")))
                .projectId(uuid(row.get("project_id")))
                .status(str(row.get("status")))
                .commitHash(str(row.get("commit_hash")))
                .branch(str(row.get("branch")))
                .logs(str(row.get("logs")))
                .triggeredBy(str(row.get("triggered_by")))
                .startedAt(time(row.get("started_at")))
                .finishedAt(time(row.get("finished_at")))
                .build();
    }

    private UUID uuid(Object v) {
        return v != null ? UUID.fromString(v.toString()) : null;
    }

    private String str(Object v) {
        return v != null ? v.toString() : null;
    }

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
}
