package com.framework.v25.controller;

import com.framework.v25.dto.BuildLogDto;
import com.framework.v25.service.BuildLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/builds")
@Slf4j
public class BuildLogController {

    private final BuildLogService buildLogService;

    @Autowired
    public BuildLogController(BuildLogService buildLogService) {
        this.buildLogService = buildLogService;
    }

    /**
     * GET /api/projects/{projectId}/builds?limit=5
     * Returns last N build logs for the project, latest first.
     * Used by the dashboard monitor section.
     */
    @GetMapping
    public ResponseEntity<List<BuildLogDto>> getBuildHistory(
            @PathVariable UUID projectId,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(buildLogService.getLastBuilds(projectId, limit));
    }

    /**
     * GET /api/projects/{projectId}/builds/{id}
     * Returns a single build log with full log text.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BuildLogDto> getById(
            @PathVariable UUID projectId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(buildLogService.getById(id));
    }
}
