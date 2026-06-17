package com.framework.v25.controller;

import com.framework.v25.dto.ApiConfigDto;
import com.framework.v25.service.ApiConfigService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/api-configs")
@Slf4j
public class ApiConfigController {

    private final ApiConfigService apiConfigService;

    @Autowired
    public ApiConfigController(ApiConfigService apiConfigService) {
        this.apiConfigService = apiConfigService;
    }

    @GetMapping
    public ResponseEntity<List<ApiConfigDto.Response>> getAll(
            @RequestParam(value = "project_id", required = false) UUID projectId,
            @RequestParam(value = "config_type", required = false) String configType) {
        return ResponseEntity.ok(apiConfigService.getAll(projectId, configType));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiConfigDto.Response> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(apiConfigService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ApiConfigDto.Response> create(@Valid @RequestBody ApiConfigDto.Request req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiConfigService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiConfigDto.Response> update(
            @PathVariable UUID id, @Valid @RequestBody ApiConfigDto.Request req) {
        return ResponseEntity.ok(apiConfigService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        apiConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
