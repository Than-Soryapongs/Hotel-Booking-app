# Password Management Features Summary

## Features Implemented

### 1. Change Password (Authenticated Users)
**Endpoint:** `POST /api/user/change-password`
- Requires authentication (Bearer token)
- User must provide old password for verification
- Changes password for currently logged-in user

**Parameters:**
- `oldPassword`: Current password for verification
- `newPassword`: New password to set

---

### 2. Forgot Password (Public Access)
**Endpoint:** `POST /api/auth/forgot-password`
- No authentication required
- Sends password reset email to user
- Token expires in 1 hour

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

---

### 3. Reset Password (Public Access)
**Endpoint:** `POST /api/auth/reset-password`
- No authentication required
- Uses token from email
- Resets password and clears failed login attempts

**Request Body:**
```json
{
  "token": "reset-token-from-email",
  "newPassword": "newSecurePassword123"
}
```

---

### 4. Email Change with Verification
**Endpoints:**
- `POST /api/user/change-email` - Request email change (requires auth)
- `GET /api/user/verify-email-change?token=xxx` - Verify new email (requires auth)
- `DELETE /api/user/cancel-email-change` - Cancel pending change (requires auth)

**Features:**
- Password verification required to initiate
- Verification email sent to NEW email
- Confirmation email sent to OLD email after success
- Token expires in 24 hours

---

## Quick Comparison

| Feature | Requires Auth | Password Needed | Token Expiry | Email Sent To |
|---------|--------------|-----------------|--------------|---------------|
| Change Password | ✅ Yes | ✅ Old password | N/A | None |
| Forgot Password | ❌ No | ❌ No | 1 hour | User's email |
| Reset Password | ❌ No | ❌ No | Uses token | None (token from email) |
| Email Change | ✅ Yes | ✅ Current password | 24 hours | New email (verify) + Old email (confirm) |

---

## User Flows

### Scenario 1: User is logged in and wants to change password
1. Go to profile settings
2. Use "Change Password" feature
3. Enter old password + new password
4. Password changed immediately

### Scenario 2: User forgot password and cannot login
1. Click "Forgot Password" on login page
2. Enter email address
3. Check email for reset link
4. Click link and enter new password
5. Login with new password

### Scenario 3: User wants to change email (logged in)
1. Go to profile settings
2. Enter new email and current password
3. Check NEW email inbox for verification link
4. Click verification link
5. OLD email receives confirmation
6. Email is updated

---

## Database Schema

### User Entity - Password & Email Related Fields

```java
// Password
private String password;

// Email verification (for registration)
private String emailVerificationToken;
private LocalDateTime emailVerificationTokenExpiresAt;

// Email change
private String pendingEmail;
private String emailChangeToken;
private LocalDateTime emailChangeTokenExpiresAt;

// Password reset
private String passwordResetToken;
private LocalDateTime passwordResetTokenExpiresAt;

// Security tracking
private Integer failedLoginAttempts;
private LocalDateTime lockedUntil;
```

---

## Security Features

✅ **Password Hashing**: All passwords stored with BCrypt  
✅ **Token Expiration**: All tokens have expiration times  
✅ **One-Time Use**: Tokens cleared after successful use  
✅ **Account Locking**: Failed attempts trigger temporary locks  
✅ **Email Verification**: Ownership verified before changes  
✅ **Password Verification**: Required for sensitive operations  
✅ **Secure Token Generation**: 32-byte SecureRandom tokens  

---

## Email Templates

All features use professional HTML email templates:
- ✉️ Email Verification (registration)
- ✉️ Welcome Email (after verification)
- ✉️ Password Reset Instructions
- ✉️ Email Change Verification
- ✉️ Email Change Confirmation

---

## Documentation Files

- 📄 `EMAIL_CHANGE_FEATURE.md` - Complete email change documentation
- 📄 `FORGOT_PASSWORD_FEATURE.md` - Complete forgot password documentation
- 📄 `PASSWORD_FEATURES_SUMMARY.md` - This summary document

---

## Testing Checklist

### Change Password
- [ ] Can change password with correct old password
- [ ] Cannot change with incorrect old password
- [ ] New password is hashed correctly
- [ ] Can login with new password

### Forgot Password
- [ ] Can request reset with valid email
- [ ] Email is received with reset link
- [ ] Token expires after 1 hour
- [ ] Invalid token shows error
- [ ] Can login after reset

### Email Change
- [ ] Cannot change without password
- [ ] Verification sent to new email
- [ ] Cannot use email already in use
- [ ] Confirmation sent to old email
- [ ] Can cancel pending change
- [ ] Profile shows pendingEmail

---

## Configuration Required

```properties
# SendGrid API Key
sendgrid.api-key=your-api-key-here
sendgrid.from-email=noreply@yourdomain.com
sendgrid.from-name=Your App Name

# Base URL for email links
app.email.verification.base-url=http://localhost:3000
```

---

## API Examples with cURL

### Change Password (Authenticated)
```bash
curl -X POST http://localhost:8080/api/user/change-password \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d "oldPassword=current123&newPassword=newSecure456"
```

### Forgot Password (Public)
```bash
curl -X POST http://localhost:8080/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}'
```

### Reset Password (Public)
```bash
curl -X POST http://localhost:8080/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"token":"reset-token-here","newPassword":"newPassword123"}'
```

### Request Email Change (Authenticated)
```bash
curl -X POST http://localhost:8080/api/user/change-email \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newEmail":"newemail@example.com","password":"currentPassword"}'
```

---

## Support

For issues or questions:
1. Check the detailed documentation files
2. Verify SendGrid configuration
3. Check email spam folders
4. Review application logs
5. Ensure base URL matches frontend domain
