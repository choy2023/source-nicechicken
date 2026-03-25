package com.example.nicechicken.notification.service;

import com.example.nicechicken.payment.event.PaymentSuccessEvent;
import com.example.nicechicken.payment.event.RefundCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseEmitters {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @EventListener
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("🔔 Notification Module: Payment success event received. Order ID: {}", event.orderId());
        notify("new_order", "A new order has been paid: " + event.orderId());
    }

    @EventListener
    public void handleRefundCompleted(RefundCompletedEvent event) {
        log.info("🔔 Notification Module: Refund completed event received. Order ID: {}", event.orderId());
        notify("refund_completed", "Order refund has been completed: " + event.orderId());
    }

    public SseEmitter add() {
        SseEmitter emitter = new SseEmitter(60 * 1000L * 30); // 30-minute timeout
        this.emitters.add(emitter);
        log.info("New SSE Connection! Current subscribers: {}", emitters.size());

        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> { emitter.complete(); this.emitters.remove(emitter); });
        emitter.onError((e) -> { emitter.complete(); this.emitters.remove(emitter); });

        try {
            // Send dummy data immediately to prevent browser timeout
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return emitter;
    }

    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data(""));
            } catch (IOException e) {
                emitter.complete();
                deadEmitters.add(emitter);
            }
        });
        emitters.removeAll(deadEmitters);
    }

    // Push event to all pipelines when a new order is received
    public void notify(String eventName, Object data) {
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(emitter);
            }
        });
    }
}