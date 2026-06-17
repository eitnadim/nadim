package com.framework.v25.dto.postgrest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRow {

    private UUID   id;

    @JsonProperty("owner_id")
    private UUID   ownerId;

    private String name;
    private String description;

    @JsonProperty("java_version")
    private String javaVersion;

    @JsonProperty("build_tool")
    private String buildTool;

    @JsonProperty("group_id")
    private String groupId;

    @JsonProperty("artifact_id")
    private String artifactId;

    @JsonProperty("package_name")
    private String packageName;

    @JsonProperty("spring_boot_version")
    private String springBootVersion;

    @JsonProperty("git_invite_email")
    private String gitInviteEmail;

    @JsonProperty("git_repo_url")
    private String gitRepoUrl;

    @JsonProperty("git_repo_name")
    private String gitRepoName;

    private String status;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}