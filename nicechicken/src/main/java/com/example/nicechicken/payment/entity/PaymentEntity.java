package com.example.nicechicken.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

import com.example.nicechicken.common.entity.BaseTimeEntity;

@Entity
@Table(name = "payment_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "stripe_payment_intent_id", nullable = false)
    private String stripePaymentIntentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status; // PAID, REFUNDED

    @Builder
    public PaymentEntity(UUID orderId, String stripePaymentIntentId, BigDecimal amount, String status) {
        this.orderId = orderId;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.amount = amount;
        this.status = status;
    }

    public void markAsRefunded() {
        this.status = "REFUNDED";
    }
}
