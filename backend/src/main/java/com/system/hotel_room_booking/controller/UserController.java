package com.system.hotel_room_booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.system.hotel_room_booking.model.dto.ChangeEmailRequest;
import com.system.hotel_room_booking.model.dto.ProfilePictureResponse;
import com.system.hotel_room_booking.model.dto.UpdateProfileRequest;
import com.system.hotel_room_booking.model.dto.UserResponse;
import com.system.hotel_room_booking.ratelimit.RateLimited;
import com.system.hotel_room_booking.ratelimit.RateLimitType;
import com.system.hotel_room_booking.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "User Profile", description = "User profile management endpoints")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Retrieves authenticated user's profile")
    public ResponseEntity<UserResponse> getProfile() {
        UserResponse response = userService.getCurrentUserProfile();
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Updates authenticated user's profile information")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateProfile(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/change-password")
    @RateLimited(limit = 5, duration = 600, type = RateLimitType.USER) // 5 password changes per 10 minutes per user
    @Operation(summary = "Change password", description = "Changes user's password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        userService.changePassword(oldPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
    
    @PostMapping("/profile-picture")
    @RateLimited(limit = 10, duration = 3600, type = RateLimitType.USER) // 10 uploads per hour per user
    @Operation(summary = "Upload profile picture", description = "Uploads or updates user's profile picture")
    public ResponseEntity<ProfilePictureResponse> uploadProfilePicture(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String profileImageUrl = userService.uploadProfilePicture(file);
        
        ProfilePictureResponse response = ProfilePictureResponse.builder()
            .profileImageUrl(profileImageUrl)
            .message("Profile picture uploaded successfully")
            .fileSize(file.getSize())
            .fileName(file.getOriginalFilename())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/profile-picture")
    @Operation(summary = "Delete profile picture", description = "Deletes user's profile picture")
    public ResponseEntity<Map<String, String>> deleteProfilePicture() {
        userService.deleteProfilePicture();
        return ResponseEntity.ok(Map.of("message", "Profile picture deleted successfully"));
    }
    
    @PostMapping("/change-email")
    @RateLimited(limit = 3, duration = 3600, type = RateLimitType.USER) // 3 email changes per hour per user
    @Operation(summary = "Request email change", description = "Initiates email change process with verification")
    public ResponseEntity<Map<String, String>> requestEmailChange(@Valid @RequestBody ChangeEmailRequest request) {
        Map<String, String> response = userService.requestEmailChange(request.getNewEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/verify-email-change")
    @Operation(summary = "Verify email change", description = "Verifies and completes the email change")
    public ResponseEntity<Map<String, String>> verifyEmailChange(@RequestParam String token) {
        Map<String, String> response = userService.verifyEmailChange(token);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/cancel-email-change")
    @Operation(summary = "Cancel email change", description = "Cancels pending email change request")
    public ResponseEntity<Map<String, String>> cancelEmailChange() {
        Map<String, String> response = userService.cancelEmailChange();
        return ResponseEntity.ok(response);
    }
}

