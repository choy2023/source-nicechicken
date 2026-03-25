package com.example.nicechicken.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * CSRF protection via Origin/Referer validation.
 *
 * Since we use HttpOnly cookies (SameSite=None for cross-origin) with CSRF disabled,
 * we need to manually verify that state-changing requests (POST/PUT/PATCH/DELETE)
 * originate from our trusted frontend domains.
 *
 * This prevents cross-site form submission attacks while keeping HttpOnly cookie auth
 * (which protects JWT from XSS — a more common attack vector than CSRF).
 */
@Component
public class CsrfOriginFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final Set<String> allowedOrigins;

    public CsrfOriginFilter(
            @Value("${app.allowed-origins:http://localhost:5173}") String allowedOriginsConfig) {
        this.allowedOrigins = new HashSet<>(
                Arrays.asList(allowedOriginsConfig.split(","))
        );
        this.allowedOrigins.add("http://localhost:5173"); // always allow local dev
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip safe (read-only) methods — CSRF only applies to state-changing requests
        if (SAFE_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Exempt paths that receive external callbacks (no Origin from our frontend)
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/payments/webhook")    // Stripe webhook
                || uri.startsWith("/oauth2/")          // Google OAuth2 callback
                || uri.startsWith("/login/oauth2/")) { // Spring Security OAuth2 redirect
            filterChain.doFilter(request, response);
            return;
        }

        // Check Origin header first, fall back to Referer
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            origin = extractOriginFromReferer(request.getHeader("Referer"));
        }

        if (origin != null && allowedOrigins.contains(origin.trim())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Reject: no valid origin found
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Forbidden: invalid origin\"}");
    }

    private String extractOriginFromReferer(String referer) {
        if (referer == null || referer.isBlank()) return null;
        try {
            java.net.URI uri = java.net.URI.create(referer);
            return uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() > 0 && uri.getPort() != 443 && uri.getPort() != 80
                    ? ":" + uri.getPort() : "");
        } catch (Exception e) {
            return null;
        }
    }
}
