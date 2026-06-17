package com.framework.v25.dto.postgrest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatchProjectPayload {
    private String status;

    @JsonProperty("git_repo_url")
    private String gitRepoUrl;

    @JsonProperty("git_repo_name")
    private String gitRepoName;
}