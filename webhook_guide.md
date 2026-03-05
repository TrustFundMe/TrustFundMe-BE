# Hướng dẫn Setup Webhook với Cloudflared

Để nhận được thông báo thanh toán (Webhook) từ PayOS về máy local của bạn, chúng ta cần tạo một "đường hầm" (tunnel) trung gian.

## Bước 1: Khởi tạo Tunnel với Cloudflared

Mở một terminal mới và chạy lệnh sau:

```bash
.\cloudflared-windows-amd64.exe tunnel --url http://localhost:8080
```
https://south-steven-enter-through.trycloudflare.com/api/payments/webhook

> [!NOTE]
> Giải thích: Cổng `8080` là cổng mặc định của `api-gateway` trong project này. Cloudflared sẽ nhận traffic từ internet và chuyển tiếp vào cổng này.

## Bước 2: Lấy URL Public

Sau khi chạy lệnh trên, hãy tìm trong log terminal dòng có nội dung tương tự như sau:
`+  https://something-random.trycloudflare.com`

Đó chính là địa chỉ public tới project của bạn.

## Bước 3: Cấu hình trên PayOS Dashboard

1. Truy cập vào [PayOS Dashboard](https://dashboard.payos.vn/).
2. Tìm đến phần **Cấu hình Webhook**.
3. Dán URL bạn vừa lấy được vào, thêm đuôi `/api/payments/webhook`.
   - Ví dụ: `https://something-random.trycloudflare.com/api/payments/webhook`
4. Lưu cấu hình.

## Bước 4: Test

1. Thực hiện một giao dịch quyên góp trên trang web của bạn.
2. Thanh toán thành công trên giao diện PayOS.
3. Kiểm tra log của `payment-service` để thấy webhook được nhận và xử lý.
