package com.example.nicechicken.payment.repository;

import com.example.nicechicken.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByOrderId(UUID orderId);
    boolean existsByStripePaymentIntentId(String stripePaymentIntentId);
}
