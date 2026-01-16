# 🚀 Hướng Dẫn Chạy Services

## ⚠️ Lưu Ý

Các services đang được build ở background. Lần đầu chạy sẽ mất 5-10 phút để download dependencies.

## 📋 Cách Chạy Services (Khuyến nghị)

### Mở 3 PowerShell Windows riêng biệt:

### Terminal 1 - Discovery Server:
```powershell
cd "D:\HOC\Ki 9\TrustFundME- BE\discovery-server"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
mvn spring-boot:run
```

### Terminal 2 - API Gateway (Đợi Terminal 1 chạy xong):
```powershell
cd "D:\HOC\Ki 9\TrustFundME- BE\api-gateway"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
mvn spring-boot:run
```

### Terminal 3 - Identity Service (Đợi Terminal 1 và 2 chạy xong):
```powershell
cd "D:\HOC\Ki 9\TrustFundME- BE\identity-service"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
mvn spring-boot:run
```

## ✅ Kiểm Tra Services Đã Chạy

Sau khi các services khởi động (khoảng 1-2 phút), kiểm tra:

1. **Eureka Dashboard**: http://localhost:8761
   - Nếu thấy dashboard = Discovery Server đã chạy ✅
   - Nếu thấy `api-gateway` và `identity-service` trong danh sách = Tất cả đã chạy ✅

2. **API Gateway**: http://localhost:8080/actuator/health

3. **Identity Service Swagger**: http://localhost:8081/swagger-ui.html

## 🐛 Nếu Gặp Lỗi

### Lỗi MySQL Connection:
- Cài MySQL: `choco install mysql -y`
- Hoặc tạo database: `CREATE DATABASE trustfundme_identity_db;`
- Sửa password trong `identity-service/src/main/resources/application.properties`

### Lỗi Port đã được sử dụng:
```powershell
# Tìm process đang dùng port
netstat -ano | findstr :8080

# Kill process (thay <PID> bằng số từ lệnh trên)
taskkill /PID <PID> /F
```

### Lỗi Maven không tìm thấy:
```powershell
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
```

## 🎯 Test API Sau Khi Chạy

### 1. Đăng ký:
```powershell
curl -X POST http://localhost:8080/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{\"email\":\"test@example.com\",\"password\":\"password123\",\"fullName\":\"Test User\"}'
```

### 2. Đăng nhập:
```powershell
curl -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"email\":\"test@example.com\",\"password\":\"password123\"}'
```


