package com.system.hotel_booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.system.hotel_booking.model.dto.UpdateProfileRequest;
import com.system.hotel_booking.model.dto.UserResponse;
import com.system.hotel_booking.model.entity.User;
import com.system.hotel_booking.repository.UserRepository;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public UserResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return mapToUserResponse(user);
    }
    
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        // Email change is now handled separately through requestEmailChange() method
        
        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }
    
    @Transactional
    public Map<String, String> requestEmailChange(String newEmail, String password) {
        User user = getCurrentUser();
        
        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        // Check if new email is the same as current
        if (newEmail.equals(user.getEmail())) {
            throw new RuntimeException("New email is the same as current email");
        }
        
        // Check if new email is already in use
        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Email is already in use");
        }
        
        // Generate email change verification token
        String verificationToken = generateSecureToken();
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(24);
        
        // Store pending email and token
        user.setPendingEmail(newEmail);
        user.setEmailChangeToken(verificationToken);
        user.setEmailChangeTokenExpiresAt(tokenExpiry);
        
        userRepository.save(user);
        
        // Send verification email to new email address
        try {
            emailService.sendEmailChangeVerificationEmail(
                newEmail,
                user.getUsername(),
                user.getEmail(), // old email
                verificationToken
            );
            
            log.info("Email change verification sent to: {} for user: {}", newEmail, user.getUsername());
            
            return Map.of(
                "message", "Verification email has been sent to " + newEmail + ". Please check your inbox and verify the new email address.",
                "pendingEmail", newEmail
            );
        } catch (Exception e) {
            log.error("Failed to send email change verification", e);
            throw new RuntimeException("Failed to send verification email. Please try again later.");
        }
    }
    
    @Transactional
    public Map<String, String> verifyEmailChange(String token) {
        User user = userRepository.findByEmailChangeToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid verification token"));
        
        // Check if token has expired
        if (user.getEmailChangeTokenExpiresAt() == null || 
            user.getEmailChangeTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired. Please request a new email change.");
        }
        
        // Check if pending email still available
        if (user.getPendingEmail() == null) {
            throw new RuntimeException("No pending email change found");
        }
        
        // Double-check if new email is still available (in case someone registered with it in the meantime)
        if (userRepository.existsByEmail(user.getPendingEmail())) {
            user.setPendingEmail(null);
            user.setEmailChangeToken(null);
            user.setEmailChangeTokenExpiresAt(null);
            userRepository.save(user);
            throw new RuntimeException("Email is no longer available");
        }
        
        String oldEmail = user.getEmail();
        String newEmail = user.getPendingEmail();
        
        // Update email
        user.setEmail(newEmail);
        user.setPendingEmail(null);
        user.setEmailChangeToken(null);
        user.setEmailChangeTokenExpiresAt(null);
        
        userRepository.save(user);
        
        // Send confirmation email to old email address
        try {
            emailService.sendEmailChangeConfirmation(oldEmail, user.getUsername(), newEmail);
        } catch (Exception e) {
            log.error("Failed to send email change confirmation to old email", e);
            // Don't fail the operation if confirmation email fails
        }
        
        log.info("Email changed successfully for user: {} from {} to {}", user.getUsername(), oldEmail, newEmail);
        
        return Map.of(
            "message", "Email has been successfully changed to " + newEmail,
            "newEmail", newEmail
        );
    }
    
    @Transactional
    public Map<String, String> cancelEmailChange() {
        User user = getCurrentUser();
        
        if (user.getPendingEmail() == null) {
            throw new RuntimeException("No pending email change found");
        }
        
        user.setPendingEmail(null);
        user.setEmailChangeToken(null);
        user.setEmailChangeTokenExpiresAt(null);
        
        userRepository.save(user);
        
        log.info("Email change cancelled for user: {}", user.getUsername());
        
        return Map.of("message", "Email change request has been cancelled");
    }
    
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    @Transactional
    public String generateApiKey() {
        User user = getCurrentUser();
        
        // Generate secure random API key
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            String apiKey = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            
            user.setApiKey(apiKey);
            user.setApiKeyExpiresAt(LocalDateTime.now().plusYears(1)); // Valid for 1 year
            
            userRepository.save(user);
            
            return apiKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate API key", e);
        }
    }
    
    @Transactional
    public void revokeApiKey() {
        User user = getCurrentUser();
        user.setApiKey(null);
        user.setApiKeyExpiresAt(null);
        userRepository.save(user);
    }
    
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        User user = getCurrentUser();
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    @Transactional
    public String uploadProfilePicture(org.springframework.web.multipart.MultipartFile file) {
        try {
            User user = getCurrentUser();
            
            // Delete old profile picture if exists
            if (user.getProfileImageUrl() != null) {
                fileStorageService.deleteProfilePicture(user.getProfileImageUrl());
            }
            
            // Store new profile picture
            String profileImageUrl = fileStorageService.storeProfilePicture(file, user.getId());
            
            // Update user
            user.setProfileImageUrl(profileImageUrl);
            userRepository.save(user);
            
            log.info("Profile picture uploaded for user: {}", user.getUsername());
            return profileImageUrl;
        } catch (Exception e) {
            log.error("Failed to upload profile picture", e);
            throw new RuntimeException("Failed to upload profile picture: " + e.getMessage());
        }
    }
    
    @Transactional
    public void deleteProfilePicture() {
        User user = getCurrentUser();
        
        if (user.getProfileImageUrl() != null) {
            fileStorageService.deleteProfilePicture(user.getProfileImageUrl());
            user.setProfileImageUrl(null);
            userRepository.save(user);
            log.info("Profile picture deleted for user: {}", user.getUsername());
        }
    }
    
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .pendingEmail(user.getPendingEmail())
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

