package com.system.hotel_room_booking.repository;

import com.system.hotel_room_booking.model.entity.Cart;
import com.system.hotel_room_booking.model.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);
    Optional<Cart> findByTransactionIdAndStatus(String transactionId, CartStatus status);
}
