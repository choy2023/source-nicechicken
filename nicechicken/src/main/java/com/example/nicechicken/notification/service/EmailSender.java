package com.example.nicechicken.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final JavaMailSender mailSender;

    public void send(String to, String subject, String content) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(content, true);
                mailSender.send(message);
                log.info("✅ Email sent (attempt {}). Recipient: {}", attempt, to);
                return;
            } catch (MessagingException e) {
                lastException = e;
                log.warn("⚠️ Email attempt {}/{} failed. Recipient: {}, Reason: {}", attempt, MAX_ATTEMPTS, to, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }

        log.error("🚨 All {} email attempts failed. Recipient: {}, Subject: {}, Last error: {}",
                MAX_ATTEMPTS, to, subject, lastException != null ? lastException.getMessage() : "unknown");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
