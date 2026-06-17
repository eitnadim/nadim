package com.framework.v25.dto.postgrest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateCustomClassPayload {

    @JsonProperty("project_id")
    private UUID projectId;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("class_name")
    private String className;

    @JsonProperty("file_size")
    private Long fileSize;

    @JsonProperty("uploaded_by")
    private UUID uploadedBy;
}
