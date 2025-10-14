package com.system.hotel_booking.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.util.SerializationUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Utility class for creating and managing secure HTTP cookies
 * Implements best practices for cookie security including HttpOnly, Secure, and SameSite attributes
 */
public class CookieUtil {
    
    // Cookie names
    public static final String ACCESS_TOKEN_COOKIE = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    
    // Cookie durations
    private static final Duration ACCESS_TOKEN_DURATION = Duration.ofMinutes(30);
    private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(1);
    
    /**
     * Creates a secure HttpOnly cookie for access token
     * 
     * @param token The JWT access token value
     * @param isProduction Whether running in production (enables Secure flag)
     * @return ResponseCookie with security settings
     */
    public static ResponseCookie createAccessTokenCookie(String token, boolean isProduction) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)                    // Prevents JavaScript access (XSS protection)
                .secure(isProduction)               // HTTPS only in production
                .path("/")                          // Available across all paths
                .maxAge(ACCESS_TOKEN_DURATION)      // 30 minutes
                .sameSite("Strict")                 // CSRF protection
                .build();
    }
    
    /**
     * Creates a secure HttpOnly cookie for refresh token
     * 
     * @param token The JWT refresh token value
     * @param isProduction Whether running in production (enables Secure flag)
     * @return ResponseCookie with security settings
     */
    public static ResponseCookie createRefreshTokenCookie(String token, boolean isProduction) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)                    // Prevents JavaScript access
                .secure(isProduction)               // HTTPS only in production
                .path("/api/auth/refresh")          // Only sent to refresh endpoint
                .maxAge(REFRESH_TOKEN_DURATION)     // 24 hours
                .sameSite("Strict")                 // CSRF protection
                .build();
    }
    
    /**
     * Creates a cookie for deletion (expires immediately)
     * 
     * @param name Cookie name to delete
     * @return ResponseCookie that expires immediately
     */
    public static ResponseCookie createDeleteCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)                          // Expires immediately
                .sameSite("Strict")
                .build();
    }
    
    /**
     * Retrieves a cookie value by name from the request
     * 
     * @param request HttpServletRequest containing cookies
     * @param name Cookie name to retrieve
     * @return Optional containing cookie value if found
     */
    public static Optional<String> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals(name))
                    .map(Cookie::getValue)
                    .findFirst();
        }
        
        return Optional.empty();
    }
    
    /**
     * Adds a ResponseCookie to the HTTP response
     * 
     * @param response HttpServletResponse to add cookie to
     * @param cookie ResponseCookie to add
     */
    public static void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader("Set-Cookie", cookie.toString());
    }
    
    /**
     * Deletes multiple cookies by name
     * 
     * @param response HttpServletResponse to add deletion cookies to
     * @param cookieNames Names of cookies to delete
     */
    public static void deleteCookies(HttpServletResponse response, String... cookieNames) {
        for (String cookieName : cookieNames) {
            addCookie(response, createDeleteCookie(cookieName));
        }
    }
    
    /**
     * Serializes an object to a Base64 encoded cookie value
     * Useful for storing complex objects in cookies (use with caution for size)
     * 
     * @param object Object to serialize
     * @return Base64 encoded string
     */
    public static String serialize(Object object) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(object));
    }
    
    /**
     * Deserializes a Base64 encoded cookie value back to an object
     * 
     * @param cookie Cookie value to deserialize
     * @param cls Class type to deserialize to
     * @return Deserialized object
     */
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue())
        ));
    }
    
    /**
     * Validates if the application is running in production mode
     * Based on common environment variables and system properties
     * 
     * @return true if production, false otherwise
     */
    public static boolean isProduction() {
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        String profile = System.getProperty("spring.profiles.active");
        
        return "production".equalsIgnoreCase(env) || 
               "prod".equalsIgnoreCase(env) ||
               "production".equalsIgnoreCase(profile) ||
               "prod".equalsIgnoreCase(profile);
    }
}

