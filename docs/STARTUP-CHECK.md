# Kiểm tra khởi động các service

Tài liệu này mô tả cách các service khởi động và điều kiện để chạy đúng.

---

## 1. Thứ tự khởi động khuyến nghị

| Bước | Service            | Port | Ghi chú |
|------|--------------------|------|--------|
| 1    | **Discovery Server** | 8761 | Chạy trước; các service khác đăng ký Eureka tại đây. |
| 2    | **MySQL**           | 3306 | DB phải chạy trước các service dùng DB (hoặc dùng H2 cho identity khi dev). |
| 3    | **Identity Service**| 8081 | Nhiều service khác phụ thuộc (JWT, user). |
| 4    | **Campaign Service**| 8082 | Cần Eureka + MySQL + .env. |
| 5    | **Feed Service**    | 8084 | Cần Eureka + MySQL. |
| 6    | **Media Service**   | 8083 | Cần Eureka + MySQL + .env (Supabase tùy chọn). |
| 7    | **Flag Service**    | 8085 | Cần Eureka + MySQL + .env. |
| 8    | **Chat Service**    | 8086 | Cần Eureka + MySQL. |
| 9    | **API Gateway**     | 8080 | Chạy sau khi đã có ít nhất Discovery; route tới các service qua Eureka. |

---

## 2. Điều kiện từng service

### 2.1 Discovery Server (8761)

- **Cấu hình**: Không cần DB, không cần JWT.
- **Khởi động**: Chạy độc lập, không phụ thuộc service khác.
- **Lỗi thường gặp**: Port 8761 bị chiếm.

---

### 2.2 API Gateway (8080)

- **Phụ thuộc**: Eureka tại `http://localhost:8761/eureka`.
- **Env**: `JWT_SECRET` có default trong code; có thể không cần .env nếu dùng default.
- **Lỗi thường gặp**: Gateway start trước khi Discovery hoặc các service downstream chưa đăng ký → gọi route trả 503. Nên start Gateway sau khi Discovery và ít nhất một vài service đã up.

---

### 2.3 Identity Service (8081)

- **Profile mặc định (MySQL)**:
  - **Bắt buộc trong .env**: `IDENTITY_DB_URL`, `JWT_SECRET`.
  - **Tùy chọn**: `DB_USERNAME`, `DB_PASSWORD`, mail, Supabase, Google Client ID.
- **Profile H2** (dev, không cần MySQL):  
  `run-identity-service-h2.ps1` hoặc `-Dspring.profiles.active=h2` → dùng `application-h2.properties`, không cần `IDENTITY_DB_URL`.
- **Lỗi thường gặp**: Thiếu `IDENTITY_DB_URL` hoặc `JWT_SECRET` → lỗi khi start. MySQL chưa chạy hoặc chưa tạo DB → kết nối DB lỗi.

---

### 2.4 Campaign Service (8082)

- **Phụ thuộc**: Eureka, MySQL (trustfundme_campaign_db).
- **Bắt buộc trong .env**: `CAMPAIGN_DB_URL`, `JWT_SECRET` (trong code default rỗng → cần set để tránh lỗi).
- **Tùy chọn**: `DB_USERNAME`, `DB_PASSWORD`, `identity.service.url` (mặc định 8081).
- **Lưu ý**: Identity Service chỉ được gọi khi **tạo campaign** (validate fundOwnerId). Service vẫn start được nếu Identity chưa chạy; chỉ lỗi khi gọi API tạo campaign.
- **Lỗi thường gặp**: Thiếu `CAMPAIGN_DB_URL` → `datasource.url` rỗng → fail khi khởi tạo DataSource.

---

### 2.5 Feed Service (8084)

- **Phụ thuộc**: Eureka, MySQL.
- **Cấu hình**: DB URL hardcode trong `application.properties` (localhost:3306/trustfundme_feed_db). JWT có default.
- **Khởi động**: Chỉ cần MySQL có DB `trustfundme_feed_db` (hoặc `createDatabaseIfNotExist=true`). Không bắt buộc .env cho DB/JWT.
- **Lỗi thường gặp**: MySQL chưa chạy hoặc quyền user/DB sai.

---

### 2.6 Media Service (8083)

- **Phụ thuộc**: Eureka, MySQL.
- **Bắt buộc trong .env**: `MEDIA_DB_URL`, `JWT_SECRET` (default rỗng).
- **Tùy chọn**: Supabase (storage); không set thì upload/storage có thể lỗi khi gọi nhưng service vẫn start.
- **Lỗi thường gặp**: Thiếu `MEDIA_DB_URL` → fail DataSource.

---

### 2.7 Flag Service (8085)

- **Phụ thuộc**: Eureka, MySQL.
- **Bắt buộc trong .env**: `FLAG_DB_URL`, `JWT_SECRET` (trong properties không có default cho JWT_SECRET).
- **Đã bổ sung**: `FLAG_DB_URL` có trong `.env.example` (jdbc:mysql://localhost:3306/trustfundme_flag_db?createDatabaseIfNotExist=true).
- **Lỗi thường gặp**: Thiếu `FLAG_DB_URL` hoặc `JWT_SECRET` → fail khi start.

---

### 2.8 Chat Service (8086)

- **Phụ thuộc**: Eureka, MySQL.
- **Cấu hình**: DB URL hardcode (trustfundme_chat_db). JWT có default.
- **Khởi động**: Chỉ cần MySQL + DB. Không bắt buộc .env cho DB/JWT.
- **Lỗi thường gặp**: MySQL chưa chạy hoặc DB chưa tồn tại.

---

## 3. Tóm tắt biến môi trường bắt buộc (.env)

| Service   | Biến bắt buộc (profile mặc định) |
|-----------|-----------------------------------|
| Gateway   | (có default JWT trong code)       |
| Identity  | `IDENTITY_DB_URL`, `JWT_SECRET`   |
| Campaign  | `CAMPAIGN_DB_URL`, `JWT_SECRET`   |
| Feed      | (hardcode DB, JWT default)        |
| Media     | `MEDIA_DB_URL`, `JWT_SECRET`      |
| Flag      | `FLAG_DB_URL`, `JWT_SECRET`       |
| Chat      | (hardcode DB, JWT default)       |

---

## 4. Cách kiểm tra nhanh sau khi start

1. **Discovery**: Mở http://localhost:8761 → trang Eureka, xem danh sách Applications (các service đăng ký).
2. **Gateway**: Gọi qua gateway, ví dụ http://localhost:8080/api/campaigns (cần campaign-service đã đăng ký).
3. **Từng service**: Gọi trực tiếp health (nếu bật actuator), ví dụ:
   - http://localhost:8082/actuator/health (campaign)
   - http://localhost:8081/actuator/health (identity)
   - tương tự cho feed, media, flag, chat.

---

## 4.1 Kiểm tra đăng ký Eureka (tránh 503)

Nếu API qua Gateway trả **503 Service Unavailable**, thường do service chưa đăng ký Eureka (Discovery chưa chạy, hoặc service chưa start / start lỗi).

### Cách 1: Dashboard Eureka

1. Chạy **Discovery Server** trước: `.\scripts\run-discovery-server.ps1` (port **8761**).
2. Mở trình duyệt: **http://localhost:8761**
3. Trong **Application**, kiểm tra có các dòng tương ứng:
   - `FEED-SERVICE` (cho `/api/feed-posts`, `/api/forum/categories`)
   - `API-GATEWAY`
   - `IDENTITY-SERVICE`, `CAMPAIGN-SERVICE`, `MEDIA-SERVICE`, `FLAG-SERVICE`, `CHAT-SERVICE` (tùy service bạn đang chạy)

Nếu **FEED-SERVICE** không xuất hiện → feed-service chưa đăng ký. Cần:
- Bảo đảm Discovery (8761) đã chạy **trước** khi start feed-service.
- Start feed-service: `.\scripts\run-feed-service.ps1` (và MySQL có DB `trustfundme_feed_db`).

### Cách 2: REST API (PowerShell / curl)

Liệt kê tất cả ứng dụng đã đăng ký (JSON):

```powershell
Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps" -Headers @{ Accept = "application/json" }
```

Hoặc xem nhanh tên instance (ví dụ feed-service):

```powershell
(Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps" -Headers @{ Accept = "application/json" }).applications.application | ForEach-Object { $_.name }
```

Kỳ vọng thấy: `api-gateway`, `feed-service`, `identity-service`, … (chữ thường do `lower-case-service-id=true`).

---

## 5. Script chạy

- Các script trong `scripts/` (run-discovery-server.ps1, run-api-gateway.ps1, run-campaign-service.ps1, …) load `.env` từ thư mục gốc TrustFundMe-BE.
- Đảm bảo file `.env` tồn tại ở thư mục gốc và có đủ biến bắt buộc (xem mục 3).
- Nếu chạy từ thư mục gốc: `.\scripts\run-discovery-server.ps1`, rồi lần lượt các service khác theo thứ tự mục 1.
