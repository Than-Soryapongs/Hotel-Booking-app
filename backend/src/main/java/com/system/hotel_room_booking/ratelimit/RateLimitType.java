package com.system.hotel_room_booking.ratelimit;

/**
 * Enum to specify the type of rate limiting
 */
public enum RateLimitType {
    /**
     * Rate limit based on IP address
     */
    IP,
    
    /**
     * Rate limit based on authenticated user
     */
    USER,
    
    /**
     * Rate limit based on both IP and user
     */
    IP_AND_USER
}
