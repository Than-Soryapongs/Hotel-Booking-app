package com.system.hotel_booking.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_api_key", columnList = "apiKey")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(nullable = true)  // Nullable for OAuth2 users who don't have passwords
    private String password;
    
    @Column(length = 50)
    private String firstName;
    
    @Column(length = 50)
    private String lastName;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;
    
    @Column(length = 255)
    private String address;
    
    @Column(length = 255)
    private String profileImageUrl;
    
    // OAuth2 fields
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;
    
    @Column(length = 255)
    private String providerId;
    
    @Column(length = 500)
    private String imageUrl;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean accountNonLocked = true;
    
    @Column(length = 255)
    private String emailVerificationToken;
    
    @Column
    private LocalDateTime emailVerificationTokenExpiresAt;
    
    // Email change verification fields
    @Column(length = 100)
    private String pendingEmail;
    
    @Column(length = 255)
    private String emailChangeToken;
    
    @Column
    private LocalDateTime emailChangeTokenExpiresAt;
    
    // Password reset fields
    @Column(length = 255)
    private String passwordResetToken;
    
    @Column
    private LocalDateTime passwordResetTokenExpiresAt;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RefreshToken> refreshTokens;
    
    // API Key fields
    @Column(unique = true, length = 255)
    private String apiKey;
    
    @Column
    private LocalDateTime apiKeyCreatedAt;
    
    @Column
    private LocalDateTime apiKeyExpiresAt;
    
    // Security tracking
    @Column
    @Builder.Default
    private Integer failedLoginAttempts = 0;
    
    @Column
    private LocalDateTime lockedUntil;
    
    @Column
    private String lastLoginIp;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime lastLoginAt;
}

