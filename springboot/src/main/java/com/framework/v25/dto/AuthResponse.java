package com.framework.v25.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private UserInfo user;

    @Data
    @Builder
@Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID   id;
        private String name;
        private String email;
    }
}
