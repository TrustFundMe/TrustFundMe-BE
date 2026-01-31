# Chat Service

Service xử lý chat giữa Staff và Fund Owner trong hệ thống TrustFundME.

## Cấu trúc

- **Port**: 8086
- **Database**: `trustfundme_chat_db`
- **Eureka Service Name**: `chat-service`

## API Endpoints

### 1. Tạo Conversation
- **POST** `/api/conversations`
- **Mô tả**: Tạo conversation mới giữa staff và fund owner
- **Quyền**: Chỉ STAFF mới có thể tạo conversation
- **Request Body**:
```json
{
  "fundOwnerId": 3,
  "campaignId": 1  // optional
}
```

### 2. Lấy danh sách Conversations
- **GET** `/api/conversations`
- **Mô tả**: Lấy danh sách conversations của user hiện tại
- **Query Parameters**:
  - `page` (default: 0)
  - `size` (default: 10)
  - `sort` (default: "lastMessageAt,desc")

### 3. Lấy Conversation theo ID
- **GET** `/api/conversations/{id}`
- **Mô tả**: Lấy chi tiết conversation theo ID

### 4. Gửi Message
- **POST** `/api/conversations/{conversationId}/messages`
- **Mô tả**: Gửi message trong conversation
- **Request Body**:
```json
{
  "conversationId": 1,
  "content": "Xin chào, tôi cần hỗ trợ về campaign"
}
```

### 5. Lấy Messages trong Conversation
- **GET** `/api/conversations/{conversationId}/messages`
- **Mô tả**: Lấy danh sách messages trong conversation
- **Query Parameters**:
  - `page` (default: 0)
  - `size` (default: 20)
  - `sort` (default: "createdAt,desc")

### 6. Đánh dấu Messages đã đọc
- **PATCH** `/api/conversations/{conversationId}/messages/read`
- **Mô tả**: Đánh dấu tất cả messages chưa đọc trong conversation là đã đọc

## Swagger Documentation

Sau khi chạy service, truy cập Swagger UI tại:
- **URL**: `http://localhost:8086/swagger-ui.html`
- **API Docs**: `http://localhost:8086/api-docs`

## Database Schema

### Table: conversations
- `id`: BIGINT PRIMARY KEY
- `staff_id`: BIGINT NOT NULL
- `fund_owner_id`: BIGINT NOT NULL
- `campaign_id`: BIGINT NULL
- `last_message_at`: DATETIME
- `created_at`: DATETIME
- `updated_at`: DATETIME

### Table: messages
- `id`: BIGINT PRIMARY KEY
- `conversation_id`: BIGINT NOT NULL (FK to conversations)
- `sender_id`: BIGINT NOT NULL
- `content`: VARCHAR(5000) NOT NULL
- `is_read`: BOOLEAN DEFAULT FALSE
- `read_at`: DATETIME
- `created_at`: DATETIME

## Authentication

Tất cả API endpoints đều yêu cầu JWT token trong header:
```
Authorization: Bearer <token>
```

Token phải chứa:
- `sub`: User ID
- `role`: User role (STAFF, FUND_OWNER, etc.)

## Cách chạy

1. Đảm bảo MySQL đã chạy và database `trustfundme_chat_db` đã được tạo
2. Chạy service:
```bash
cd chat-service
mvn spring-boot:run
```

Hoặc sử dụng script:
```powershell
.\scripts\run-chat-service.ps1
```

## Lưu ý

- Chỉ STAFF mới có thể tạo conversation mới
- STAFF và FUND_OWNER đều có thể gửi và nhận messages trong conversation
- Messages được sắp xếp theo thời gian tạo (mới nhất trước)
- Conversation được sắp xếp theo thời gian message cuối cùng (mới nhất trước)
