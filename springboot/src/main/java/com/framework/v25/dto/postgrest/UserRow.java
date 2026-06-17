package com.framework.v25.dto.postgrest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRow {

    private UUID   id;
    private String name;
    private String email;

    @JsonProperty("password_hash")
    private String passwordHash;

    private String role;
}
