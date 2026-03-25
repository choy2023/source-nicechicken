package com.example.nicechicken.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues short-lived, single-use tokens for SSE authentication.
 *
 * Flow:
 *   1. Client calls GET /api/sse/token (authenticated via JWT cookie)
 *   2. Server returns a one-time token (valid for 30 seconds)
 *   3. Client connects EventSource with ?token=xxx (no cookie needed)
 *   4. Server validates and consumes the token on SSE subscribe
 */
@Slf4j
@Service
public class SseTokenService {

    private static final long TOKEN_TTL_SECONDS = 30;

    private record TokenEntry(String email, String role, Instant expiresAt) {}

    private final ConcurrentHashMap<String, TokenEntry> tokens = new ConcurrentHashMap<>();

    public String issueToken(String email, String role) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new TokenEntry(email, role, Instant.now().plusSeconds(TOKEN_TTL_SECONDS)));
        log.debug("SSE token issued for [{}], expires in {}s", email, TOKEN_TTL_SECONDS);
        return token;
    }

    /**
     * Validates and consumes the token (single-use).
     * Returns the email if valid, null otherwise.
     */
    public String validateAndConsume(String token) {
        if (token == null) return null;

        TokenEntry entry = tokens.remove(token);
        if (entry == null) {
            log.debug("SSE token not found (already used or invalid)");
            return null;
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            log.debug("SSE token expired for [{}]", entry.email());
            return null;
        }
        return entry.email();
    }

    public String getRole(String token) {
        TokenEntry entry = tokens.get(token);
        return entry != null ? entry.role() : null;
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanupExpired() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}
