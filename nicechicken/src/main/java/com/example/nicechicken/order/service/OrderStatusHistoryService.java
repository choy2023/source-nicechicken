package com.example.nicechicken.order.service;

import com.example.nicechicken.order.entity.OrderStatus;
import com.example.nicechicken.order.entity.OrderStatusHistory;
import com.example.nicechicken.order.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusHistoryService {

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID orderId, OrderStatus oldStatus, OrderStatus newStatus, String changedBy) {
        try {
            orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                    .orderId(orderId)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .changedAt(LocalDateTime.now())
                    .changedBy(changedBy)
                    .build());
        } catch (Exception e) {
            log.error("Failed to save order status history for orderId={}: {}", orderId, e.getMessage());
        }
    }
}
