# Hướng dẫn Setup Webhook Vĩnh Viễn bằng Vercel + Pusher

Để nhận được thông báo thanh toán (Webhook) từ PayOS về máy local mà **không cần chạy Cloudflare Tunnel** mỗi lần code, chúng ta sử dụng kiến trúc Forwarder:
PayOS -> Vercel (Luôn online) -> Pusher (WebSocket) -> Local Spring Boot.

## Bước 1: Khởi tạo và Lấy Credentials Pusher

1. Tạo tài khoản miễn phí tại **[Pusher](https://pusher.com/)**.
2. Tạo một **Channels app** mới.
3. Lấy 4 thông số: `app_id`, `key`, `secret`, `cluster` và điền vào file `.env` ở backend:
   ```ini
   PUSHER_APP_ID=xxx
   PUSHER_KEY=xxx
   PUSHER_SECRET=xxx
   PUSHER_CLUSTER=ap1
   ```

## Bước 2: Deploy Forwarder lên Vercel (Chỉ làm 1 lần duy nhất)

Mở terminal mới, di chuyển vào thư mục `TrustFundMe-BE/webhook-forwarder`:

```bash
cd TrustFundMe-BE/webhook-forwarder
npx vercel login
```
Đăng nhập Vercel trên trình duyệt. Sau đó chạy lệnh deploy:

```bash
npx vercel --prod
```
Làm theo hướng dẫn trên màn hình (Enter liên tục để dùng mặc định). Khi hoàn tất, Vercel sẽ cấp cho bạn một URL, ví dụ: `https://payos-webhook-forwarder.vercel.app`.

Tiếp theo, vào Dashboard của project Vercel mới tạo -> **Settings** -> **Environment Variables**, thêm 4 biến môi trường của Pusher vào:
- `PUSHER_APP_ID`
- `PUSHER_KEY`
- `PUSHER_SECRET`
- `PUSHER_CLUSTER`

Sau khi thêm xong, Vercel sẽ tự động redeploy để nhận biến mới.

## Bước 3: Cấu hình Webhook trên PayOS

1. Truy cập vào [PayOS Dashboard](https://dashboard.payos.vn/).
2. Tìm đến phần **Cấu hình Webhook**.
3. Dán URL Vercel bạn vừa nhận được, thêm đuôi `/api/webhook`.
   - Ví dụ: `https://payos-webhook-forwarder.vercel.app/api/webhook`
4. Lưu cấu hình. **URL này sẽ vĩnh viễn không thay đổi.**

## Bước 4: Test thử

1. Chạy Spring Boot `payment-service`. Bạn sẽ thấy log báo: `Pusher Webhook Listener initialized and subscribed to 'payos-webhook' channel`.
2. Lên web tạo 1 giao dịch Quyên góp và thanh toán thử.
3. Khi thanh toán xong, PayOS sẽ bắn webhook lên Vercel -> Vercel đẩy xuống Pusher -> Pusher đẩy về backend local của bạn ngay lập tức.
4. Kiểm tra log của `payment-service` để thấy webhook được xử lý.
