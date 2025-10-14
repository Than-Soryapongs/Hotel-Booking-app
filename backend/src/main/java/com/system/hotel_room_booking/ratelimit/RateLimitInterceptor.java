package com.system.hotel_room_booking.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to enforce rate limiting on API endpoints
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RateLimitService rateLimitService;
    
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) 
            throws Exception {
        
        if (handler instanceof HandlerMethod handlerMethod) {
            RateLimited rateLimited = handlerMethod.getMethodAnnotation(RateLimited.class);
            
            if (rateLimited != null) {
                boolean allowed = rateLimitService.tryConsume(request, rateLimited);
                
                if (!allowed) {
                    log.warn("Rate limit exceeded for {} from IP: {}", 
                        request.getRequestURI(), 
                        getClientIp(request));
                    
                    response.setStatus(429); // Too Many Requests
                    response.setContentType("application/json");
                    response.getWriter().write(
                        String.format("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\",\"limit\":%d,\"duration\":%d}",
                            rateLimited.limit(), rateLimited.duration())
                    );
                    return false;
                }
                
                // Add rate limit headers to response
                long availableTokens = rateLimitService.getAvailableTokens(request, rateLimited);
                response.addHeader("X-Rate-Limit-Limit", String.valueOf(rateLimited.limit()));
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(availableTokens));
                response.addHeader("X-Rate-Limit-Duration", String.valueOf(rateLimited.duration()));
            }
        }
        
        return true;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
