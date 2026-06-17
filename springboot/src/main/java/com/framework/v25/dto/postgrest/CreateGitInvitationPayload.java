package com.framework.v25.dto.postgrest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateGitInvitationPayload {

    @JsonProperty("project_id")
    private UUID projectId;

    @JsonProperty("invited_email")
    private String invitedEmail;

    private String status;
}
