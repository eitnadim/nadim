package com.framework.v25.service;

import com.framework.v25.config.PostgRestClient;
import com.framework.v25.dto.AuthResponse;
import com.framework.v25.dto.LoginRequest;
import com.framework.v25.dto.postgrest.UserRow;
import com.framework.v25.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final PostgRestClient postgRestClient;
    private final JwtUtil         jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor injection — all three dependencies are final and guaranteed
     * non-null by Spring before the bean is used.
     * Never mix @RequiredArgsConstructor with @Autowired field injection —
     * only one approach should be used per class.
     */
    @Autowired
    public AuthService(
            PostgRestClient postgRestClient,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder) {
        this.postgRestClient = postgRestClient;
        this.jwtUtil         = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request) {

        // 1. Fetch user from PostgREST (anon role — no JWT yet)
        UserRow user = postgRestClient.findUserByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // 2. Verify BCrypt password against stored hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // 3. Issue JWT — embeds user_id + role=authenticated for PostgREST RLS
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .build())
                .build();
    }
}