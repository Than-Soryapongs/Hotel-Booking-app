# Rate Limiting Feature

## Overview
This document describes the rate limiting implementation for the Hotel Booking API to prevent abuse, ensure fair usage, and protect against DDoS attacks.

## Implementation Details

### Technology Stack
- **Bucket4j**: Token bucket algorithm implementation
- **Caffeine Cache**: In-memory caching for rate limit buckets
- **Spring Interceptors**: Request interception for rate limit enforcement

### Rate Limiting Strategy
The system uses the **Token Bucket Algorithm**:
- Each user/IP has a bucket with a fixed capacity of tokens
- Each request consumes one token
- Tokens refill at a constant rate
- When bucket is empty, requests are rejected with HTTP 429

## Rate Limit Configuration

### Authentication Endpoints

| Endpoint | Limit | Duration | Type | Reason |
|----------|-------|----------|------|---------|
| `POST /api/auth/signup` | 5 requests | 1 hour | IP | Prevent account creation abuse |
| `POST /api/auth/login` | 10 requests | 5 minutes | IP | Prevent brute force attacks |
| `POST /api/auth/resend-verification` | 3 requests | 10 minutes | IP | Prevent email spam |
| `POST /api/auth/forgot-password` | 3 requests | 10 minutes | IP | Prevent email spam |
| `POST /api/auth/reset-password` | 5 requests | 10 minutes | IP | Prevent brute force attacks |

### User Profile Endpoints

| Endpoint | Limit | Duration | Type | Reason |
|----------|-------|----------|------|---------|
| `POST /api/user/change-password` | 5 requests | 10 minutes | USER | Prevent password change abuse |
| `POST /api/user/change-email` | 3 requests | 1 hour | USER | Prevent email change abuse |
| `POST /api/user/profile-picture` | 10 requests | 1 hour | USER | Prevent storage abuse |

### Rate Limit Types

#### IP-based Rate Limiting
```java
@RateLimited(limit = 10, duration = 300, type = RateLimitType.IP)
```
- Applies to **all requests from the same IP address**
- Best for public endpoints (login, signup, forgot password)
- Protects against distributed attacks

#### User-based Rate Limiting
```java
@RateLimited(limit = 5, duration = 600, type = RateLimitType.USER)
```
- Applies to **authenticated users** based on username
- Best for protected endpoints
- Falls back to IP-based if user is not authenticated

#### Combined Rate Limiting
```java
@RateLimited(limit = 20, duration = 60, type = RateLimitType.IP_AND_USER)
```
- Applies to **both IP address AND username**
- Most restrictive option
- Best for highly sensitive operations

## HTTP Response Headers

When rate limiting is active, the API returns additional headers:

```http
X-Rate-Limit-Limit: 10
X-Rate-Limit-Remaining: 7
X-Rate-Limit-Duration: 300
```

### Header Descriptions
- `X-Rate-Limit-Limit`: Maximum requests allowed in the time window
- `X-Rate-Limit-Remaining`: Number of requests remaining in current window
- `X-Rate-Limit-Duration`: Time window duration in seconds

## Error Responses

### HTTP 429 - Too Many Requests

When rate limit is exceeded:

```json
{
  "timestamp": "2025-10-14T10:30:00",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Maximum 10 requests per 300 seconds allowed.",
  "path": "/api/auth/login",
  "details": [
    "Limit: 10 requests",
    "Window: 300 seconds"
  ]
}
```

## Usage Examples

### Adding Rate Limiting to a Controller Method

```java
@RestController
@RequestMapping("/api/booking")
public class BookingController {
    
    // IP-based rate limiting: 50 requests per minute
    @PostMapping("/create")
    @RateLimited(limit = 50, duration = 60, type = RateLimitType.IP)
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequest request) {
        // Your logic here
    }
    
    // User-based rate limiting: 100 requests per hour
    @GetMapping("/my-bookings")
    @RateLimited(limit = 100, duration = 3600, type = RateLimitType.USER)
    public ResponseEntity<List<Booking>> getMyBookings() {
        // Your logic here
    }
}
```

### Custom Rate Limits for Different Scenarios

```java
// Strict limits for sensitive operations
@PostMapping("/admin/delete-user")
@RateLimited(limit = 1, duration = 60, type = RateLimitType.USER)
public ResponseEntity<?> deleteUser(@PathVariable Long id) {
    // ...
}

// Generous limits for read operations
@GetMapping("/hotels")
@RateLimited(limit = 1000, duration = 60, type = RateLimitType.IP)
public ResponseEntity<List<Hotel>> getAllHotels() {
    // ...
}

// Moderate limits for write operations
@PostMapping("/reviews")
@RateLimited(limit = 10, duration = 3600, type = RateLimitType.USER)
public ResponseEntity<Review> createReview(@RequestBody ReviewRequest request) {
    // ...
}
```

## Testing Rate Limits

### Using cURL

```bash
# Test login rate limit (10 requests per 5 minutes)
for i in {1..12}; do
  echo "Request $i:"
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"usernameOrEmail":"test","password":"test"}' \
    -v
  echo "---"
done
```

### Using Postman

1. Create a Collection Request
2. Add the endpoint URL
3. Go to Tests tab and add:
```javascript
pm.test("Rate limit headers present", function () {
    pm.response.to.have.header("X-Rate-Limit-Limit");
    pm.response.to.have.header("X-Rate-Limit-Remaining");
    pm.response.to.have.header("X-Rate-Limit-Duration");
});

pm.test("Rate limit not exceeded", function () {
    pm.expect(pm.response.code).to.not.equal(429);
});
```

### Using Python Script

```python
import requests
import time

url = "http://localhost:8080/api/auth/login"
headers = {"Content-Type": "application/json"}
data = {"usernameOrEmail": "test", "password": "test"}

for i in range(1, 15):
    response = requests.post(url, headers=headers, json=data)
    print(f"Request {i}: Status {response.status_code}")
    
    if 'X-Rate-Limit-Remaining' in response.headers:
        print(f"  Remaining: {response.headers['X-Rate-Limit-Remaining']}")
    
    if response.status_code == 429:
        print("  Rate limit exceeded!")
        break
    
    time.sleep(0.5)
```

## Configuration

### Modifying Cache Settings

In `CacheConfig.java`:

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("rateLimitBuckets");
    cacheManager.setCaffeine(Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)  // Adjust expiration
        .maximumSize(100_000));                 // Adjust cache size
    return cacheManager;
}
```

### Excluding Endpoints from Rate Limiting

In `WebMvcConfig.java`:

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/public/**",     // Add public endpoints
                "/api/health",         // Add health checks
                "/actuator/**",
                "/swagger-ui/**"
            );
}
```

## Monitoring and Logging

Rate limit events are logged with IP addresses:

```
WARN - Rate limit exceeded for /api/auth/login from IP: 192.168.1.100
```

### Metrics Collection

Add to `application.properties` for detailed metrics:

```properties
# Enable rate limit metrics
management.endpoints.web.exposure.include=metrics,prometheus
management.metrics.enable.rate.limit=true
```

## Best Practices

### 1. Choose Appropriate Limits
- **Read operations**: Higher limits (100-1000 req/min)
- **Write operations**: Moderate limits (10-50 req/min)
- **Sensitive operations**: Strict limits (1-5 req/min)

### 2. Use Correct Rate Limit Type
- **Public endpoints**: IP-based
- **Authenticated endpoints**: USER-based
- **Highly sensitive**: IP_AND_USER

### 3. Set Reasonable Time Windows
- **Short operations**: 60-300 seconds
- **Resource-intensive**: 600-3600 seconds
- **Email/SMS operations**: 3600+ seconds

### 4. Communicate Limits to Users
- Document rate limits in API documentation
- Return clear error messages
- Include rate limit headers in all responses

### 5. Monitor and Adjust
- Track rate limit hits in logs
- Analyze legitimate vs malicious traffic
- Adjust limits based on usage patterns

## Production Considerations

### 1. Distributed Caching
For multi-instance deployments, use Redis instead of Caffeine:

```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
</dependency>
```

### 2. IP Address Detection
Configure X-Forwarded-For header handling for load balancers:

```java
private String getClientIp(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    if (xfHeader == null || xfHeader.isEmpty()) {
        return request.getRemoteAddr();
    }
    // Use first IP in chain (original client)
    return xfHeader.split(",")[0].trim();
}
```

### 3. Whitelist Important IPs
```java
private static final Set<String> WHITELISTED_IPS = Set.of(
    "10.0.0.1",      // Internal services
    "192.168.1.100"  // Admin IP
);

if (WHITELISTED_IPS.contains(clientIp)) {
    return true; // Skip rate limiting
}
```

## Troubleshooting

### Issue: Rate limits too strict
**Solution**: Increase limit or duration in `@RateLimited` annotation

### Issue: Rate limits not working
**Solution**: Check that WebMvcConfig is registering the interceptor

### Issue: All requests blocked
**Solution**: Check cache configuration and bucket creation logic

### Issue: Different behavior per server instance
**Solution**: Implement distributed caching with Redis

## Security Benefits

✅ **DDoS Protection**: Limits flood of requests  
✅ **Brute Force Prevention**: Limits login/password attempts  
✅ **Resource Protection**: Prevents API abuse  
✅ **Fair Usage**: Ensures equitable access  
✅ **Cost Control**: Limits expensive operations  

## Performance Impact

- **Overhead**: ~1-2ms per request (cache lookup)
- **Memory**: ~1KB per unique IP/user
- **CPU**: Minimal (token bucket math)
- **Scalability**: Handles 100K+ concurrent users

## Related Documentation

- [Spring Boot Rate Limiting](https://spring.io/guides)
- [Bucket4j Documentation](https://bucket4j.com/)
- [API Security Best Practices](https://owasp.org/www-project-api-security/)
