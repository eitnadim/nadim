package com.framework.v25.dto.postgrest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class CreateBuildLogPayload {

    @JsonProperty("project_id")
    private UUID projectId;

    private String status;
    private String logs;

    @JsonProperty("commit_hash")
    private String commitHash;

    private String branch;
}
