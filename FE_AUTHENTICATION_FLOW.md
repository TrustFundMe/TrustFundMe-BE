# Frontend Authentication Flow Guide

## Tổng quan

Hệ thống sử dụng OTP (One-Time Password) 6 số để:
1. **Verify email** khi đăng ký
2. **Reset password** khi quên mật khẩu

---

## 1. Flow Verify Email (Sau khi đăng ký)

### Bước 1: User đăng ký
```
POST /api/auth/register
{
  "email": "user@example.com",
  "password": "Password123",
  "fullName": "Nguyen Van A",
  "phoneNumber": "0900000001"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "Nguyen Van A",
    "phoneNumber": "0900000001",
    "role": "USER",
    "verified": false  // ← Chưa verify
  }
}
```

**Lưu ý:** User mới đăng ký có `verified: false`

### Bước 2: Gửi OTP để verify email
```
POST /api/auth/send-otp
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP has been sent to your email"
}
```

**Frontend cần làm:**
- Hiển thị form nhập OTP
- User nhập OTP từ email

### Bước 3: Verify OTP
```
POST /api/auth/verify-otp
{
  "email": "user@example.com",
  "otp": "123456"
}
```

**Response nếu OTP đúng:**
```json
{
  "success": true,
  "message": "OTP verified successfully. You can now reset your password.",
  "token": "eyJhbGciOiJIUzUxMiJ9..."  // ← Token để verify email
}
```

**Response nếu OTP sai:**
```json
{
  "error": "Invalid or expired OTP"
}
// Không có token
```

### Bước 4: Verify email (dùng token từ bước 3)
```
POST /api/auth/verify-email
{
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Email verified successfully"
}
```

**Sau khi verify:**
- User có thể đăng nhập và sử dụng đầy đủ tính năng
- `user.verified = true`

---

## 2. Flow Reset Password (Quên mật khẩu)

### Bước 1: User request reset password
```
POST /api/auth/send-otp
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP has been sent to your email"
}
```

**Frontend cần làm:**
- Hiển thị form nhập OTP
- User nhập OTP từ email

### Bước 2: Verify OTP
```
POST /api/auth/verify-otp
{
  "email": "user@example.com",
  "otp": "123456"
}
```

**Response nếu OTP đúng:**
```json
{
  "success": true,
  "message": "OTP verified successfully. You can now reset your password.",
  "token": "eyJhbGciOiJIUzUxMiJ9..."  // ← Token để reset password
}
```

**Response nếu OTP sai:**
```json
{
  "error": "Invalid or expired OTP"
}
// Không có token → Không thể reset password
```

### Bước 3: Reset password (dùng token từ bước 2)
```
POST /api/auth/reset-password
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "newPassword": "NewPassword123!@#"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Password reset successfully"
}
```

**Sau khi reset:**
- User có thể đăng nhập với password mới

---

## 3. UI Flow cho Frontend

### Màn hình Verify Email (Sau đăng ký)

```
┌─────────────────────────────┐
│   Verify Your Email         │
├─────────────────────────────┤
│                             │
│  We've sent a verification  │
│  code to:                   │
│  user@example.com           │
│                             │
│  [  ] [  ] [  ] [  ] [  ] [  ]  ← OTP input
│                             │
│  [Resend Code]              │
│                             │
│  [Verify Email]             │
│                             │
└─────────────────────────────┘
```

**Logic:**
1. User đăng ký thành công → Redirect đến màn hình verify email
2. Tự động gọi `POST /api/auth/send-otp`
3. User nhập OTP → Gọi `POST /api/auth/verify-otp`
4. Nếu có token → Gọi `POST /api/auth/verify-email` với token
5. Verify thành công → Redirect đến trang chủ

### Màn hình Reset Password

```
┌─────────────────────────────┐
│   Reset Password             │
├─────────────────────────────┤
│                             │
│  Step 1: Enter Email        │
│  [email@example.com]        │
│  [Send OTP]                 │
│                             │
│  Step 2: Enter OTP          │
│  [  ] [  ] [  ] [  ] [  ] [  ]  ← OTP input
│  [Verify OTP]               │
│                             │
│  Step 3: New Password       │
│  [New Password]             │
│  [Confirm Password]         │
│  [Reset Password]           │
│                             │
└─────────────────────────────┘
```

**Logic:**
1. User nhập email → Gọi `POST /api/auth/send-otp`
2. User nhập OTP → Gọi `POST /api/auth/verify-otp`
3. Nếu có token → Hiển thị form nhập password mới
4. User nhập password → Gọi `POST /api/auth/reset-password` với token
5. Reset thành công → Redirect đến trang đăng nhập

---

## 4. API Endpoints Summary

| Endpoint | Method | Purpose | Request | Response |
|----------|--------|---------|---------|----------|
| `/api/auth/register` | POST | Đăng ký user mới | email, password, fullName, phoneNumber | accessToken, user (verified: false) |
| `/api/auth/send-otp` | POST | Gửi OTP (dùng cho verify email hoặc reset password) | email | success, message |
| `/api/auth/verify-otp` | POST | Verify OTP | email, otp | success, message, **token** (nếu đúng) |
| `/api/auth/verify-email` | POST | Verify email sau khi verify OTP | token | success, message |
| `/api/auth/reset-password` | POST | Reset password sau khi verify OTP | token, newPassword | success, message |
| `/api/auth/login` | POST | Đăng nhập | email, password | accessToken, user |

---

## 5. Token Management

### Token từ verify-otp
- **Type:** JWT token
- **Expiration:** 30 phút
- **Purpose:** 
  - Verify email (nếu user mới đăng ký)
  - Reset password (nếu user quên mật khẩu)
- **Lưu ý:** Token chỉ dùng 1 lần

### Cách phân biệt:
- **Verify email:** User mới đăng ký, `verified: false` → Dùng token để verify email
- **Reset password:** User quên mật khẩu → Dùng token để reset password

---

## 6. Error Handling

### OTP sai:
```json
{
  "error": "Invalid or expired OTP"
}
```
**Frontend:** Hiển thị thông báo lỗi, cho phép nhập lại OTP

### OTP đã dùng:
```json
{
  "error": "OTP has already been used"
}
```
**Frontend:** Yêu cầu gửi OTP mới

### OTP hết hạn:
```json
{
  "error": "OTP has expired"
}
```
**Frontend:** Hiển thị nút "Resend OTP"

### Token hết hạn:
```json
{
  "error": "Reset token has expired"
}
```
**Frontend:** Yêu cầu verify OTP lại

---

## 7. Best Practices cho Frontend

1. **Lưu token:** Sau khi verify OTP thành công, lưu token vào state/localStorage
2. **Auto redirect:** Sau khi verify email/reset password thành công, tự động redirect
3. **Resend OTP:** Cho phép user gửi lại OTP nếu không nhận được
4. **OTP input:** Tự động focus sang ô tiếp theo khi nhập
5. **Loading state:** Hiển thị loading khi đang gửi OTP/verify
6. **Error display:** Hiển thị lỗi rõ ràng cho user
7. **Token expiration:** Kiểm tra token còn hạn trước khi gọi API

---

## 8. Example Code (React/TypeScript)

```typescript
// Verify Email Flow
const handleVerifyEmail = async () => {
  // Step 1: Send OTP
  await fetch('/api/auth/send-otp', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: userEmail })
  });

  // Step 2: User enters OTP → Verify
  const verifyResponse = await fetch('/api/auth/verify-otp', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: userEmail, otp: enteredOtp })
  });
  
  const { token } = await verifyResponse.json();
  
  // Step 3: Verify email with token
  await fetch('/api/auth/verify-email', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token })
  });
};

// Reset Password Flow
const handleResetPassword = async () => {
  // Step 1: Send OTP
  await fetch('/api/auth/send-otp', {
    method: 'POST',
    body: JSON.stringify({ email: userEmail })
  });

  // Step 2: Verify OTP
  const verifyResponse = await fetch('/api/auth/verify-otp', {
    method: 'POST',
    body: JSON.stringify({ email: userEmail, otp: enteredOtp })
  });
  
  const { token } = await verifyResponse.json();
  
  // Step 3: Reset password
  await fetch('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ 
      token, 
      newPassword: newPassword 
    })
  });
};
```
