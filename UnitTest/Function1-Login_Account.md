# Function1-Login Account

| **Function Code** | Function1-Login Account | | **Function Name** | | Login Account |
|-------------------|-------------------------|---|-------------------|---|---------------|
| **Created By**    | DatTDQ                  | | **Executed By**   | | DatTDQ        |
| **Lines of code** | 28                      | | **Lack of test cases** | | 0        |
| **Test requirement** | Xác thực người dùng đăng nhập qua email + password. Trả về access token, refresh token và thông tin user khi thành công. Ném `UnauthorizedException` khi email không tồn tại hoặc password sai. Cho phép login với tài khoản đã bị deactivate (FE sẽ handle restricted view). | | | | |

| Passed | Failed | Untested | N | A | B | Total Test Cases |
|:------:|:------:|:--------:|:-:|:-:|:-:|:----------------:|
| 11     | 0      | 0        | 5 | 6 | 0 | 11               |

---

## Test Case Matrix

|             | Condition / Expected                              | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 | UTCID07 | UTCID08 | UTCID09 | UTCID10 | UTCID11 |
|-------------|---------------------------------------------------|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:|:-------:|
| **Condition** | **Precondition**                                |         |         |         |         |         |         |         |         |         |         |         |
|             | Database accessible                                | O       | O       | O       | O       | O       | O       | O       | O       | O       | O       | O       |
|             | `userRepository` mocked                            | O       | O       | O       | O       | O       | O       | O       | O       | O       | O       | O       |
|             | `passwordEncoder` mocked                           | O       | O       | O       | O       | O       | O       | O       | O       | O       | O       | O       |
|             | `jwtUtil` mocked                                   | O       | O       | O       | O       | O       | O       | O       | O       | O       | O       | O       |
|             | **email (request.email)**                          |         |         |         |         |         |         |         |         |         |         |         |
|             | `donor@trustfund.vn` (exists)                      | O       |         |         | O       | O       |         |         |         |         |         | O       |
|             | `staff@trustfund.vn` (exists)                      |         |         |         |         |         |         |         |         |         | O       |         |
|             | `admin@trustfund.vn` (exists)                      |         |         |         |         |         |         |         |         | O       |         |         |
|             | `notfound@trustfund.vn` (not in DB)                |         | O       |         |         |         |         |         |         |         |         |         |
|             | Empty / null                                       |         |         |         |         |         | O       |         |         |         |         |         |
|             | Invalid format `abc@`                              |         |         |         |         |         |         | O       |         |         |         |         |
|             | **password (request.password)**                    |         |         |         |         |         |         |         |         |         |         |         |
|             | Correct password `Pa$$w0rd`                        | O       |         |         | O       |         |         |         |         | O       | O       | O       |
|             | Wrong password `WrongPass`                         |         |         | O       |         |         |         |         |         |         |         |         |
|             | Empty / null                                       |         | O       |         |         | O       | O       | O       | O       |         |         |         |
|             | Length < 6 (boundary)                              |         |         |         |         |         |         |         | O       |         |         |         |
|             | **User state**                                     |         |         |         |         |         |         |         |         |         |         |         |
|             | `isActive = true`                                  | O       |         | O       |         |         |         |         |         | O       | O       | O       |
|             | `isActive = false`                                 |         |         |         | O       |         |         |         |         |         |         |         |
|             | **Role**                                           |         |         |         |         |         |         |         |         |         |         |         |
|             | DONOR                                              | O       |         | O       | O       | O       |         |         |         |         |         | O       |
|             | STAFF                                              |         |         |         |         |         |         |         |         |         | O       |         |
|             | ADMIN                                              |         |         |         |         |         |         |         |         | O       |         |         |
| **Confirm** | **Return value**                                   |         |         |         |         |         |         |         |         |         |         |         |
|             | Non-null `AuthResponse`                            | O       |         |         | O       |         |         |         |         | O       | O       | O       |
|             | `accessToken` not null                             | O       |         |         | O       |         |         |         |         | O       | O       | O       |
|             | `refreshToken` not null                            | O       |         |         | O       |         |         |         |         | O       | O       | O       |
|             | `user.role` matches input role                     | O       |         |         | O       |         |         |         |         | O       | O       | O       |
|             | `expiresIn > 0`                                    | O       |         |         | O       |         |         |         |         | O       | O       | O       |
|             | **Exception thrown**                               |         |         |         |         |         |         |         |         |         |         |         |
|             | `UnauthorizedException("Invalid email or password")` |       | O       | O       |         | O       | O       | O       | O       |         |         |         |
|             | No exception                                       | O       |         |         | O       |         |         |         |         | O       | O       | O       |
|             | **Repository / Encoder calls**                     |         |         |         |         |         |         |         |         |         |         |         |
|             | `userRepository.findByEmail()` invoked             | O       | O       | O       | O       | O       | O       | O       | O       | O       | O       | O       |
|             | `passwordEncoder.matches()` invoked                | O       |         | O       | O       |         |         |         |         | O       | O       | O       |
|             | `jwtUtil.generateToken()` invoked                  | O       |         |         | O       |         |         |         |         | O       | O       | O       |
|             | `jwtUtil.generateRefreshToken()` invoked           | O       |         |         | O       |         |         |         |         | O       | O       | O       |
|             | **Log message**                                    |         |         |         |         |         |         |         |         |         |         |         |
|             | "User logged in successfully (Active: true)"       | O       |         |         |         |         |         |         |         | O       | O       | O       |
|             | "User logged in successfully (Active: false)"      |         |         |         | O       |         |         |         |         |         |         |         |
| **Result**  | Type (N: Normal, A: Abnormal, B: Boundary)         | N       | A       | A       | N       | A       | A       | A       | B       | N       | N       | N       |
|             | Passed/Failed                                      | P       | P       | P       | P       | P       | P       | P       | P       | P       | P       | P       |
|             | Executed Date                                      | 12/04   | 12/04   | 12/04   | 12/04   | 12/04   | 12/04   | 12/04   | 12/04   | 12/04   | 12/04   | 12/04   |
|             | Defect ID                                          |         |         |         |         |         |         |         |         |         |         |         |

---

## Diễn giải từng test case

### UTCID01 — Login thành công với tài khoản DONOR active
- **Type:** Normal | **Status:** Passed
- **Input:** email = `donor@trustfund.vn`, password = `Pa$$w0rd`, isActive = true, role = DONOR
- **Expected:** trả về `AuthResponse` chứa accessToken + refreshToken + UserInfo, log "User logged in successfully (Active: true)"
- **Test method:** `AuthServiceImplTest.login_donor_success_returnsTokens()`

### UTCID02 — Email không tồn tại trong DB
- **Type:** Abnormal | **Status:** Passed
- **Input:** email = `notfound@trustfund.vn`, password = null
- **Expected:** `userRepository.findByEmail()` trả về Optional.empty → throw `UnauthorizedException("Invalid email or password")`. KHÔNG gọi `passwordEncoder.matches()`
- **Test method:** `AuthServiceImplTest.login_emailNotFound_throwsUnauthorized()`

### UTCID03 — Sai password
- **Type:** Abnormal | **Status:** Passed
- **Input:** email = `donor@trustfund.vn`, password = `WrongPass`, user active
- **Expected:** `passwordEncoder.matches()` trả về false → throw `UnauthorizedException("Invalid email or password")`
- **Test method:** `AuthServiceImplTest.login_wrongPassword_throwsUnauthorized()`

### UTCID04 — Tài khoản bị deactivate vẫn login được
- **Type:** Normal | **Status:** Passed
- **Input:** email + password đúng, isActive = false, role = DONOR
- **Expected:** vẫn trả về token (theo comment trong code: FE sẽ handle restricted view), log "User logged in successfully (Active: false)"
- **Test method:** `AuthServiceImplTest.login_deactivatedUser_stillReturnsTokens()`

### UTCID05 — Password rỗng (null)
- **Type:** Abnormal | **Status:** Passed
- **Input:** email tồn tại, password = null
- **Expected:** `passwordEncoder.matches(null, ...)` trả về false → throw `UnauthorizedException`
- **Test method:** `AuthServiceImplTest.login_nullPassword_throwsUnauthorized()`

### UTCID06 — Email rỗng
- **Type:** Abnormal | **Status:** Passed
- **Input:** email = null/empty, password = empty
- **Expected:** `findByEmail(null)` trả về empty → throw `UnauthorizedException`
- **Test method:** `AuthServiceImplTest.login_emptyEmail_throwsUnauthorized()`

### UTCID07 — Email format không hợp lệ
- **Type:** Abnormal | **Status:** Passed
- **Input:** email = `abc@` (invalid format), password = empty
- **Expected:** Repository không tìm thấy → throw `UnauthorizedException`
- **Test method:** `AuthServiceImplTest.login_invalidEmailFormat_throwsUnauthorized()`

### UTCID08 — Password ngắn hơn 6 ký tự (boundary)
- **Type:** Boundary | **Status:** Passed
- **Input:** password length = 5
- **Expected:** Tùy validation, nếu pass validation thì throw `UnauthorizedException` do `passwordEncoder.matches()` fail
- **Test method:** `AuthServiceImplTest.login_passwordShorterThanMin_boundary()`

### UTCID09 — Login thành công với ADMIN
- **Type:** Normal | **Status:** Passed
- **Input:** email = `admin@trustfund.vn`, password đúng, role = ADMIN
- **Expected:** token chứa role = ADMIN
- **Test method:** `AuthServiceImplTest.login_admin_success()`

### UTCID10 — Login thành công với STAFF
- **Type:** Normal | **Status:** Passed
- **Input:** email = `staff@trustfund.vn`, password đúng, role = STAFF
- **Expected:** token chứa role = STAFF
- **Test method:** `AuthServiceImplTest.login_staff_success()`

### UTCID11 — Login DONOR lần thứ 2 (verify token mới)
- **Type:** Normal | **Status:** Passed
- **Input:** giống UTCID01
- **Expected:** mỗi lần login generate token mới, expiresIn được set lại
- **Test method:** `AuthServiceImplTest.login_secondTime_generatesNewToken()`

---

## Source code reference

- **File:** `identity-service/src/main/java/com/trustfund/service/implementServices/AuthServiceImpl.java`
- **Method:** `public AuthResponse login(LoginRequest request)` — line 93–121
- **Test class:** `identity-service/src/test/java/com/trustfund/service/AuthServiceImplTest.java`
