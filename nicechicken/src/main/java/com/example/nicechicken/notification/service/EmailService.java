package com.example.nicechicken.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailSender emailSender;

    @Async
    public void sendHtmlEmail(String to, String subject, String content) {
        if (to == null || to.isBlank()) {
            log.warn("Email address missing, cancelling dispatch. Subject: {}", subject);
            return;
        }
        try {
            emailSender.send(to, subject, content);
        } catch (Exception e) {
            // Handled by @Recover in EmailSender after all retries are exhausted
        }
    }
}
