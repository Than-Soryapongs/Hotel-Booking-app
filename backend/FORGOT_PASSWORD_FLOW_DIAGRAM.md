# Forgot Password - Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FORGOT PASSWORD FLOW                                 │
└─────────────────────────────────────────────────────────────────────────────┘

STEP 1: REQUEST PASSWORD RESET
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

   ┌───────┐
   │ User  │  "I forgot my password"
   └───┬───┘
       │
       │ Goes to login page
       │
       ▼
   ┌────────────────┐
   │ Login Page     │
   │ [Forgot Pass?] │───┐ Clicks "Forgot Password?"
   └────────────────┘   │
                        │
                        ▼
                  ┌──────────────────┐
                  │ Enter Email Page │
                  └────────┬─────────┘
                           │
                           │ Enters: user@example.com
                           │
                           ▼
                  POST /api/auth/forgot-password
                  { "email": "user@example.com" }
                           │
                           ▼
                  ┌────────────────┐
                  │  AuthService   │
                  └────────┬───────┘
                           │
                           ├─► 1. Find user by email
                           │   └─► ✓ User exists
                           │
                           ├─► 2. Generate secure token
                           │   └─► Token: "abc123xyz..."
                           │
                           ├─► 3. Set expiration (1 hour)
                           │   └─► Expires at: 2025-10-14 15:30:00
                           │
                           ├─► 4. Save to database
                           │   ├─► passwordResetToken = "abc123xyz..."
                           │   └─► passwordResetTokenExpiresAt = 2025-10-14 15:30:00
                           │
                           ├─► 5. Send email via EmailService
                           │   └─► To: user@example.com
                           │
                           ▼
                  Response: {
                    "message": "Password reset instructions sent",
                    "email": "user@example.com"
                  }
                           │
                           ▼
                  ┌────────────────┐
                  │ Success Page   │
                  │ "Check email!" │
                  └────────────────┘


STEP 2: EMAIL DELIVERY
━━━━━━━━━━━━━━━━━━━━━

                  ┌────────────────┐
                  │   SendGrid     │
                  └────────┬───────┘
                           │
                           │ Sends HTML email
                           │
                           ▼
                  ┌────────────────────────────┐
                  │    User's Email Inbox      │
                  ├────────────────────────────┤
                  │ From: Hotel Booking        │
                  │ Subject: Reset Your Pass   │
                  │                            │
                  │ Hello johndoe!             │
                  │                            │
                  │ Click to reset:            │
                  │ [Reset Password Button]    │
                  │                            │
                  │ Link expires in 1 hour     │
                  └────────────────────────────┘


STEP 3: USER CLICKS EMAIL LINK
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

   ┌───────┐
   │ User  │ Clicks reset button
   └───┬───┘
       │
       │ Opens link: 
       │ http://frontend.com/reset-password?token=abc123xyz...
       │
       ▼
   ┌──────────────────────┐
   │ Reset Password Page  │
   │                      │
   │ Enter new password:  │
   │ [_______________]    │
   │                      │
   │ Confirm password:    │
   │ [_______________]    │
   │                      │
   │ [Reset Password]     │
   └──────────┬───────────┘
              │
              │ User enters: newSecure123
              │
              ▼
   POST /api/auth/reset-password
   {
     "token": "abc123xyz...",
     "newPassword": "newSecure123"
   }


STEP 4: PASSWORD RESET PROCESSING
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

              ┌────────────────┐
              │  AuthService   │
              └────────┬───────┘
                       │
                       ├─► 1. Find user by token
                       │   └─► SELECT * FROM users 
                       │       WHERE passwordResetToken = 'abc123xyz...'
                       │
                       ├─► 2. Check token expiration
                       │   ├─► Current time: 2025-10-14 14:45:00
                       │   ├─► Expires at:   2025-10-14 15:30:00
                       │   └─► ✓ Valid (not expired)
                       │
                       ├─► 3. Hash new password
                       │   └─► BCrypt hash: $2a$10$N9q...
                       │
                       ├─► 4. Update database
                       │   ├─► password = $2a$10$N9q...
                       │   ├─► passwordResetToken = NULL
                       │   ├─► passwordResetTokenExpiresAt = NULL
                       │   ├─► failedLoginAttempts = 0
                       │   └─► lockedUntil = NULL
                       │
                       ▼
              Response: {
                "message": "Password reset successfully",
                "username": "johndoe"
              }
                       │
                       ▼
              ┌────────────────┐
              │ Success Page   │
              │ "Password      │
              │  changed!"     │
              │                │
              │ [Go to Login]  │
              └────────────────┘


STEP 5: USER LOGS IN
━━━━━━━━━━━━━━━━━━━

   ┌───────┐
   │ User  │ Goes to login page
   └───┬───┘
       │
       ▼
   ┌────────────────┐
   │  Login Page    │
   └────────┬───────┘
            │
            │ Enters:
            │ - Username: johndoe
            │ - Password: newSecure123
            │
            ▼
   POST /api/auth/login
   {
     "usernameOrEmail": "johndoe",
     "password": "newSecure123"
   }
            │
            ▼
   ┌────────────────┐
   │  AuthService   │
   └────────┬───────┘
            │
            ├─► 1. Find user
            ├─► 2. Check if enabled
            ├─► 3. Check if locked
            ├─► 4. Verify password ✓
            ├─► 5. Generate JWT tokens
            └─► 6. Update last login
            │
            ▼
   Response: {
     "accessToken": "eyJhbG...",
     "refreshToken": "eyJhbG...",
     "user": { ... }
   }
            │
            ▼
   ┌────────────────┐
   │ Dashboard Page │
   │ "Welcome back!"│
   └────────────────┘


ERROR SCENARIOS
━━━━━━━━━━━━━━━

❌ Email Not Found
   POST /api/auth/forgot-password
   { "email": "notfound@example.com" }
   │
   └─► Error: "User not found with this email address"


❌ Invalid Token
   POST /api/auth/reset-password
   { "token": "invalid-token", "newPassword": "..." }
   │
   └─► Error: "Invalid password reset token"


❌ Expired Token
   POST /api/auth/reset-password
   { "token": "expired-token", "newPassword": "..." }
   │
   ├─► Current time: 2025-10-14 16:00:00
   ├─► Token expires: 2025-10-14 15:30:00
   └─► Error: "Password reset token has expired. Please request a new one."


❌ Weak Password
   POST /api/auth/reset-password
   { "token": "valid-token", "newPassword": "123" }
   │
   └─► Error: "Password must be at least 6 characters"


DATABASE STATE CHANGES
━━━━━━━━━━━━━━━━━━━━━

BEFORE REQUEST:
┌──────────────────────────────────────────────────────────┐
│ User Table                                               │
├──────────────┬─────────────┬──────────────┬─────────────┤
│ email        │ password    │ resetToken   │ resetExpiry │
├──────────────┼─────────────┼──────────────┼─────────────┤
│ user@ex.com  │ $2a$10$Abc │ NULL         │ NULL        │
└──────────────┴─────────────┴──────────────┴─────────────┘

AFTER FORGOT PASSWORD:
┌──────────────────────────────────────────────────────────────────┐
│ User Table                                                       │
├──────────────┬─────────────┬──────────────┬──────────────────────┤
│ email        │ password    │ resetToken   │ resetExpiry          │
├──────────────┼─────────────┼──────────────┼──────────────────────┤
│ user@ex.com  │ $2a$10$Abc │ abc123xyz... │ 2025-10-14 15:30:00  │
└──────────────┴─────────────┴──────────────┴──────────────────────┘

AFTER RESET PASSWORD:
┌──────────────────────────────────────────────────────────┐
│ User Table                                               │
├──────────────┬─────────────┬──────────────┬─────────────┤
│ email        │ password    │ resetToken   │ resetExpiry │
├──────────────┼─────────────┼──────────────┼─────────────┤
│ user@ex.com  │ $2a$10$Xyz │ NULL         │ NULL        │
└──────────────┴─────────────┴──────────────┴─────────────┘
                    ↑ NEW PASSWORD


SECURITY HIGHLIGHTS
━━━━━━━━━━━━━━━━━━━

🔒 Token is 32-byte SecureRandom (256 bits of entropy)
🔒 Token expires in 1 hour (3600 seconds)
🔒 Token is single-use (cleared after reset)
🔒 Password hashed with BCrypt (cost factor 10)
🔒 Failed login attempts reset on password change
🔒 Account unlocked on password change
🔒 No authentication required for public endpoints
🔒 Email ownership verified (link sent to user's email)
```
