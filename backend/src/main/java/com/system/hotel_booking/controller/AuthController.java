package com.system.hotel_booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.system.hotel_booking.model.dto.AuthResponse;
import com.system.hotel_booking.model.dto.ForgotPasswordRequest;
import com.system.hotel_booking.model.dto.LoginRequest;
import com.system.hotel_booking.model.dto.ResetPasswordRequest;
import com.system.hotel_booking.model.dto.SignupRequest;
import com.system.hotel_booking.ratelimit.RateLimited;
import com.system.hotel_booking.ratelimit.RateLimitType;
import com.system.hotel_booking.service.AuthService;
import com.system.hotel_booking.util.CookieUtil;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/signup")
    @RateLimited(limit = 5, duration = 3600, type = RateLimitType.IP) // 5 signups per hour per IP
    @Operation(summary = "Register new user", description = "Creates a new user account and sends email verification")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequest request) {
        Map<String, String> response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verifies user email with token sent to their inbox")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        Map<String, String> response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/resend-verification")
    @RateLimited(limit = 3, duration = 600, type = RateLimitType.IP) // 3 resends per 10 minutes per IP
    @Operation(summary = "Resend verification email", description = "Resends verification email to user")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestParam String email) {
        Map<String, String> response = authService.resendVerificationEmail(email);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login")
    @RateLimited(limit = 10, duration = 300, type = RateLimitType.IP) // 10 login attempts per 5 minutes per IP
    @Operation(summary = "User login", description = "Authenticates user and sets JWT tokens in HttpOnly cookies")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, 
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        AuthResponse response = authService.login(request, httpRequest);
        
        // Set tokens in HttpOnly cookies
        boolean isProduction = CookieUtil.isProduction();
        ResponseCookie accessTokenCookie = CookieUtil.createAccessTokenCookie(
            response.getAccessToken(), isProduction
        );
        ResponseCookie refreshTokenCookie = CookieUtil.createRefreshTokenCookie(
            response.getRefreshToken(), isProduction
        );
        
        CookieUtil.addCookie(httpResponse, accessTokenCookie);
        CookieUtil.addCookie(httpResponse, refreshTokenCookie);
        
        // Return response without tokens (tokens are now in cookies)
        response.setAccessToken(null);
        response.setRefreshToken(null);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generates new access token using refresh token from cookie")
    public ResponseEntity<AuthResponse> refreshToken(HttpServletRequest httpRequest,
                                                     HttpServletResponse httpResponse) {
        // Get refresh token from cookie
        String refreshToken = CookieUtil.getCookie(httpRequest, CookieUtil.REFRESH_TOKEN_COOKIE)
            .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        
        AuthResponse response = authService.refreshToken(refreshToken);
        
        // Set new access token in cookie
        boolean isProduction = CookieUtil.isProduction();
        ResponseCookie accessTokenCookie = CookieUtil.createAccessTokenCookie(
            response.getAccessToken(), isProduction
        );
        CookieUtil.addCookie(httpResponse, accessTokenCookie);
        
        // Return response without tokens
        response.setAccessToken(null);
        response.setRefreshToken(null);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Revokes refresh token and clears cookies")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest httpRequest,
                                                      HttpServletResponse httpResponse) {
        // Get refresh token from cookie
        String refreshToken = CookieUtil.getCookie(httpRequest, CookieUtil.REFRESH_TOKEN_COOKIE)
            .orElse(null);
        
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        
        // Delete cookies
        CookieUtil.deleteCookies(httpResponse, 
            CookieUtil.ACCESS_TOKEN_COOKIE, 
            CookieUtil.REFRESH_TOKEN_COOKIE
        );
        
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    @PostMapping("/forgot-password")
    @RateLimited(limit = 3, duration = 600, type = RateLimitType.IP) // 3 requests per 10 minutes per IP
    @Operation(summary = "Forgot password", description = "Sends password reset email to user")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Map<String, String> response = authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/reset-password")
    @RateLimited(limit = 5, duration = 600, type = RateLimitType.IP) // 5 attempts per 10 minutes per IP
    @Operation(summary = "Reset password", description = "Resets user password with valid token")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        Map<String, String> response = authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(response);
    }
}
