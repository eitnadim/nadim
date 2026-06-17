package com.framework.v25.controller;

import com.framework.v25.dto.ProjectDbConfigDto;
import com.framework.v25.service.ProjectDbConfigService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/db-config")
@Slf4j
public class ProjectDbConfigController {

    private final ProjectDbConfigService service;

    @Autowired
    public ProjectDbConfigController(ProjectDbConfigService service) {
        this.service = service;
    }

    /** GET /api/projects/{projectId}/db-config */
    @GetMapping
    public ResponseEntity<ProjectDbConfigDto.Response> get(@PathVariable UUID projectId) {
        return service.getByProject(projectId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /** POST /api/projects/{projectId}/db-config  — create or update */
    @PostMapping
    public ResponseEntity<ProjectDbConfigDto.Response> save(
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectDbConfigDto.Request req) {
        return ResponseEntity.ok(service.save(projectId, req));
    }

    /** POST /api/projects/{projectId}/db-config/test  — test connection */
    @PostMapping("/test")
    public ResponseEntity<ProjectDbConfigDto.TestResult> test(
            @PathVariable UUID projectId,
            @RequestBody ProjectDbConfigDto.Request req) {
        return ResponseEntity.ok(service.testConnection(projectId, req));
    }

    /** DELETE /api/projects/{projectId}/db-config */
    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable UUID projectId) {
        service.delete(projectId);
        return ResponseEntity.noContent().build();
    }
}
