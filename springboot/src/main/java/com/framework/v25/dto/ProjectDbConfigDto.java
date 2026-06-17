package com.framework.v25.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ProjectDbConfigDto {

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Request {

        @NotBlank(message = "dbHost is required")
        private String  dbHost;

        @NotNull(message = "dbPort is required")
        private Integer dbPort     = 5432;

        @NotBlank(message = "dbName is required")
        private String  dbName;

        @NotBlank(message = "dbUsername is required")
        private String  dbUsername;

        // password optional on update (blank = keep existing)
        private String  dbPassword;

        private String  dbSchema   = "public";
        private Boolean sslEnabled = false;
        private Boolean isActive   = true;
    }

    @Data @Builder @Jacksonized @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private UUID           id;
        private UUID           projectId;
        private String         dbHost;
        private Integer        dbPort;
        private String         dbName;
        private String         dbUsername;
        private String         dbSchema;
        private Boolean        sslEnabled;
        private Boolean        isActive;
        private String         testStatus;   // UNTESTED | SUCCESS | FAILED
        private OffsetDateTime lastTested;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        // password is never returned to frontend
    }

    @Data @Builder @Jacksonized @NoArgsConstructor @AllArgsConstructor
    public static class TestResult {
        private boolean success;
        private String  message;
        private Long    latencyMs;
    }
}
