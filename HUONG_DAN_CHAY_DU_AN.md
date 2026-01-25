# Hướng Dẫn Chạy Dự Án TrustFundME

## 📋 Yêu Cầu Hệ Thống

- **Java**: JDK 17 hoặc cao hơn
- **Maven**: Đã cài đặt và cấu hình trong PATH
- **MySQL**: Đã cài đặt và đang chạy
- **PowerShell**: Windows PowerShell hoặc PowerShell Core

## 🗄️ Chuẩn Bị Database

### Bước 1: Kiểm tra MySQL đang chạy
```powershell
Get-Service -Name "*mysql*"
```

Nếu MySQL chưa chạy, khởi động:
```powershell
Start-Service MySQL80
# Hoặc tên service MySQL của bạn
```

### Bước 2: Tạo Database (Tùy chọn)
Mở MySQL Workbench và chạy:
```sql
CREATE DATABASE IF NOT EXISTS trustfundme_identity_db;
CREATE DATABASE IF NOT EXISTS trustfundme_campaign_db;
```

**Lưu ý:** Database sẽ tự động tạo nếu chưa có (vì có `createDatabaseIfNotExist=true`)

### Bước 3: Kiểm tra cấu hình
Mở file `identity-service/src/main/resources/application.properties` và đảm bảo:
- `spring.datasource.username=root`
- `spring.datasource.password=12345` (hoặc password MySQL của bạn)

### Bước 4: Cấu hình Google Login (Mới)
Mở file `.env` ở thư mục gốc và cập nhật:
```properties
GOOGLE_CLIENT_ID=YOUR_REAL_GOOGLE_CLIENT_ID
```

## 🚀 Cách Chạy Dự Án

### Cách 1: Chạy Tự Động (Khuyến Nghị) ⭐

**Bước 1:** Mở PowerShell ở thư mục dự án

**Bước 2:** Cho phép chạy script (chỉ cần làm 1 lần):
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

**Bước 3:** Chạy tất cả services bằng script tiện lợi:
```powershell
.\start.bat
```
(Hoặc chạy file script gốc: `.\scripts\run-all-services.ps1`)

Script sẽ tự động mở 6 cửa sổ PowerShell, mỗi cửa sổ chạy 1 service.

**Thứ tự chạy:**
1. Discovery Server (port 8761)
2. API Gateway (port 8080)
3. Identity Service (port 8081)
4. Campaign Service (port 8082)
5. Media Service (port 8083)
6. Feed Service (port 8084)

---

### Cách 2: Chạy Thủ Công Từng Service

Mở **4 cửa sổ PowerShell riêng biệt**:

#### Terminal 1 - Discovery Server
```powershell
cd "D:\HOC\Ki 9\TrustFundME- BE\discovery-server"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
mvn spring-boot:run
```

**Đợi đến khi thấy:** `Started DiscoveryServerApplication`

#### Terminal 2 - API Gateway
```powershell
cd "D:\HOC\Ki 9\TrustFundME- BE\api-gateway"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
mvn spring-boot:run
```

**Đợi đến khi thấy:** `Started ApiGatewayApplication`

#### Terminal 3 - Identity Service
```powershell
cd "D:\HOC\Ki 9\TrustFundME- BE\identity-service"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
mvn spring-boot:run
```

**Đợi đến khi thấy:** `Started IdentityServiceApplication`

#### Terminal 4 - Campaign Service
```powershell
cd "D:\HOC\Ki 9\TrustFundME- BE\campaign-service"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
mvn spring-boot:run
```

**Đợi đến khi thấy:** `Started CampaignServiceApplication`

---

## ✅ Kiểm Tra Services Đã Chạy

### 1. Discovery Server (Eureka Dashboard)
- **URL:** http://localhost:8761
- **Kiểm tra:** Xem các services đã đăng ký

### 2. API Gateway
- **Health Check:** http://localhost:8080/actuator/health
- **Kiểm tra:** Response `{"status":"UP"}`

### 3. Identity Service
- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **API Docs:** http://localhost:8081/api-docs

### 4. Campaign Service
- **Swagger UI:** http://localhost:8082/swagger-ui.html
- **API Docs:** http://localhost:8082/api-docs

### 4. Media Service
- **Swagger UI:** http://localhost:8083/swagger-ui.html
- **API Docs:** http://localhost:8083/api-docs

### 5. Feed Service
- **Swagger UI:** http://localhost:8084/swagger-ui.html
- **API Docs:** http://localhost:8084/api-docs

---

## 🧪 Test API

### Qua Swagger UI (Dễ nhất)
1. Mở: http://localhost:8081/swagger-ui.html
2. Tìm endpoint `/api/auth/register`
3. Click "Try it out"
4. Paste JSON:
```json
{
  "email": "test@example.com",
  "password": "password123",
  "fullName": "Test User",
  "phoneNumber": "0123456789"
}
```
5. Click "Execute"

### Campaign Service (port 8082)
1. Mở: http://localhost:8082/swagger-ui.html
2. Thử `GET /api/campaigns` (danh sách campaign), `GET /api/campaigns/{id}`, `GET /api/campaigns/fund-owner/{fundOwnerId}`

### Qua API Gateway
```powershell
# Đăng ký
curl -X POST http://localhost:8080/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{\"email\":\"test@example.com\",\"password\":\"password123\",\"fullName\":\"Test User\",\"phoneNumber\":\"0123456789\"}'

# Đăng nhập
curl -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"email\":\"test@example.com\",\"password\":\"password123\"}'
```

---

## 🛑 Dừng Tất Cả Services

### Cách 1: Dùng Script
```powershell
.\scripts\stop-all-services.ps1
```

### Cách 2: Thủ Công
Trong mỗi terminal, nhấn `Ctrl + C` để dừng service

### Cách 3: Dừng tất cả Java processes
```powershell
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force
```

---

## 🔧 Xử Lý Lỗi Thường Gặp

### Lỗi: "Execution Policy"
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Lỗi: "MySQL connection failed"
- Kiểm tra MySQL đang chạy: `Get-Service -Name "*mysql*"`
- Kiểm tra password trong `application.properties`
- Kiểm tra database đã tạo chưa

### Lỗi: "Port already in use"
- Dừng service đang dùng port đó
- Hoặc đổi port trong `application.properties`

### Lỗi: "Main class not found"
- Chạy: `mvn clean compile`
- Sau đó chạy lại: `mvn spring-boot:run`

### Lỗi: Maven JWT Dependencies (jjwt-api, jjwt-impl, jjwt-jackson)
Nếu bạn gặp lỗi về JWT dependencies khi pull code từ GitHub:

**Cách 1: Dùng script tự động (Khuyến nghị)**
```powershell
.\scripts\fix-maven-jwt.ps1
```

**Cách 2: Thủ công**
```powershell
# 1. Clean tất cả projects
cd identity-service
mvn clean
cd ..\campaign-service
mvn clean
cd ..\api-gateway
mvn clean
cd ..\media-service
mvn clean
cd ..

# 2. Xóa JWT dependencies từ local repository
Remove-Item -Path "$env:USERPROFILE\.m2\repository\io\jsonwebtoken" -Recurse -Force

# 3. Re-download dependencies
cd identity-service
mvn dependency:resolve
cd ..\campaign-service
mvn dependency:resolve
cd ..\api-gateway
mvn dependency:resolve
cd ..\media-service
mvn dependency:resolve
cd ..
```

**Nguyên nhân thường gặp:**
- Maven local repository bị corrupt
- Java version khác nhau (cần Java 17+)
- Maven version cũ
- Cache của Maven

**Kiểm tra:**
```powershell
# Kiểm tra Java version (cần 17+)
java -version

# Kiểm tra Maven version
mvn -version
```

---

## 📝 Tóm Tắt Nhanh

1. ✅ Đảm bảo MySQL đang chạy
2. ✅ Kiểm tra password MySQL trong `application.properties`
3. ✅ Chạy: `.\start.bat`
4. ✅ Đợi tất cả services khởi động xong
5. ✅ Test API qua Swagger: http://localhost:8081/swagger-ui.html (Identity) hoặc http://localhost:8082/swagger-ui.html (Campaign)

---

## 🎯 Các URL Quan Trọng

- **Eureka Dashboard:** http://localhost:8761
- **API Gateway Health:** http://localhost:8080/actuator/health
- **Identity Service Swagger:** http://localhost:8081/swagger-ui.html
- **Campaign Service Swagger:** http://localhost:8082/swagger-ui.html
- **API Endpoint:** http://localhost:8080/api/auth/*

---

Chúc bạn code vui vẻ! 🚀
