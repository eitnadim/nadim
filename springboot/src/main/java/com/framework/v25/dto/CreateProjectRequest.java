package com.framework.v25.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Java version is required")
    private String javaVersion;

    @NotBlank(message = "Build tool is required")
    private String buildTool;

    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotBlank(message = "Artifact ID is required")
    private String artifactId;

    private String packageName;

    @NotBlank(message = "Spring Boot version is required")
    private String springBootVersion;

    @NotBlank(message = "Git invite email is required")
    @Email(message = "Must be a valid email")
    private String gitInviteEmail;
}
