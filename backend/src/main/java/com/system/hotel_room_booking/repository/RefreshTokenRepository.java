package com.system.hotel_room_booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.system.hotel_room_booking.model.entity.RefreshToken;
import com.system.hotel_room_booking.model.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    List<RefreshToken> findByUser(User user);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = ?1")
    void deleteByUserId(Long userId);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < ?1")
    void deleteExpiredTokens(LocalDateTime now);
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = ?2 WHERE rt.token = ?1")
    void revokeToken(String token, LocalDateTime revokedAt);
}

