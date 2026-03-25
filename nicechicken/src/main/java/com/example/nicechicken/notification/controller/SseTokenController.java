package com.example.nicechicken.notification.controller;

import com.example.nicechicken.notification.service.SseTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Issues a short-lived SSE token for the authenticated user.
 * This endpoint is called via axios (which sends the JWT cookie),
 * and returns a token that EventSource can use as a query parameter.
 */
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseTokenController {

    private final SseTokenService sseTokenService;

    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> issueToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        String token = sseTokenService.issueToken(email, role);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
