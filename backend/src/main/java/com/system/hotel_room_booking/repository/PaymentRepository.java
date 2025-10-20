package com.system.hotel_room_booking.repository;

import com.system.hotel_room_booking.model.entity.Payment;
import com.system.hotel_room_booking.model.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by transaction ID
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Find payment by PayWay transaction ID
     */
    Optional<Payment> findByPaywayTransactionId(String paywayTransactionId);

    /**
     * Find all payments for a user
     */
    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByUserId(@Param("userId") Long userId);

    /**
     * Find all payments for a cart
     */
    @Query("SELECT p FROM Payment p WHERE p.cart.id = :cartId ORDER BY p.createdAt DESC")
    List<Payment> findByCartId(@Param("cartId") Long cartId);

    /**
     * Find payment by cart and status
     */
    @Query("SELECT p FROM Payment p WHERE p.cart.id = :cartId AND p.status = :status")
    Optional<Payment> findByCartIdAndStatus(@Param("cartId") Long cartId, @Param("status") PaymentStatus status);

    /**
     * Find all payments for a booking
     */
    @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId ORDER BY p.createdAt DESC")
    List<Payment> findByBookingId(@Param("bookingId") Long bookingId);

    /**
     * Find payments by status
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find pending payments older than specified time
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :expiryTime")
    List<Payment> findExpiredPendingPayments(@Param("expiryTime") LocalDateTime expiryTime);

    /**
     * Check if a transaction ID exists
     */
    boolean existsByTransactionId(String transactionId);

    /**
     * Count payments by user and status
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.user.id = :userId AND p.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") PaymentStatus status);
}
