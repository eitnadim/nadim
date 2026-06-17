package com.framework.v25.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ApiConfigDto {

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "aliasName is required")
        private String aliasName;

        @NotBlank(message = "configType is required")
        private String configType;   // form_crud | grid | chart | custom_sql

        private String schemaName;
        private String tableName;
        private String displayType;

        @NotBlank(message = "configuration is required")
        private String configuration; // full JSON string

        private UUID   projectId;
        private Boolean isActive = true;
    }

    @Data @Builder @Jacksonized @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private UUID           id;
        private UUID           projectId;
        private String         aliasName;
        private String         configType;
        private String         schemaName;
        private String         tableName;
        private String         displayType;
        private String         configuration;
        private Boolean        isActive;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }
}
