package com.example.nicechicken.payment.controller;

import com.example.nicechicken.payment.entity.PaymentEntity;
import com.example.nicechicken.payment.repository.PaymentRepository;
import com.example.nicechicken.payment.event.PaymentSuccessEvent;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final ApplicationEventPublisher eventPublisher;
    private final PaymentRepository paymentRepository;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            // 1. Security Validation: Verify Signature to ensure request is from Stripe
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // 2. Handle payment completion event (checkout.session.completed)
        if ("checkout.session.completed".equals(event.getType())) {
            log.info("✅ checkout.session.completed event received!");

            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

            if (dataObjectDeserializer.getObject().isPresent()) {
                Session session = (Session) dataObjectDeserializer.getObject().get();
                processSession(session);
            } else {
                log.error("❌ Critical Error: Unable to parse object due to Stripe API version mismatch!");

                try {
                    // Bypass code to force parse on version mismatch (wrapped in try-catch)
                    Session session = (Session) dataObjectDeserializer.deserializeUnsafe();
                    processSession(session);
                } catch (com.stripe.exception.EventDataObjectDeserializationException e) {
                    // Last line of defense if forced parsing fails
                    log.error("🚨 Forced parsing failed: {}", e.getMessage());
                }
            }
        }
        return ResponseEntity.ok("");
    }

    private void processSession(Session session) {
        log.info("✅ Session object parsed successfully! Metadata: {}", session.getMetadata());

        if (session.getMetadata() != null && session.getMetadata().get("orderId") != null) {
            UUID orderId = UUID.fromString(session.getMetadata().get("orderId"));
            String paymentIntentId = session.getPaymentIntent();

            // Idempotency check: skip if this payment was already processed
            // Stripe webhooks use at-least-once delivery — duplicates are expected
            if (paymentRepository.existsByStripePaymentIntentId(paymentIntentId)) {
                log.info("⏭️ Duplicate webhook ignored. PaymentIntent already processed: {}", paymentIntentId);
                return;
            }

            BigDecimal amount = BigDecimal.valueOf(session.getAmountTotal()).divide(BigDecimal.valueOf(100));

            // Save payment information
            PaymentEntity payment = PaymentEntity.builder()
                    .orderId(orderId)
                    .stripePaymentIntentId(paymentIntentId)
                    .amount(amount)
                    .status("PAID")
                    .build();
            paymentRepository.save(payment);

            // Publish event
            log.info("🎉 Payment success event published! Order ID: {}", orderId);
            eventPublisher.publishEvent(new PaymentSuccessEvent(orderId));
        } else {
            log.warn("❌ Warning: 'orderId' missing from metadata!");
        }
    }
}