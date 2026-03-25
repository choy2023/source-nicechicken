package com.example.nicechicken.payment.service;

import com.example.nicechicken.order.entity.OrderEntity;
import com.example.nicechicken.order.repository.OrderRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PaymentService {

        private final OrderRepository orderRepository;

        @Value("${stripe.api.key}")
        private String stripeSecretKey;

        @Value("${app.frontend.url}")
        private String frontendUrl;

        @PostConstruct
        public void init() {
                // Instruction: Set global API key
                Stripe.apiKey = stripeSecretKey;
        }

        public String createCheckoutSession(UUID orderId) throws StripeException {
                // 1. Retrieve the "source of truth" amount from the database
                OrderEntity order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

                // 2. Technical requirement: Convert to cents (using BigDecimal)
                // $14.50 -> 1450 cents
                long amountInCents = order.getTotalAmount()
                                .multiply(new BigDecimal("100"))
                                .longValue();

                // Build session parameters for Stripe Checkout
                SessionCreateParams params = SessionCreateParams.builder()
                                .setMode(SessionCreateParams.Mode.PAYMENT)
                                .setSuccessUrl(frontendUrl + "/payment/success")
                                .setCancelUrl(frontendUrl + "/payment/cancel")
                                // Instruction: Embed internal order ID in metadata
                                .putMetadata("orderId", orderId.toString())
                                .setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES).getEpochSecond())
                                .addLineItem(
                                                SessionCreateParams.LineItem.builder()
                                                                .setQuantity(1L)
                                                                .setPriceData(
                                                                                SessionCreateParams.LineItem.PriceData
                                                                                                .builder()
                                                                                                .setCurrency("nzd")
                                                                                                .setUnitAmount(amountInCents)
                                                                                                .setProductData(
                                                                                                                SessionCreateParams.LineItem.PriceData.ProductData
                                                                                                                                .builder()
                                                                                                                                .setName("Nice Chicken Order - "
                                                                                                                                                + order.getCustomerName())
                                                                                                                                .build())
                                                                                                .build())
                                                                .build())
                                .build();

                // 4. Return the payment page URL after session creation
                Session session = Session.create(params);
                return session.getUrl();
        }
}