# Function1 - Login (AuthService.login)

| Function Code | Function1-Login | Function Name | | | Login |
|---------------|-----------------|---------------|---|---|-------|
| **Created By** | [Your name] | **Executed By** | | | [Your name] |
| **Lines of code** | ~28 | **Lack of test cases** | | | 0 |
| **Source** | `identity-service/.../AuthServiceImpl.java#login()` | **Test class** | | | `AuthServiceImplTest.java` |
| **Test requirement** | Xác thực người dùng qua email + password, trả về access token và refresh token. Ném `UnauthorizedException` nếu sai credentials. | | | | |

| Passed | Failed | Untested | N | A | B | Total Test Cases |
|--------|--------|----------|---|---|---|------------------|
| 5      | 0      | 0        | 2 | 3 | 0 | 5                |

---

## Test Case Matrix

|             | Condition / Expected                        | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 |
|-------------|---------------------------------------------|:-------:|:-------:|:-------:|:-------:|:-------:|
| **Condition** | **Precondition**                          |         |         |         |         |         |
|             | Database accessible                         | O       | O       | O       | O       | O       |
|             | `userRepository` mocked                     | O       | O       | O       | O       | O       |
|             | `passwordEncoder` mocked                    | O       | O       | O       | O       | O       |
|             | `jwtUtil` mocked                            | O       | O       | O       | O       | O       |
|             | **Email (request.email)**                   |         |         |         |         |         |
|             | `user@trustfund.vn` (exists in DB)          | O       |         | O       | O       | O       |
|             | `notfound@trustfund.vn` (not in DB)         |         | O       |         |         |         |
|             | **Password (request.password)**             |         |         |         |         |         |
|             | Correct password `Pa$$w0rd`                 | O       |         |         | O       | O       |
|             | Wrong password `WrongPass`                  |         |         | O       |         |         |
|             | `null`                                      |         | O       |         |         |         |
|             | **User state**                              |         |         |         |         |         |
|             | `isActive = true`                           | O       |         | O       |         | O       |
|             | `isActive = false` (deactivated)            |         |         |         | O       |         |
|             | Role = `DONOR`                              | O       |         | O       | O       |         |
|             | Role = `STAFF`                              |         |         |         |         | O       |
| **Confirm** | **Return value**                            |         |         |         |         |         |
|             | Non-null `AuthResponse`                     | O       |         |         | O       | O       |
|             | `accessToken` generated                     | O       |         |         | O       | O       |
|             | `refreshToken` generated                    | O       |         |         | O       | O       |
|             | `user` mapped to `UserInfo`                 | O       |         |         | O       | O       |
|             | **Exception**                               |         |         |         |         |         |
|             | `UnauthorizedException("Invalid email or password")` |  | O       | O       |         |         |
|             | No exception thrown                         | O       |         |         | O       | O       |
|             | **Repository interaction**                  |         |         |         |         |         |
|             | `userRepository.findByEmail()` called       | O       | O       | O       | O       | O       |
|             | `passwordEncoder.matches()` called          | O       |         | O       | O       | O       |
|             | `jwtUtil.generateToken()` called            | O       |         |         | O       | O       |
|             | **Log message**                             |         |         |         |         |         |
|             | "User logged in successfully ..."           | O       |         |         | O       | O       |
| **Result**  | Type (N: Normal, A: Abnormal, B: Boundary)  | N       | A       | A       | N       | N       |
|             | Passed/Failed                               | P       | P       | P       | P       | P       |
|             | Executed Date                               | 12/04   | 12/04   | 12/04   | 12/04   | 12/04   |
|             | Defect ID                                   |         |         |         |         |         |

---

## Mô tả test case

### UTCID01 — Login thành công với DONOR
- **Type:** Normal | **Status:** Passed
- **Input:** email = `user@trustfund.vn`, password = `Pa$$w0rd`, isActive = true, role = DONOR
- **Expected:** trả về `AuthResponse` chứa accessToken + refreshToken + UserInfo, không throw exception
- **Tương ứng test method:** `AuthServiceImplTest.login_success_returnsTokens()`

### UTCID02 — Email không tồn tại
- **Type:** Abnormal | **Status:** Passed
- **Input:** email = `notfound@trustfund.vn`, password = null
- **Expected:** throw `UnauthorizedException("Invalid email or password")`, không gọi passwordEncoder
- **Tương ứng test method:** `AuthServiceImplTest.login_emailNotFound_throwsUnauthorized()`

### UTCID03 — Sai mật khẩu
- **Type:** Abnormal | **Status:** Passed
- **Input:** email tồn tại, password = `WrongPass`
- **Expected:** `passwordEncoder.matches()` trả về false → throw `UnauthorizedException`
- **Tương ứng test method:** `AuthServiceImplTest.login_wrongPassword_throwsUnauthorized()`

### UTCID04 — Tài khoản bị deactivate vẫn login được
- **Type:** Normal | **Status:** Passed
- **Input:** email + password đúng, `isActive = false`
- **Expected:** vẫn trả về token (theo comment trong code — FE sẽ handle restricted view)
- **Tương ứng test method:** `AuthServiceImplTest.login_deactivatedUser_stillReturnsTokens()`

### UTCID05 — Login thành công với STAFF
- **Type:** Normal | **Status:** Passed
- **Input:** email + password đúng, role = STAFF
- **Expected:** token chứa role STAFF, trả về thành công
- **Tương ứng test method:** `AuthServiceImplTest.login_staffRole_tokenContainsStaffRole()`
