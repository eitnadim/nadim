package com.framework.v25.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.framework.v25.dto.postgrest.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class PostgRestClient {

    private final RestTemplate restTemplate;
    private final String       baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public PostgRestClient(
            RestTemplate restTemplate,
            @Value("${postgrest.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl      = baseUrl;
    }

    // ── Users ─────────────────────────────────────────────────

    public Optional<UserRow> findUserByEmail(String email) {
        String url = baseUrl + "/users?email=eq." + email
                + "&select=id,name,email,password_hash,role&limit=1";
        List<UserRow> rows = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers()),
                new ParameterizedTypeReference<List<UserRow>>() {}
        ).getBody();
        return (rows != null && !rows.isEmpty()) ? Optional.of(rows.get(0)) : Optional.empty();
    }

    // ── Projects ──────────────────────────────────────────────

    public List<ProjectRow> getAllProjects() {
        List<ProjectRow> rows = restTemplate.exchange(
                baseUrl + "/projects?order=updated_at.desc&select=*",
                HttpMethod.GET, new HttpEntity<>(headers()),
                new ParameterizedTypeReference<List<ProjectRow>>() {}
        ).getBody();
        return rows != null ? rows : List.of();
    }

    public List<ProjectRow> getProjectsByOwner(UUID ownerId) {
        List<ProjectRow> rows = restTemplate.exchange(
                baseUrl + "/projects?owner_id=eq." + ownerId + "&order=updated_at.desc&select=*",
                HttpMethod.GET, new HttpEntity<>(headers()),
                new ParameterizedTypeReference<List<ProjectRow>>() {}
        ).getBody();
        return rows != null ? rows : List.of();
    }

    public Optional<ProjectRow> getProjectById(UUID id) {
        List<ProjectRow> rows = restTemplate.exchange(
                baseUrl + "/projects?id=eq." + id + "&select=*&limit=1",
                HttpMethod.GET, new HttpEntity<>(headers()),
                new ParameterizedTypeReference<List<ProjectRow>>() {}
        ).getBody();
        return (rows != null && !rows.isEmpty()) ? Optional.of(rows.get(0)) : Optional.empty();
    }

    // ── FIX: Use Map instead of @Builder POJO ─────────────────
    // @JsonProperty on Lombok @Builder fields does NOT serialize
    // to snake_case correctly — the builder uses camelCase internally.
    // Solution: build a Map with the exact column names PostgREST expects.

    public ProjectRow createProject(CreateProjectPayload p) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (p.getName()              != null) body.put("name",                p.getName());
        if (p.getDescription()       != null) body.put("description",         p.getDescription());
        if (p.getJavaVersion()       != null) body.put("java_version",        p.getJavaVersion());
        if (p.getBuildTool()         != null) body.put("build_tool",          p.getBuildTool());
        if (p.getGroupId()           != null) body.put("group_id",            p.getGroupId());
        if (p.getArtifactId()        != null) body.put("artifact_id",         p.getArtifactId());
        if (p.getPackageName()       != null) body.put("package_name",        p.getPackageName());
        if (p.getSpringBootVersion() != null) body.put("spring_boot_version", p.getSpringBootVersion());
        if (p.getGitInviteEmail()    != null) body.put("git_invite_email",    p.getGitInviteEmail());
        if (p.getGitRepoName()       != null) body.put("git_repo_name",       p.getGitRepoName());
        if (p.getStatus()            != null) body.put("status",              p.getStatus());

        HttpHeaders h = preferHeaders();
        ResponseEntity<ProjectRow[]> res = restTemplate.exchange(
                baseUrl + "/projects", HttpMethod.POST,
                new HttpEntity<>(body, h), ProjectRow[].class);
        ProjectRow[] rows = res.getBody();
        return (rows != null && rows.length > 0) ? rows[0] : null;
    }

    public void patchProject(UUID id, PatchProjectPayload p) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (p.getStatus()      != null) body.put("status",        p.getStatus());
        if (p.getGitRepoUrl()  != null) body.put("git_repo_url",  p.getGitRepoUrl());
        if (p.getGitRepoName() != null) body.put("git_repo_name", p.getGitRepoName());
        if (body.isEmpty()) return;

        // Tunnel PATCH over POST with X-HTTP-Method-Override
        HttpHeaders h = headers();
        h.set("X-HTTP-Method-Override", "PATCH");
        restTemplate.exchange(
                baseUrl + "/projects?id=eq." + id, HttpMethod.POST,
                new HttpEntity<>(body, h), Void.class);
    }

    public void deleteProject(UUID id) {
        restTemplate.exchange(
                baseUrl + "/projects?id=eq." + id, HttpMethod.DELETE,
                new HttpEntity<>(headers()), Void.class);
    }

    // ── Build logs ────────────────────────────────────────────

    public void createBuildLog(CreateBuildLogPayload p) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (p.getProjectId()  != null) body.put("project_id",  p.getProjectId().toString());
        if (p.getStatus()     != null) body.put("status",      p.getStatus());
        if (p.getLogs()       != null) body.put("logs",        p.getLogs());
        if (p.getCommitHash() != null) body.put("commit_hash", p.getCommitHash());
        if (p.getBranch()     != null) body.put("branch",      p.getBranch());
        restTemplate.exchange(baseUrl + "/build_logs", HttpMethod.POST,
                new HttpEntity<>(body, headers()), Void.class);
    }

    // ── Deploy logs ───────────────────────────────────────────

    public void createDeployLog(CreateDeployLogPayload p) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (p.getProjectId() != null) body.put("project_id", p.getProjectId().toString());
        if (p.getStatus()    != null) body.put("status",     p.getStatus());
        if (p.getDeployUrl() != null) body.put("deploy_url", p.getDeployUrl());
        if (p.getLogs()      != null) body.put("logs",       p.getLogs());
        restTemplate.exchange(baseUrl + "/deploy_logs", HttpMethod.POST,
                new HttpEntity<>(body, headers()), Void.class);
    }

    // ── Custom classes ────────────────────────────────────────

    public void createCustomClassRecord(CreateCustomClassPayload p) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (p.getProjectId()  != null) body.put("project_id", p.getProjectId().toString());
        if (p.getFileName()   != null) body.put("file_name",  p.getFileName());
        if (p.getFilePath()   != null) body.put("file_path",  p.getFilePath());
        if (p.getClassName()  != null) body.put("class_name", p.getClassName());
        if (p.getFileSize()   != null) body.put("file_size",  p.getFileSize());
        restTemplate.exchange(baseUrl + "/custom_classes", HttpMethod.POST,
                new HttpEntity<>(body, headers()), Void.class);
    }

    // ── Git invitations ───────────────────────────────────────

    public void createGitInvitation(CreateGitInvitationPayload p) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (p.getProjectId()    != null) body.put("project_id",    p.getProjectId().toString());
        if (p.getInvitedEmail() != null) body.put("invited_email", p.getInvitedEmail());
        if (p.getStatus()       != null) body.put("status",        p.getStatus());
        restTemplate.exchange(baseUrl + "/git_invitations", HttpMethod.POST,
                new HttpEntity<>(body, headers()), Void.class);
    }

    // ── Headers ───────────────────────────────────────────────

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        return h;
    }

    private HttpHeaders preferHeaders() {
        HttpHeaders h = headers();
        h.set("Prefer", "return=representation");
        return h;
    }
}