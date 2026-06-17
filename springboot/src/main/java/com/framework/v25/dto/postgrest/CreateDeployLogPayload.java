package com.framework.v25.dto.postgrest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class CreateDeployLogPayload {

    @JsonProperty("project_id")
    private UUID projectId;

    private String status;

    @JsonProperty("deploy_url")
    private String deployUrl;

    private String logs;
}