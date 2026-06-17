package com.framework.v25.controller;

import com.framework.v25.dto.ColumnDto;
import com.framework.v25.service.SchemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schema")
@Slf4j
public class SchemaController {

    private final SchemaService schemaService;

    @Autowired
    public SchemaController(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    /**
     * GET /api/schema/columns?schema=public&table=users&projectId=xxx
     * Returns column names and data types for a table.
     * If projectId is provided, queries the project's own configured database.
     * Otherwise falls back to the platform database.
     */
    @GetMapping("/columns")
    public ResponseEntity<List<ColumnDto>> getColumns(
            @RequestParam String schema,
            @RequestParam String table,
            @RequestParam(required = false) UUID projectId) {
        return ResponseEntity.ok(schemaService.getColumns(schema, table, projectId));
    }
}
