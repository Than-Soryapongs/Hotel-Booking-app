package com.system.hotel_room_booking.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply rate limiting to controller methods
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    
    /**
     * Number of requests allowed per time window
     */
    int limit() default 100;
    
    /**
     * Time window duration in seconds
     */
    int duration() default 60;
    
    /**
     * Rate limit type (IP-based or user-based)
     */
    RateLimitType type() default RateLimitType.IP;
}
