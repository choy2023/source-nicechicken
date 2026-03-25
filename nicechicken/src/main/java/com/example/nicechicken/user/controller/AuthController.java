package com.example.nicechicken.user.controller;

import com.example.nicechicken.user.dto.LoginRequest;
import com.example.nicechicken.user.entity.Role;
import com.example.nicechicken.user.entity.UserEntity;
import com.example.nicechicken.user.repository.UserRepository;
import com.example.nicechicken.user.security.JwtUtil;
import com.example.nicechicken.user.security.LoginRateLimiter;

import io.github.bucket4j.Bucket;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;

    @PostMapping("/login")
    public ResponseEntity<?> login(

            @RequestBody LoginRequest request,
            HttpServletResponse response,
            HttpServletRequest httpRequest) {

        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = httpRequest.getRemoteAddr();
        }

        Bucket bucket = rateLimiter.resolveBucket(ip);
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many login attempts. Please try again later."));
        }

        Optional<UserEntity> userOpt = userRepository.findByEmail(request.username());

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (user.getRole() == Role.ROLE_ADMIN && passwordEncoder.matches(request.password(), user.getPassword())) {
                String token = jwtUtil.generateToken(user.getEmail());

                ResponseCookie cookie = ResponseCookie.from("jwt", token)
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("None")
                        .path("/")
                        .maxAge(86400)
                        .build();

                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                return ResponseEntity.ok(Map.of("message", "Login successful"));

            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials or not an admin user."));
    }

}