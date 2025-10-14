# Rate Limiting - Quick Reference

## 📊 Current Rate Limits

### Authentication Endpoints
```
POST /api/auth/signup           → 5 requests / 1 hour       (IP-based)
POST /api/auth/login            → 10 requests / 5 minutes   (IP-based)
POST /api/auth/resend-verification → 3 requests / 10 minutes (IP-based)
POST /api/auth/forgot-password  → 3 requests / 10 minutes   (IP-based)
POST /api/auth/reset-password   → 5 requests / 10 minutes   (IP-based)
```

### User Profile Endpoints
```
POST /api/user/change-password  → 5 requests / 10 minutes   (USER-based)
POST /api/user/change-email     → 3 requests / 1 hour       (USER-based)
POST /api/user/profile-picture  → 10 requests / 1 hour      (USER-based)
```

## 🚀 Quick Start

### 1. Add Rate Limiting to Endpoint

```java
@PostMapping("/api/example")
@RateLimited(limit = 100, duration = 60, type = RateLimitType.IP)
public ResponseEntity<?> exampleEndpoint() {
    // Your code here
}
```

### 2. Rate Limit Types

```java
RateLimitType.IP           // Based on IP address
RateLimitType.USER         // Based on authenticated user
RateLimitType.IP_AND_USER  // Both IP and user combined
```

### 3. Common Configurations

```java
// Strict (sensitive operations)
@RateLimited(limit = 1, duration = 60, type = RateLimitType.USER)

// Moderate (write operations)
@RateLimited(limit = 10, duration = 300, type = RateLimitType.IP)

// Generous (read operations)
@RateLimited(limit = 100, duration = 60, type = RateLimitType.IP)
```

## 📨 Response Headers

Every request includes:
```
X-Rate-Limit-Limit: 10
X-Rate-Limit-Remaining: 7
X-Rate-Limit-Duration: 300
```

## ⚠️ Error Response (HTTP 429)

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

## 🧪 Testing Commands

### cURL Test
```bash
# Test rate limit
for i in {1..12}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"usernameOrEmail":"test","password":"test"}' \
    -i | grep -E "HTTP|X-Rate-Limit"
done
```

### Check Headers
```bash
curl -i http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 🔧 Configuration Files

### Dependencies (pom.xml)
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```

### Core Files
```
src/main/java/com/system/hotel_booking/
├── ratelimit/
│   ├── RateLimited.java            (Annotation)
│   ├── RateLimitType.java          (Enum)
│   ├── RateLimitService.java       (Business Logic)
│   └── RateLimitInterceptor.java   (Request Interceptor)
├── config/
│   ├── CacheConfig.java            (Cache Setup)
│   └── WebMvcConfig.java           (Register Interceptor)
└── common/
    ├── RateLimitExceededException.java
    └── GlobalExceptionHandler.java  (Error Handling)
```

## 📝 Common Patterns

### Pattern 1: Public API Endpoint
```java
@GetMapping("/api/public/hotels")
@RateLimited(limit = 1000, duration = 60, type = RateLimitType.IP)
public ResponseEntity<List<Hotel>> getHotels() {
    return ResponseEntity.ok(hotelService.findAll());
}
```

### Pattern 2: Authenticated Write Operation
```java
@PostMapping("/api/bookings")
@RateLimited(limit = 20, duration = 3600, type = RateLimitType.USER)
public ResponseEntity<Booking> createBooking(@RequestBody BookingRequest request) {
    return ResponseEntity.ok(bookingService.create(request));
}
```

### Pattern 3: Sensitive Admin Operation
```java
@DeleteMapping("/api/admin/users/{id}")
@RateLimited(limit = 1, duration = 60, type = RateLimitType.USER)
public ResponseEntity<?> deleteUser(@PathVariable Long id) {
    adminService.deleteUser(id);
    return ResponseEntity.ok().build();
}
```

## 🎯 Best Practices

| Operation Type | Recommended Limit | Duration | Type |
|----------------|-------------------|----------|------|
| Login/Auth | 5-10 | 5-10 min | IP |
| Password Reset | 3-5 | 10-15 min | IP |
| Read (Public) | 100-1000 | 60 sec | IP |
| Read (Auth) | 100-500 | 60 sec | USER |
| Write (Simple) | 20-50 | 60 sec | USER |
| Write (Complex) | 5-10 | 60 sec | USER |
| File Upload | 10-20 | 1 hour | USER |
| Email Send | 3-5 | 10 min | IP |

## 🛠️ Troubleshooting

### Issue: Rate limit too strict
```java
// Increase limit or duration
@RateLimited(limit = 20, duration = 60)  // was 10
```

### Issue: Need to exclude endpoint
In `WebMvcConfig.java`:
```java
.excludePathPatterns("/api/public/**")
```

### Issue: Check current limits
```bash
curl -i http://localhost:8080/api/endpoint
# Check X-Rate-Limit-* headers
```

## 📚 More Information

- Full Documentation: `RATE_LIMITING.md`
- Implementation Details: See `ratelimit/` package
- Configuration: `WebMvcConfig.java` and `CacheConfig.java`
