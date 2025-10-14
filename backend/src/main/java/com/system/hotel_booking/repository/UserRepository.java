package com.system.hotel_booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.system.hotel_booking.model.entity.User;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByApiKey(String apiKey);
    
    Optional<User> findByEmailVerificationToken(String token);
    
    Optional<User> findByEmailChangeToken(String token);
    
    Optional<User> findByPasswordResetToken(String token);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = ?2, u.lockedUntil = ?3 WHERE u.id = ?1")
    void updateFailedAttempts(Long userId, Integer attempts, LocalDateTime lockedUntil);
    
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = ?2 WHERE u.id = ?1")
    void updateLastLogin(Long userId, LocalDateTime lastLoginAt);
}

