package com.example.nicechicken.notification.service;

import com.example.nicechicken.order.event.OrderStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class CustomerSseEmitters {

    // email -> list of emitters (one customer may have multiple tabs open)
    private final ConcurrentHashMap<String, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        String email = event.customerEmail();
        if (email == null) return;

        log.info("📡 Customer SSE: Pushing status update to {}. Order {} → {}",
                email, event.orderId(), event.newStatus());

        notifyCustomer(email, "order_status", Map.of(
                "orderId", event.orderId().toString(),
                "oldStatus", event.oldStatus().name(),
                "newStatus", event.newStatus().name()
        ));
    }

    public SseEmitter add(String email) {
        SseEmitter emitter = new SseEmitter(60 * 1000L * 30); // 30-minute timeout

        emitterMap.computeIfAbsent(email, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("New Customer SSE Connection for [{}]. Current subscribers: {}", email, countAll());

        emitter.onCompletion(() -> removeEmitter(email, emitter));
        emitter.onTimeout(() -> { emitter.complete(); removeEmitter(email, emitter); });
        emitter.onError((e) -> { emitter.complete(); removeEmitter(email, emitter); });

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return emitter;
    }

    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        emitterMap.forEach((email, emitters) -> {
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
            if (emitters.isEmpty()) {
                emitterMap.remove(email);
            }
        });
    }

    private void notifyCustomer(String email, String eventName, Object data) {
        List<SseEmitter> emitters = emitterMap.get(email);
        if (emitters == null || emitters.isEmpty()) return;

        List<SseEmitter> deadEmitters = new ArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                emitter.complete();
                deadEmitters.add(emitter);
            }
        });
        emitters.removeAll(deadEmitters);
        if (emitters.isEmpty()) {
            emitterMap.remove(email);
        }
    }

    private void removeEmitter(String email, SseEmitter emitter) {
        List<SseEmitter> emitters = emitterMap.get(email);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emitterMap.remove(email);
            }
        }
    }

    private int countAll() {
        return emitterMap.values().stream().mapToInt(List::size).sum();
    }
}
