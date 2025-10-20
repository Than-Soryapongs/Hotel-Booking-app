package com.system.hotel_room_booking.repository;

import com.system.hotel_room_booking.model.entity.Discount;
import com.system.hotel_room_booking.model.entity.DiscountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    
    Optional<Discount> findByCode(String code);
    
    List<Discount> findByType(DiscountType type);
    
    List<Discount> findByIsActiveTrue();
    
    @Query("SELECT d FROM Discount d WHERE d.code = :code AND d.isActive = true " +
           "AND (d.validFrom IS NULL OR d.validFrom <= :now) " +
           "AND (d.validUntil IS NULL OR d.validUntil >= :now) " +
           "AND (d.maxUsageCount IS NULL OR d.currentUsageCount < d.maxUsageCount)")
    Optional<Discount> findValidDiscountByCode(@Param("code") String code, @Param("now") LocalDateTime now);
    
    @Query("SELECT d FROM Discount d WHERE d.isActive = true " +
           "AND (d.validFrom IS NULL OR d.validFrom <= :now) " +
           "AND (d.validUntil IS NULL OR d.validUntil >= :now)")
    List<Discount> findActiveDiscounts(@Param("now") LocalDateTime now);
}
