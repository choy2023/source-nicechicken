package com.example.nicechicken.order.repository;

import com.example.nicechicken.order.entity.OrderEntity;
import com.example.nicechicken.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    // JOIN FETCH query to prevent N+1 problem
    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.orderItems")
    List<OrderEntity> findAllWithItems();

    // Fetch orders for a specific user (Prevent N+1)
    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.orderItems WHERE o.user.id = :userId AND o.deletedAt IS NULL")
    List<OrderEntity> findAllByUserIdWithItems(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM OrderEntity o WHERE o.deletedAt < :threshold")
    void deleteByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status AND o.deletedAt IS NULL AND o.createdAt < :threshold")
    List<OrderEntity> findPendingOrdersBefore(@Param("status") OrderStatus status, @Param("threshold") LocalDateTime threshold);
}