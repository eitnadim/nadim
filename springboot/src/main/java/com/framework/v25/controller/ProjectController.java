package com.framework.v25.controller;

import com.framework.v25.dto.*;
import com.framework.v25.service.ProjectService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    @Autowired
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // GET /api/projects
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAll(
            @RequestParam(value = "owner_id", required = false) UUID ownerId) {
        return ResponseEntity.ok(projectService.getAll(ownerId));
    }

    // GET /api/projects/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    // POST /api/projects
    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(request));
    }

    // DELETE /api/projects/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/projects/{id}/build
    @PostMapping("/{id}/build")
    public ResponseEntity<BuildResult> build(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.build(id));
    }

    // POST /api/projects/{id}/deploy
    @PostMapping("/{id}/deploy")
    public ResponseEntity<DeployResult> deploy(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.deploy(id));
    }

    // GET /api/projects/{id}/download/angular
    // Downloads the Angular source code ZIP — user works on this locally
    @GetMapping("/{id}/download/angular")
    public ResponseEntity<Resource> downloadAngular(@PathVariable UUID id) throws IOException {
        Resource resource = projectService.downloadAngularZip(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"angular-" + id + ".zip\"")
                .body(resource);
    }

    // POST /api/projects/{id}/classes/upload
    @PostMapping("/{id}/classes/upload")
    public ResponseEntity<UploadResponse> uploadClass(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(projectService.uploadCustomClass(id, file));
    }

    // GET /api/projects/{id}/git/invitation-status?username=john
    @GetMapping("/{id}/git/invitation-status")
    public ResponseEntity<Map<String, String>> invitationStatus(
            @PathVariable UUID id,
            @RequestParam String username) {
        String status = projectService.getInvitationStatus(id, username);
        return ResponseEntity.ok(Map.of("status", status));
    }

    // POST /api/projects/{id}/git/resend-invitation?username=john
    @PostMapping("/{id}/git/resend-invitation")
    public ResponseEntity<Map<String, Object>> resendInvitation(
            @PathVariable UUID id,
            @RequestParam String username) {
        boolean ok = projectService.resendInvitation(id, username);
        return ResponseEntity.ok(Map.of("success", ok));
    }

    // POST /api/projects/{id}/git/connect
    // Connects an existing project to GitHub (when token was missing at creation)
    @PostMapping("/{id}/git/connect")
    public ResponseEntity<ProjectResponse> connectGitHub(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.connectGitHub(id));
    }

}