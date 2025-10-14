package com.system.hotel_booking.common;

/**
 * Exception thrown when rate limit is exceeded
 */
public class RateLimitExceededException extends RuntimeException {
    
    private final int limit;
    private final int duration;
    
    public RateLimitExceededException(int limit, int duration) {
        super(String.format("Rate limit exceeded. Maximum %d requests per %d seconds allowed.", limit, duration));
        this.limit = limit;
        this.duration = duration;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public int getDuration() {
        return duration;
    }
}
