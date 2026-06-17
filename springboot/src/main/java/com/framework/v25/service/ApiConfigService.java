package com.framework.v25.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.framework.v25.dto.ApiConfigDto;
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
public class ApiConfigService {

    private final RestTemplate  restTemplate;
    private final String        baseUrl;
    private final ObjectMapper  objectMapper;

    @Autowired
    public ApiConfigService(
            RestTemplate restTemplate,
            @Value("${postgrest.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl      = baseUrl;
        this.objectMapper = new ObjectMapper();
    }

    // ── GET all ───────────────────────────────────────────────

    public List<ApiConfigDto.Response> getAll(UUID projectId, String configType) {
        StringBuilder url = new StringBuilder(baseUrl + "/fw_configs?order=created_at.desc&select=*");
        if (projectId  != null)               url.append("&project_id=eq.").append(projectId);
        if (configType != null && !configType.isBlank()) url.append("&config_type=eq.").append(configType);

        List<Map<String, Object>> rows = restTemplate.exchange(
                url.toString(), HttpMethod.GET, new HttpEntity<>(headers()),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
        return rows == null ? List.of()
                : rows.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── GET by ID ─────────────────────────────────────────────

    public ApiConfigDto.Response getById(UUID id) {
        String url = baseUrl + "/fw_configs?id=eq." + id + "&limit=1";
        List<Map<String, Object>> rows = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers()),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
        if (rows == null || rows.isEmpty()) throw new RuntimeException("Config not found");
        return toResponse(rows.get(0));
    }

    // ── CREATE ────────────────────────────────────────────────

    public ApiConfigDto.Response create(ApiConfigDto.Request req) {
        HttpHeaders h = headers();
        h.set("Prefer", "return=representation");
        ResponseEntity<Map[]> res = restTemplate.exchange(
                baseUrl + "/fw_configs", HttpMethod.POST,
                new HttpEntity<>(buildPayload(req), h), Map[].class);
        Map[] body = res.getBody();
        if (body == null || body.length == 0) throw new RuntimeException("Failed to create config");
        return toResponse(body[0]);
    }

    // ── UPDATE ────────────────────────────────────────────────

    public ApiConfigDto.Response update(UUID id, ApiConfigDto.Request req) {
        HttpHeaders h = headers();
        h.set("Prefer", "return=representation");
        Map<String, Object> payload = buildPayload(req);
        payload.put("updated_at", OffsetDateTime.now().toString());
        ResponseEntity<Map[]> res = restTemplate.exchange(
                baseUrl + "/fw_configs?id=eq." + id, HttpMethod.PATCH,
                new HttpEntity<>(payload, h), Map[].class);
        Map[] body = res.getBody();
        if (body == null || body.length == 0) throw new RuntimeException("Config not found");
        return toResponse(body[0]);
    }

    // ── DELETE ────────────────────────────────────────────────

    public void delete(UUID id) {
        restTemplate.exchange(
                baseUrl + "/fw_configs?id=eq." + id, HttpMethod.DELETE,
                new HttpEntity<>(headers()), Void.class);
    }

    // ── Payload builder ───────────────────────────────────────

    private Map<String, Object> buildPayload(ApiConfigDto.Request req) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (req.getProjectId()  != null) m.put("project_id",   req.getProjectId().toString());
        m.put("alias_name",   req.getAliasName());
        m.put("config_type",  req.getConfigType());
        if (req.getSchemaName()  != null) m.put("schema_name",  req.getSchemaName());
        if (req.getTableName()   != null) m.put("table_name",   req.getTableName());
        if (req.getDisplayType() != null) m.put("display_type", req.getDisplayType());
        m.put("is_active",    req.getIsActive() != null ? req.getIsActive() : true);

        // ── IMPORTANT ─────────────────────────────────────────
        // The configuration column is JSONB in PostgreSQL.
        // PostgREST requires the value sent as a raw JSON object (Map),
        // NOT a JSON-encoded string — otherwise it stores a double-encoded
        // string literal instead of a proper JSONB value.
        // Angular sends it as a JSON string → parse it back to a Map here.
        String configStr = req.getConfiguration();
        if (configStr != null && !configStr.isBlank()) {
            try {
                Object parsed = objectMapper.readValue(configStr, Object.class);
                m.put("configuration", parsed);        // send as Object → PostgREST stores as JSONB
            } catch (JsonProcessingException e) {
                log.warn("configuration is not valid JSON, storing as-is: {}", e.getMessage());
                m.put("configuration", configStr);     // fallback: store raw string
            }
        }

        return m;
    }

    // ── Response mapper ───────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ApiConfigDto.Response toResponse(Map<String, Object> row) {
        return ApiConfigDto.Response.builder()
                .id(parseUUID(row.get("id")))
                .projectId(parseUUID(row.get("project_id")))
                .aliasName((String)  row.get("alias_name"))
                .configType((String) row.get("config_type"))
                .schemaName((String) row.get("schema_name"))
                .tableName((String)  row.get("table_name"))
                .displayType((String)row.get("display_type"))
                // ── FIX ───────────────────────────────────────
                // configuration is JSONB → PostgREST deserializes it to
                // LinkedHashMap (not String). Serialize back to JSON string
                // so Angular receives a clean JSON string it can JSON.parse().
                .configuration(toJsonString(row.get("configuration")))
                .isActive(row.get("is_active") instanceof Boolean b ? b : true)
                .createdAt(parseTime(row.get("created_at")))
                .updatedAt(parseTime(row.get("updated_at")))
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Converts any value to a JSON string.
     * - If it is already a String → return as-is
     * - If it is a Map / List (JSONB deserialized by Jackson) → serialize to JSON
     * - If null → return null
     */
    private String toJsonString(Object value) {
        if (value == null)           return null;
        if (value instanceof String) return (String) value;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize configuration to JSON string: {}", e.getMessage());
            return value.toString();
        }
    }

    private UUID parseUUID(Object v) {
        return v != null ? UUID.fromString(v.toString()) : null;
    }

    private OffsetDateTime parseTime(Object v) {
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