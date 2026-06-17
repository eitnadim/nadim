package com.framework.v25.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private UUID   id;
    private UUID   ownerId;
    private String name;
    private String description;
    private String javaVersion;
    private String buildTool;
    private String groupId;
    private String artifactId;
    private String packageName;
    private String springBootVersion;
    private String gitInviteEmail;
    private String gitRepoUrl;
    private String gitRepoName;
    private String status;
    private String createdAt;
    private String updatedAt;
}