# Forgot Password Feature

## Overview
This feature implements a secure password reset flow that allows users who have forgotten their password to reset it via email verification.

## How It Works

### 1. User Requests Password Reset
- User provides their email address
- System generates a secure reset token (expires in 1 hour)
- Password reset email sent to user's email address
- Token stored in database with expiration time

### 2. User Receives Email
- Email contains a reset link with the token
- Link directs to password reset page
- User has 1 hour to use the link before it expires

### 3. User Resets Password
- User clicks the link and enters new password
- System validates the token and expiration
- Password is updated and token is cleared
- Failed login attempts are reset (if account was locked)

## API Endpoints

### Forgot Password (Request Reset)
```http
POST /api/auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**Response (Success):**
```json
{
  "message": "Password reset instructions have been sent to your email address.",
  "email": "user@example.com"
}
```

**Response (Error - User Not Found):**
```json
{
  "error": "User not found with this email address"
}
```

### Reset Password
```http
POST /api/auth/reset-password
Content-Type: application/json

{
  "token": "secure-reset-token-here",
  "newPassword": "newSecurePassword123"
}
```

**Response (Success):**
```json
{
  "message": "Your password has been reset successfully. You can now log in with your new password.",
  "username": "johndoe"
}
```

**Response (Error - Invalid Token):**
```json
{
  "error": "Invalid password reset token"
}
```

**Response (Error - Expired Token):**
```json
{
  "error": "Password reset token has expired. Please request a new one."
}
```

## Database Changes

### User Entity - New Fields
```java
@Column(length = 255)
private String passwordResetToken;

@Column
private LocalDateTime passwordResetTokenExpiresAt;
```

## Security Features

1. **Token Expiration**: Reset tokens expire after 1 hour for security
2. **Secure Token Generation**: Uses SecureRandom with Base64 encoding (32 bytes)
3. **One-Time Use**: Token is cleared after successful password reset
4. **Password Hashing**: New password is hashed using BCrypt before storage
5. **Account Unlock**: Failed login attempts are reset upon successful password reset
6. **Email Verification**: Only registered email addresses receive reset instructions

## Email Template

The password reset email includes:
- Greeting with username
- Clear explanation of the password reset request
- Prominent "Reset Password" button
- Plain text link as backup
- Expiration notice (1 hour)
- Security notice (ignore if not requested)
- Professional HTML styling

## Flow Diagram

```
User Forgot Password
       ↓
Enter Email Address
       ↓
POST /api/auth/forgot-password
       ↓
System validates email exists
       ↓
Generate secure token (1 hour expiry)
       ↓
Store token in database
       ↓
Send email with reset link
       ↓
User clicks link in email
       ↓
User enters new password
       ↓
POST /api/auth/reset-password
       ↓
Validate token & expiration
       ↓
Hash new password
       ↓
Update password & clear token
       ↓
Reset failed login attempts
       ↓
Success - User can login
```

## Error Handling

| Error | Message |
|-------|---------|
| Email not found | "User not found with this email address" |
| Invalid token | "Invalid password reset token" |
| Expired token | "Password reset token has expired. Please request a new one." |
| Email send failure | "Failed to send password reset email. Please try again later." |
| Validation errors | Specific validation messages for email/password format |

## Password Requirements

- Minimum 6 characters (enforced by validation)
- Should include mix of letters, numbers, and special characters (recommended)
- Cannot be empty or whitespace only

## Security Considerations

### Rate Limiting (Recommended)
Consider implementing rate limiting to prevent abuse:
- Max 3 password reset requests per email per hour
- Max 5 failed reset attempts before temporary lockout

### Additional Security (Future Enhancements)
1. **Two-Factor Authentication**: Require 2FA before allowing password reset
2. **Security Questions**: Add security questions as additional verification
3. **IP Tracking**: Log IP addresses of reset requests
4. **Notification Email**: Send notification to user when password is changed
5. **Old Password Prevention**: Prevent reuse of recent passwords

## Testing the Feature

### Test Case 1: Successful Password Reset
1. Request password reset with valid email
2. Check email inbox for reset link
3. Click link and enter new password
4. Verify password is changed
5. Login with new password

### Test Case 2: Expired Token
1. Request password reset
2. Wait for token to expire (or manually expire in DB)
3. Try to reset password
4. Verify error message about expired token

### Test Case 3: Invalid Token
1. Try to reset password with random/invalid token
2. Verify error message about invalid token

### Test Case 4: Non-existent Email
1. Request password reset with email not in system
2. Verify appropriate error message

## Integration with Frontend

### Forgot Password Page
```javascript
// Example: Forgot password form submission
const handleForgotPassword = async (email) => {
  try {
    const response = await fetch('/api/auth/forgot-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    });
    
    const data = await response.json();
    
    if (response.ok) {
      // Show success message
      alert(data.message);
    } else {
      // Show error message
      alert(data.error);
    }
  } catch (error) {
    alert('Failed to send reset email. Please try again.');
  }
};
```

### Reset Password Page
```javascript
// Example: Reset password form submission
const handleResetPassword = async (token, newPassword) => {
  try {
    const response = await fetch('/api/auth/reset-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, newPassword })
    });
    
    const data = await response.json();
    
    if (response.ok) {
      // Show success message and redirect to login
      alert(data.message);
      window.location.href = '/login';
    } else {
      // Show error message
      alert(data.error);
    }
  } catch (error) {
    alert('Failed to reset password. Please try again.');
  }
};
```

## Configuration

Ensure these properties are set in `application.properties`:

```properties
# SendGrid Email Configuration
sendgrid.api-key=your-sendgrid-api-key
sendgrid.from-email=noreply@yourdomain.com
sendgrid.from-name=Your App Name

# Base URL for password reset links
app.email.verification.base-url=http://localhost:3000
```

## Maintenance

### Token Cleanup
Consider implementing a scheduled job to clean up expired tokens:

```java
@Scheduled(cron = "0 0 0 * * *") // Run daily at midnight
public void cleanupExpiredTokens() {
    LocalDateTime now = LocalDateTime.now();
    userRepository.clearExpiredPasswordResetTokens(now);
}
```

## Support & Troubleshooting

### Common Issues

1. **Email not received**: Check spam folder, verify SendGrid configuration
2. **Link not working**: Verify base URL configuration matches frontend domain
3. **Token expired**: User needs to request new password reset
4. **Multiple requests**: Latest token invalidates previous ones

## Related Features

- [Email Verification](./EMAIL_VERIFICATION.md)
- [Email Change with Verification](./EMAIL_CHANGE_FEATURE.md)
- [Change Password (Authenticated)](./CHANGE_PASSWORD.md)
