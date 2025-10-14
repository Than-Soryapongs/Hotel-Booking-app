package com.system.hotel_booking.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage rate limiting buckets for API endpoints
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    private final CacheManager cacheManager;
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    
    /**
     * Resolve bucket for the request based on rate limit configuration
     */
    public Bucket resolveBucket(HttpServletRequest request, RateLimited rateLimited) {
        String key = generateKey(request, rateLimited.type());
        
        return bucketCache.computeIfAbsent(key, k -> createBucket(rateLimited));
    }
    
    /**
     * Generate unique key for rate limiting based on type
     */
    private String generateKey(HttpServletRequest request, RateLimitType type) {
        return switch (type) {
            case IP -> getClientIp(request);
            case USER -> {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                yield auth != null && auth.isAuthenticated() 
                    ? auth.getName() 
                    : getClientIp(request);
            }
            case IP_AND_USER -> {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String username = auth != null && auth.isAuthenticated() 
                    ? auth.getName() 
                    : "anonymous";
                yield getClientIp(request) + ":" + username;
            }
        };
    }
    
    /**
     * Create a new bucket with specified limits
     */
    private Bucket createBucket(RateLimited rateLimited) {
        Bandwidth limit = Bandwidth.builder()
            .capacity(rateLimited.limit())
            .refillIntervally(rateLimited.limit(), Duration.ofSeconds(rateLimited.duration()))
            .build();
        
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
    
    /**
     * Check if request is allowed under rate limit
     */
    public boolean tryConsume(HttpServletRequest request, RateLimited rateLimited) {
        Bucket bucket = resolveBucket(request, rateLimited);
        return bucket.tryConsume(1);
    }
    
    /**
     * Get available tokens for debugging/monitoring
     */
    public long getAvailableTokens(HttpServletRequest request, RateLimited rateLimited) {
        Bucket bucket = resolveBucket(request, rateLimited);
        return bucket.getAvailableTokens();
    }
}
