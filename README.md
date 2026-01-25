# TrustFundME - Microservice Architecture

Dự án thiện nguyện minh bạch sử dụng kiến trúc Microservices với Spring Boot và Spring Cloud.

## 🏗️ Kiến Trúc

Dự án bao gồm các microservices sau:

1. **Discovery Server (Eureka)** - Port 8761
   - Service Registry để đăng ký và phát hiện các microservices

2. **API Gateway** - Port 8080
   - Cổng vào duy nhất cho tất cả API requests
   - JWT Authentication
   - Routing đến các microservices

3. **Identity Service** - Port 8081
   - Xác thực và phân quyền người dùng
   - JWT token generation
   - User management

## 🚀 Cách Chạy Dự Án

### Yêu Cầu Hệ Thống

- Java 17+
- Maven 3.6+
- Docker & Docker Compose (tùy chọn)
- MySQL 8.0+

### Chạy với Docker Compose (Khuyến nghị)

```bash
# Build và chạy tất cả services
docker-compose up --build

# Chạy ở chế độ background
docker-compose up -d

# Xem logs
docker-compose logs -f

# Dừng tất cả services
docker-compose down
```

### Chạy Thủ Công

#### 1. Khởi động MySQL

```bash
# Tạo database
mysql -u root -p
CREATE DATABASE trustfundme_identity_db;
```

### Chạy Nhanh (Windows)

```bash
.\start.bat
```

#### 2. Khởi động Discovery Server

```bash
cd discovery-server
./mvnw spring-boot:run
```

Kiểm tra: http://localhost:8761

#### 3. Khởi động API Gateway

```bash
cd api-gateway
./mvnw spring-boot:run
```

#### 4. Khởi động Identity Service

```bash
cd identity-service
./mvnw spring-boot:run
```

## 📝 API Documentation

### Swagger UI

- **Identity Service**: http://localhost:8081/swagger-ui.html
- **API Gateway**: http://localhost:8080/actuator/gateway/routes

### API Endpoints

#### Authentication (qua API Gateway)

- **POST** `/api/auth/register` - Đăng ký tài khoản mới
  ```json
  {
    "email": "user@example.com",
    "password": "password123",
    "fullName": "Nguyen Van A",
    "phoneNumber": "0123456789"
  }
  ```

- **POST** `/api/auth/login` - Đăng nhập
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```

- **POST** `/api/auth/refresh` - Làm mới token
  ```
  Header: Refresh-Token: <refresh_token>
  ```

#### Response Format

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "Nguyen Van A",
    "phoneNumber": "0123456789",
    "role": "USER"
  }
}
```

## 🔐 Authentication Flow

1. User đăng ký/đăng nhập qua Identity Service
2. Nhận được JWT access token và refresh token
3. Sử dụng access token trong header: `Authorization: Bearer <token>`
4. API Gateway validate token trước khi route đến các services
5. Khi token hết hạn, sử dụng refresh token để lấy token mới

## 📁 Cấu Trúc Thư Mục

```
TrustFundME-BE/
├── discovery-server/          # Eureka Discovery Server
├── api-gateway/               # Spring Cloud Gateway
├── identity-service/          # Authentication Service
│   ├── src/main/java/com/trustfund/
│   │   ├── config/           # Configuration
│   │   ├── controller/       # REST Controllers
│   │   ├── service/          # Business Logic
│   │   │   ├── interfaceServices/
│   │   │   └── implementServices/
│   │   ├── repository/       # Data Access
│   │   ├── model/            # Entities & DTOs
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── exception/        # Exception Handling
│   │   │   ├── exceptions/
│   │   │   └── handler/
│   │   └── utils/            # Utilities
│   └── src/main/resources/
├── docker-compose.yml        # Docker Compose config
└── README.md
```

## 🛠️ Công Nghệ Sử Dụng

- **Spring Boot 3.2.5**
- **Spring Cloud 2023.0.1**
- **Spring Cloud Gateway** - API Gateway
- **Netflix Eureka** - Service Discovery
- **Spring Security** - Security & Authentication
- **JWT (jjwt)** - Token-based Authentication
- **Spring Data JPA** - Data Access
- **MySQL** - Database
- **Swagger/OpenAPI** - API Documentation
- **Lombok** - Code Generation
- **Docker** - Containerization

## 📋 Checklist Phát Triển

- [x] Discovery Server (Eureka)
- [x] API Gateway với JWT Filter
- [x] Identity Service với Authentication
- [x] User Registration & Login
- [x] JWT Token Generation & Validation
- [x] Exception Handling
- [x] Swagger Documentation
- [x] Docker Compose Setup
- [ ] Unit Tests
- [ ] Integration Tests
- [ ] Email Service Integration
- [ ] Password Reset Functionality

## 🔍 Kiểm Tra Services

### Eureka Dashboard
http://localhost:8761

### Health Checks
- Discovery Server: http://localhost:8761/actuator/health
- API Gateway: http://localhost:8080/actuator/health
- Identity Service: http://localhost:8081/actuator/health

## 💡 Best Practices

1. **Single Responsibility**: Mỗi service chỉ làm một việc cụ thể
2. **Database per Service**: Mỗi service có database riêng
3. **API Versioning**: Sử dụng version trong API path
4. **Error Handling**: Luôn có GlobalExceptionHandler
5. **Logging**: Sử dụng structured logging
6. **Documentation**: Swagger/OpenAPI cho tất cả APIs

## 📚 Tài Liệu Tham Khảo

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Eureka Documentation](https://github.com/Netflix/eureka)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)

## 👥 Tác Giả

Dự án tốt nghiệp - TrustFundME - Hệ thống thiện nguyện minh bạch

## 📄 License

MIT License


