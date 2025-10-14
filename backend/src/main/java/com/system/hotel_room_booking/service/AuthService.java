package com.system.hotel_room_booking.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.system.hotel_room_booking.model.dto.AuthResponse;
import com.system.hotel_room_booking.model.dto.LoginRequest;
import com.system.hotel_room_booking.model.dto.SignupRequest;
import com.system.hotel_room_booking.model.dto.UserResponse;
import com.system.hotel_room_booking.model.entity.RefreshToken;
import com.system.hotel_room_booking.model.entity.Role;
import com.system.hotel_room_booking.model.entity.RoleName;
import com.system.hotel_room_booking.model.entity.User;
import com.system.hotel_room_booking.repository.RefreshTokenRepository;
import com.system.hotel_room_booking.repository.RoleRepository;
import com.system.hotel_room_booking.repository.UserRepository;
import com.system.hotel_room_booking.security.JwtTokenProvider;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 30;
    
    @Transactional
    public Map<String, String> signup(SignupRequest request) {
        // Validate username and email
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }
        
        // Generate email verification token
        String verificationToken = generateSecureToken();
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(24);
        
        // Create new user (disabled until email verification)
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword())) // Hash password
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .gender(request.getGender())
            .enabled(false) // Account disabled until email verification
            .accountNonLocked(true)
            .failedLoginAttempts(0)
            .emailVerificationToken(verificationToken)
            .emailVerificationTokenExpiresAt(tokenExpiry)
            .build();
        
        // Assign default role
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
            .orElseThrow(() -> new RuntimeException("Default role not found"));
        
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        
        User savedUser = userRepository.save(user);
        
        // Send verification email
        try {
            emailService.sendVerificationEmail(
                savedUser.getEmail(), 
                savedUser.getUsername(), 
                verificationToken
            );
        } catch (Exception e) {
            log.error("Failed to send verification email", e);
            // Don't fail registration if email fails, user can request resend
        }
        
        log.info("User registered successfully: {}. Verification email sent.", savedUser.getUsername());
        
        return Map.of(
            "message", "Registration successful! Please check your email to verify your account.",
            "email", savedUser.getEmail()
        );
    }
    
    @Transactional
    public Map<String, String> verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid verification token"));
        
        // Check if token has expired
        if (user.getEmailVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired. Please request a new one.");
        }
        
        // Activate user account
        user.setEnabled(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiresAt(null);
        
        userRepository.save(user);
        
        // Send welcome email
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            log.error("Failed to send welcome email", e);
        }
        
        log.info("Email verified successfully for user: {}", user.getUsername());
        
        return Map.of(
            "message", "Email verified successfully! You can now log in to your account.",
            "username", user.getUsername()
        );
    }
    
    @Transactional
    public Map<String, String> resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getEnabled()) {
            throw new RuntimeException("Email is already verified");
        }
        
        // Generate new verification token
        String verificationToken = generateSecureToken();
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(24);
        
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiresAt(tokenExpiry);
        
        userRepository.save(user);
        
        // Send verification email
        emailService.sendVerificationEmail(
            user.getEmail(), 
            user.getUsername(), 
            verificationToken
        );
        
        log.info("Verification email resent to: {}", user.getEmail());
        
        return Map.of(
            "message", "Verification email has been resent. Please check your email.",
            "email", user.getEmail()
        );
    }
    
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // Find user
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
            .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        
        // Check if email is verified
        if (!user.getEnabled()) {
            throw new RuntimeException("Please verify your email before logging in. Check your inbox for the verification link.");
        }
        
        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Account is locked. Try again later.");
        }
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    request.getPassword()
                )
            );
            
            // Reset failed attempts on successful login
            if (user.getFailedLoginAttempts() > 0) {
                userRepository.updateFailedAttempts(user.getId(), 0, null);
            }
            
            // Update last login
            userRepository.updateLastLogin(user.getId(), LocalDateTime.now());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            String accessToken = tokenProvider.generateAccessToken(authentication);
            String refreshToken = createRefreshToken(user, httpRequest);
            
            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .user(mapToUserResponse(user))
                .build();
                
        } catch (Exception e) {
            // Increment failed login attempts
            int newFailCount = user.getFailedLoginAttempts() + 1;
            LocalDateTime lockTime = null;
            
            if (newFailCount >= MAX_LOGIN_ATTEMPTS) {
                lockTime = LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES);
            }
            
            userRepository.updateFailedAttempts(user.getId(), newFailCount, lockTime);
            throw new RuntimeException("Invalid credentials");
        }
    }
    
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        
        if (token.getRevokedAt() != null) {
            throw new RuntimeException("Refresh token has been revoked");
        }
        
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token has expired");
        }
        
        User user = token.getUser();
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getUsername(), null, 
            user.getRoles().stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    role.getName().name()))
                .collect(Collectors.toList())
        );
        
        String newAccessToken = tokenProvider.generateAccessToken(authentication);
        
        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(tokenProvider.getJwtExpirationMs())
            .user(mapToUserResponse(user))
            .build();
    }
    
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            refreshTokenRepository.revokeToken(token.getToken(), LocalDateTime.now());
        });
    }
    
    @Transactional
    public Map<String, String> forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with this email address"));
        
        // Generate password reset token
        String resetToken = generateSecureToken();
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(1); // Token expires in 1 hour
        
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiresAt(tokenExpiry);
        
        userRepository.save(user);
        
        // Send password reset email
        try {
            emailService.sendPasswordResetEmail(
                user.getEmail(),
                user.getUsername(),
                resetToken
            );
            
            log.info("Password reset email sent to: {}", user.getEmail());
            
            return Map.of(
                "message", "Password reset instructions have been sent to your email address.",
                "email", user.getEmail()
            );
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            throw new RuntimeException("Failed to send password reset email. Please try again later.");
        }
    }
    
    @Transactional
    public Map<String, String> resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid password reset token"));
        
        // Check if token has expired
        if (user.getPasswordResetTokenExpiresAt() == null || 
            user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Password reset token has expired. Please request a new one.");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        
        // Reset failed login attempts if any
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }
        
        userRepository.save(user);
        
        log.info("Password reset successfully for user: {}", user.getUsername());
        
        return Map.of(
            "message", "Your password has been reset successfully. You can now log in with your new password.",
            "username", user.getUsername()
        );
    }
    
    private String createRefreshToken(User user, HttpServletRequest request) {
        String token = tokenProvider.generateRefreshToken(user.getUsername());
        
        long expirationMillis = tokenProvider.getJwtRefreshExpirationMs();
        long expirationSeconds = expirationMillis / 1000;
        
        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .token(token)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusSeconds(expirationSeconds))
            .ipAddress(request != null ? getClientIp(request) : null)
            .userAgent(request != null ? request.getHeader("User-Agent") : null)
            .build();
        
        refreshTokenRepository.save(refreshToken);
        
        return token;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
    
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .gender(user.getGender())
            .address(user.getAddress())
            .profileImageUrl(user.getProfileImageUrl())
            .roles(user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet()))
            .enabled(user.getEnabled())
            .emailVerified(user.getEnabled())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }
}
