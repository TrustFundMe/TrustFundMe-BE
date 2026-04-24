# Function1-Login account Staff

| Function Code | Function1-Login account Staff | Function Name | | | Login account Staff | | |
|---------------|-------------------------------|---------------|---|---|---------------------|---|---|
| **Created By** | DatTDQ | **Executed By** | | | DatTDQ | | |
| **Lines of code** | 100 | **Lack of test cases** | | | 5 | | |
| **Test requirement** | <Brief description about requirements which are tested in this function> | | | | | | |

| Passed | Failed | Untested | N | A | B | Total Test Cases |
|--------|--------|----------|---|---|---|------------------|
| 5      | 0      | 0        | 5 | 0 | 0 | 5                |

---

## Test Case Matrix

|         |                                                                                  |         | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 |
|---------|----------------------------------------------------------------------------------|---------|---------|---------|---------|---------|---------|
| **Condition** | **Precondition**                                                            |         |         |         |         |         |         |
|         | Can connect with server                                                          |         | O       | O       | O       | O       | O       |
|         | Role: Staff                                                                      |         | O       | O       | O       | O       | O       |
|         |                                                                                  |         |         |         |         |         |         |
|         | **Username**                                                                     |         |         |         |         |         |         |
|         | staff_un_01                                                                      |         | O       | O       |         | O       | O       |
|         | Empty                                                                            |         |         |         | O       |         |         |
|         |                                                                                  |         |         |         |         |         |         |
|         | **Password**                                                                     |         |         |         |         |         |         |
|         | staff_pw_01                                                                      |         | O       |         | O       | O       |         |
|         | Empty                                                                            |         |         | O       |         |         |         |
|         | wrong_pw                                                                         |         |         |         |         |         | O       |
|         | **Account status**                                                               |         |         |         |         |         |         |
|         | Active                                                                           |         | O       | O       | O       |         | O       |
|         | Inactive                                                                         |         |         |         |         | O       |         |
| **Confirm** | **Return**                                                                   |         |         |         |         |         |         |
|         | **Status Code**                                                                  |         |         |         |         |         |         |
|         | status = 200                                                                     |         | O       |         |         |         |         |
|         | status = 400                                                                     |         |         | O       | O       |         | O       |
|         | status = 403                                                                     |         |         |         |         | O       |         |
|         | **Data**                                                                         |         |         |         |         |         |         |
|         | token                                                                            |         | O       |         |         |         |         |
|         | Empty                                                                            |         |         | O       | O       | O       | O       |
|         | **Exception**                                                                    |         |         |         |         |         |         |
|         | BadRequestException                                                              |         |         | O       | O       |         |         |
|         | AuthenException                                                                  |         |         |         |         |         |         |
|         | **Log message**                                                                  |         |         |         |         |         |         |
|         | Message = "Đăng nhập thành công!"                                                |         | O       |         |         |         |         |
|         | Message = "Xác thực thất bại: {password=không được để trống}"                    |         |         | O       |         |         |         |
|         | Message = "Xác thực thất bại: {username=không được để trống}"                    |         |         |         | O       |         |         |
|         | Message = "Tài khoản của bạn đã bị dừng hoạt động, vui lòng liên hệ ADMIN"        |         |         |         |         | O       |         |
|         | Message = "Sai ID hoặc mật khẩu!"                                                |         |         |         |         |         | O       |
|         | Message = "Đăng nhập thất bại!"                                                  |         |         | O       |         |         |         |
| **Result** | Type (N: Normal, A: Abnormal, B: Boundary)                                    |         | N       | N       | N       | N       | N       |
|         | Passed/Failed                                                                    |         | P       | P       | P       | P       | P       |
|         | Executed Date                                                                    |         | 08/21   | 08/21   | 08/21   | 08/21   | 08/21   |
|         | Defect ID                                                                        |         |         |         |         |         |         |

---

## Diễn giải các test case

### UTCID01 — Đăng nhập thành công
- **Type:** Normal | **Status:** Passed
- **Input:** username = `staff_un_01`, password = `staff_pw_01`, account status = Active
- **Expected:** status = 200, trả về token, log "Đăng nhập thành công!"

### UTCID02 — Password trống
- **Type:** Normal | **Status:** Passed
- **Input:** username = `staff_un_01`, password = Empty, account status = Active
- **Expected:** status = 400, BadRequestException, log "Xác thực thất bại: {password=không được để trống}" + "Đăng nhập thất bại!"

### UTCID03 — Username trống
- **Type:** Normal | **Status:** Passed
- **Input:** username = Empty, password = `staff_pw_01`, account status = Active
- **Expected:** status = 400, BadRequestException, log "Xác thực thất bại: {username=không được để trống}"

### UTCID04 — Tài khoản bị khóa
- **Type:** Normal | **Status:** Passed
- **Input:** username = `staff_un_01`, password = `staff_pw_01`, account status = Inactive
- **Expected:** status = 403, log "Tài khoản của bạn đã bị dừng hoạt động, vui lòng liên hệ ADMIN"

### UTCID05 — Sai mật khẩu
- **Type:** Normal | **Status:** Passed
- **Input:** username = `staff_un_01`, password = `wrong_pw`, account status = Active
- **Expected:** status = 400, log "Sai ID hoặc mật khẩu!"
