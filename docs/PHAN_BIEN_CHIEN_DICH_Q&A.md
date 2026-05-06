# TrustFundMe — PHẢN BIỆN BẢO VỆ ĐỒ ÁN: PHẦN CHIẾN DỊCH (CAMPAIGN)

## Tài liệu chuẩn bị các câu hỏi ngặt nghèo nhất & phương án trả lời chi tiết

> **Phạm vi:** Tập trung vào toàn bộ vòng đời chiến dịch — từ tạo, duyệt, nhận donate, giải ngân, minh chứng, cho đến đóng/hủy.
> Mỗi tình huống đều có: Câu hỏi → Phân tích vấn đề → Cơ chế xử lý hiện tại (code evidence) → Hạn chế → Hướng cải thiện.
> **Cập nhật:** 2026-05-06

## TỔNG QUAN TECH STACK LIÊN QUAN ĐẾN CHIẾN DỊCH

| Công nghệ | Vai trò trong Campaign |
|-----------|----------------------|
| **Spring Boot 3.x + JPA/Hibernate** | ORM mapping `Campaign`, `Expenditure`, `ExpenditureItem` entity → MySQL. `@Transactional` đảm bảo ACID cho local transactions. |
| **Spring Cloud Gateway (WebFlux)** | Reactive gateway định tuyến `/api/campaigns/**` → campaign-service, `/api/payments/**` → payment-service. JWT filter inject `X-User-Id`, `X-User-Role`. |
| **Netflix Eureka** | Service Discovery — campaign-service đăng ký với Eureka, gateway resolve `lb://campaign-service` thay vì hardcode IP:port. |
| **VietQR + Casso** | VietQR tạo QR code chuyển khoản chuẩn quốc gia. Casso (bank aggregator) giám sát tài khoản ngân hàng 24/7, gửi webhook khi có giao dịch vào/ra. |
| **Pusher Channels** | Real-time WebSocket — Casso → Vercel webhook-forwarder → Pusher → `PusherCassoListener.java` trong payment-service. |
| **Perplexity AI** | LLM API phân tích giá thị trường — campaign-service gọi `PerplexityClient` gửi danh sách items, nhận về market analysis report. |
| **MySQL 8.0 (DECIMAL 19,4)** | Lưu trữ tài chính dùng `BigDecimal` Java → `DECIMAL(19,4)` MySQL — tránh lỗi floating point. |
| **Supabase Storage** | Object storage cho ảnh cover chiến dịch, minh chứng chi tiêu, KYC. Media Service upload → trả về URL. |
| **Scheduled Tasks (Spring @Scheduled)** | `ExpenditureEnforcementScheduler` (mỗi phút check evidence quá hạn), `PaymentCleanupTask` (mỗi phút cleanup PENDING donations). |
| **Next.js 14 App Router** | FE dùng Server Components + Client Components. API Routes làm proxy backend. `DonationExceedWarningModal` (client component) xử lý overfund warning. |
| **face-api.js** | Thư viện face recognition chạy trên browser — so sánh ảnh selfie với ảnh CCCD trong flow KYC. 128-dim face descriptor vector. |
| **Lombok (@Builder, @Data)** | Code generation — tất cả entity dùng `@Builder` pattern, `@Data` generate getter/setter/toString. `@RequiredArgsConstructor` cho DI. |

---

# MỤC LỤC

1. [NGÂN SÁCH BIẾN ĐỘNG (Tiền thấp / Tiền cao)](#1-ngân-sách-biến-động)
2. [CHIẾN DỊCH LỖ NẶNG / LỢI NHUẬN QUÁ THẤP](#2-chiến-dịch-lỗ-nặng--lợi-nhuận-quá-thấp)
3. [NGÂN SÁCH THỪA / KHÔNG ĐỦ CHI TRẢ](#3-ngân-sách-thừa--không-đủ-chi-trả)
4. [DONOR DONATE VƯỢT MỤC TIÊU (Overfunding)](#4-donor-donate-vượt-mục-tiêu-overfunding)
5. [CHIẾN DỊCH KHÔNG AI DONATE (Underfunding)](#5-chiến-dịch-không-ai-donate-underfunding)
6. [FUND OWNER RÚT TIỀN NHƯNG KHÔNG MUA HÀNG](#6-fund-owner-rút-tiền-nhưng-không-mua-hàng)
7. [GIÁ THỊ TRƯỜNG BIẾN ĐỘNG SAU KHI LẬP KẾ HOẠCH](#7-giá-thị-trường-biến-động-sau-khi-lập-kế-hoạch)
8. [FUND OWNER KHAI GIÁ CAO ĐỂ CẮT TIỀN](#8-fund-owner-khai-giá-cao-để-cắt-tiền)
9. [2 NGƯỜI DONATE CÙNG LÚC → RACE CONDITION](#9-2-người-donate-cùng-lúc--race-condition)
10. [DONOR CHUYỂN TIỀN SAI NỘI DUNG](#10-donor-chuyển-tiền-sai-nội-dung)
11. [CASSO WEBHOOK KHÔNG HOẠT ĐỘNG](#11-casso-webhook-không-hoạt-động)
12. [ĐÓNG CHIẾN DỊCH — SỐ DƯ XỬ LÝ NHƯ THẾ NÀO](#12-đóng-chiến-dịch--số-dư-xử-lý-như-thế-nào)
13. [CHIẾN DỊCH BỊ FLAG NHIỀU LẦN](#13-chiến-dịch-bị-flag-nhiều-lần)
14. [PAYMENT SERVICE CHẾT GIỮA GIAO DỊCH](#14-payment-service-chết-giữa-giao-dịch)
15. [DISTRIBUTED TRANSACTION — BALANCE KHÔNG ĐỒNG BỘ](#15-distributed-transaction--balance-không-đồng-bộ)
16. [BALANCE ÂM — CÓ THỂ XẢY RA KHÔNG?](#16-balance-âm--có-thể-xảy-ra-không)
17. [GIAO DỊCH CHI ÂM TỪ TÀI KHOẢN CHIẾN DỊCH](#17-giao-dịch-chi-âm-từ-tài-khoản-chiến-dịch)
18. [ĐIỀU KHOẢN PHÁP LÝ — NỀN TẢNG MIỄN TRỪ TRÁCH NHIỆM NHƯ THẾ NÀO](#18-điều-khoản-pháp-lý--nền-tảng-miễn-trừ-trách-nhiệm)
19. [CHIẾN DỊCH ITEMIZED VS AUTHORIZED — KHI NÀO DÙNG CÁI NÀO](#19-chiến-dịch-itemized-vs-authorized--khi-nào-dùng)
20. [COMMITMENT (CAM KẾT) CÓ GIÁ TRỊ PHÁP LÝ KHÔNG](#20-commitment-cam-kết-có-giá-trị-pháp-lý-không)
21. [TẠI SAO TIỀN VÀO THẲNG TÀI KHOẢN CHỦ QUỸ MÀ KHÔNG QUA NỀN TẢNG](#21-tại-sao-tiền-vào-thẳng-tài-khoản-chủ-quỹ)
22. [NẾU NHÀ TÀI TRỢ YÊU CẦU HOÀN TIỀN](#22-nếu-nhà-tài-trợ-yêu-cầu-hoàn-tiền)
23. [TRUST SCORE BỊ THAO TÚNG](#23-trust-score-bị-thao-túng)
24. [GENERAL FUND — AI KIỂM SOÁT QUỸ CHUNG](#24-general-fund--ai-kiểm-soát-quỹ-chung)
25. [TẠI SAO CẦN KYC TRƯỚC KHI TẠO CHIẾN DỊCH](#25-tại-sao-cần-kyc-trước-khi-tạo-chiến-dịch)

---

# 1. NGÂN SÁCH BIẾN ĐỘNG

## Q: Nếu ngân sách quá thấp (VD: chiến dịch chỉ nhận được 100.000đ trên mục tiêu 10 triệu), hoặc ngân sách quá cao (vượt mục tiêu gấp 3 lần), hệ thống xử lý như thế nào?

### Trả lời:

**Trường hợp ngân sách QUÁ THẤP (underfunding):**

Hệ thống **không bắt buộc** chiến dịch phải đạt 100% mục tiêu mới giải ngân. Cơ chế xử lý:

1. **Tiến độ theo phần trăm:** [`DonationService.getCampaignProgress()`](payment-service/src/main/java/com/trustfund/service/DonationService.java:369) tính `progressPercentage = raised / goal × 100`, cap tại 100%. Frontend hiển thị thanh tiến độ.

2. **Giải ngân theo đợt (Milestone-based):** Hệ thống giải ngân theo kế hoạch đã lập, không phải theo tổng mục tiêu. Nếu đợt 1 cần 2 triệu mà chỉ nhận được 1 triệu → Fund Owner phải **tái cấu trúc kế hoạch** (giảm số lượng vật phẩm, thay đổi nhà cung cấp).

3. **Chính sách trong điều khoản:** Theo [`mockData.ts`](../danbox/src/components/campaign/new-campaign-test/mockData.ts:16): *"Nếu thiếu hụt ngân sách cuối kỳ, chủ quỹ phải nộp kế hoạch tái cấu trúc milestone để Staff duyệt."*

4. **Staff quyết định:** Staff review từng đợt giải ngân. Nếu số tiền không đủ, staff có thể:
   - Yêu cầu Fund Owner sửa kế hoạch chi tiêu
   - Tạm dừng chiến dịch (`PAUSED`) để chờ thêm donate
   - Hỗ trợ từ General Fund

**Trường hợp ngân sách QUÁ CAO (overfunding):**

1. **FE cảnh báo trước khi donate:** Component [`DonationExceedWarningModal`](../danbox/src/components/donation/DonationExceedWarningModal.tsx:19) hiển thị popup khi `donationAmount > remaining`:
   - Thông báo: *"Đợt này chỉ cần thêm X VNĐ nữa là hoàn thành"*
   - Hiển thị: phần vào đợt hiện tại vs. phần giữ cho đợt tiếp theo
   - Donor có thể "Tiếp tục quyên góp" hoặc "Điều chỉnh số tiền"

2. **Tiền vượt vào quỹ tổng:** Theo [`mockData.ts`](../danbox/src/components/campaign/new-campaign-test/mockData.ts:14): *"Quỹ tổng là ví duy nhất của chiến dịch; hạng mục là dự toán, không phải ví riêng."* → Tất cả tiền vào `Campaign.balance`, các đợt chi tiêu là dự toán.

3. **Không giới hạn nhận quyên góp:** Theo [`mockData.ts`](../danbox/src/components/campaign/new-campaign-test/mockData.ts:15): *"Chiến dịch có thể nhận vượt mục tiêu trong phạm vi mục đích thiện nguyện đã công bố."*

4. **Tiền dư không được rút về cá nhân:** [`mockData.ts`](../danbox/src/components/campaign/new-campaign-test/mockData.ts:17): *"Tiền dư cuối kỳ không được rút về tài khoản cá nhân; xử lý theo chính sách minh bạch đã cam kết."*

5. **Khi đóng chiến dịch, số dư thu hồi:** [`CampaignServiceImpl.closeCampaign()`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:355) set `status = "CLOSED"` → toàn bộ balance còn lại chuyển về General Fund.

**🔧 Chi tiết kỹ thuật (Tech Deep-dive):**

- **Database schema:** `campaigns.balance` là `DECIMAL(19,4)` — precision 19 cho phép lưu đến hàng triệu tỷ, scale 4 cho 4 chữ số thập phân. Tất cả phép tính tiền dùng `java.math.BigDecimal` tránh lỗi floating-point.
- **API flow donate vượt mục tiêu:** FE gọi `GET /api/payments/donations/progress/{campaignId}` → nhận `raisedAmount`, `goalAmount` → tính `remaining` → so sánh với `donationAmount` → nếu vượt → hiện `DonationExceedWarningModal` → donor xác nhận → `POST /api/payments/create` (KHÔNG giới hạn amount).
- **FundraisingGoal:** [`FundraisingGoal.targetAmount`](campaign-service/src/main/java/com/trustfund/model/FundraisingGoal.java:27) là mục tiêu tham chiếu, `isActive = true` cho goal hiện tại. Có thể tạo nhiều goal (VD: tăng mục tiêu giữa chừng).
- **Progress calculation:** [`DonationService.getCampaignProgress()`](payment-service/src/main/java/com/trustfund/service/DonationService.java:369) dùng `sumDonationAmountByCampaignId()` (JPQL aggregate) + fetch active goal từ campaign-service → tính `pct = raised/goal × 100`, cap tại 100%, floor tại 1% nếu `raised > 0`.

---

# 2. CHIẾN DỊCH LỖ NẶNG / LỢI NHUẬN QUÁ THẤP

## Q: Nếu chiến dịch chi tiêu nhiều hơn số tiền quyên góp được (lỗ), hoặc số tiền quyên góp quá ít so với kế hoạch, hệ thống xử lý ra sao?

### Trả lời:

**Quan trọng:** TrustFundMe là nền tảng **thiện nguyện**, KHÔNG phải đầu tư. Không có khái niệm "lợi nhuận" hay "lỗ" theo nghĩa kinh doanh. Tuy nhiên, **chi vượt mức (overspending)** là vấn đề thực tế:

**1. Giải ngân theo kế hoạch, KHÔNG cho rút tùy ý:**
- Fund Owner phải lập [`Expenditure`](campaign-service/src/main/java/com/trustfund/model/Expenditure.java:23) với `totalExpectedAmount`.
- Staff duyệt kế hoạch trước khi giải ngân.
- Hệ thống kiểm tra `campaign.balance >= expenditure.totalAmount` trước khi cho phép rút.

**2. Variance tracking (chênh lệch thực chi vs. dự kiến):**
- [`Expenditure.variance`](campaign-service/src/main/java/com/trustfund/model/Expenditure.java:56) = `totalReceivedAmount - totalAmount`
- Variance > 0: còn dư → phải hoàn lại qua `createRefund()`
- Variance < 0: chi vượt → cần giải trình

**3. Không thể chi vượt balance:**
- [`CampaignServiceImpl.updateBalance()`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:365): `balance = oldBalance + amount`. Khi chi, amount là số âm.
- Hệ thống **KHÔNG kiểm tra balance >= 0** ở tầng code → **đây là điểm yếu cần cải thiện**. Nên thêm check `if (newBalance < 0) throw exception`.

**4. Refund khi chi ít hơn dự kiến:**
- Fund Owner tạo refund với `ExpenditureTransaction.type = "REFUND"` → tiền cộng lại vào balance.

**5. Nếu chiến dịch thực sự "lỗ" (không đủ tiền):**
- General Fund có thể **hỗ trợ** bằng [`InternalTransaction`](campaign-service/src/main/java/com/trustfund/model/InternalTransaction.java:21) với `type = TRANSFER` hoặc `SUPPORT`.
- Staff/Admin quyết định chuyển tiền từ quỹ chung sang chiến dịch cần hỗ trợ.

**🔧 Chi tiết kỹ thuật (Tech Deep-dive):**

- **Expenditure review flow (chi tiết backend):**
  1. Fund Owner tạo expenditure: `POST /api/expenditures` → lưu `Expenditure` với `status = "PENDING_REVIEW"`, kèm list `ExpenditureCatology` → mỗi catology có list `ExpenditureItem`.
  2. Staff duyệt: `PUT /api/expenditures/{id}/status` → `status = "APPROVED"`. Nếu campaign type = AUTHORIZED → tự động set `WITHDRAWAL_REQUESTED`.
  3. Fund Owner yêu cầu rút: `PUT /api/expenditures/{id}/withdraw` → `isWithdrawalRequested = true`.
  4. Staff giải ngân: Upload proof (`proofUrl`), set `status = "DISBURSED"`, trừ `campaign.balance`.
  5. Fund Owner mua hàng, cập nhật actual: `PUT /api/expenditures/{id}/actuals` → `actualPrice`, `actualQuantity` cho từng item.
  6. Fund Owner nộp minh chứng: Upload hóa đơn → `ExpenditureEvidence.status = "SUBMITTED"`.
- **@Transactional scope:** `ExpenditureServiceImpl` dùng `@Transactional` cho mỗi operation → đảm bảo rollback nếu lỗi giữa chừng. Nhưng cross-service call (notification, trust score) nằm trong try-catch ngoài transaction boundary → không rollback main flow.
- **Variance formula:** `variance = totalReceivedAmount - totalAmount`. `totalAmount` = tổng `expectedPrice × expectedQuantity` của tất cả items. `totalReceivedAmount` = số tiền thực nhận từ giải ngân. Nếu Fund Owner chi 8 triệu trên 10 triệu nhận → variance = +2 triệu → phải refund.

---

# 3. NGÂN SÁCH THỪA / KHÔNG ĐỦ CHI TRẢ

## Q: Nếu ngân sách bị thừa (chi ít hơn dự kiến) hoặc không đủ chi trả (chiến dịch hết tiền giữa chừng), phương án giải quyết là gì?

### Trả lời:

**Trường hợp NGÂN SÁCH THỪA:**

1. **Refund bắt buộc:** Khi Fund Owner chi ít hơn số tiền đã nhận cho đợt giải ngân, hệ thống yêu cầu hoàn lại:
   - [`ExpenditureTransaction`](campaign-service/src/main/java/com/trustfund/model/ExpenditureTransaction.java:18) với `type = "REFUND"`, `status = "COMPLETED"`
   - Số tiền hoàn được cộng lại vào `campaign.balance`

2. **Tiền dư cuối chiến dịch → General Fund:**
   - Khi đóng chiến dịch ([`closeCampaign()`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:355)), toàn bộ `balance > 0` được tạo giao dịch `RECOVERY` chuyển về General Fund (ID=1).
   - **Không có tiền bị mất**, không có tiền bị Fund Owner rút về cá nhân.

3. **Điều khoản pháp lý rõ ràng:** Điều 3.2 trong [`fullRiskTermsVietnamese`](../danbox/src/components/campaign/new-campaign-test/mockData.ts:67): *"Người tạo chiến dịch cam kết sử dụng toàn bộ số tiền quyên góp được đúng mục đích đã công bố. Nghiêm cấm sử dụng kinh phí cho mục đích cá nhân."*

**Trường hợp KHÔNG ĐỦ CHI TRẢ:**

1. **Tái cấu trúc kế hoạch chi tiêu:**
   - Fund Owner nộp kế hoạch mới (ít vật phẩm hơn, giá rẻ hơn, thay đổi nhà cung cấp)
   - Staff review và duyệt kế hoạch mới

2. **General Fund hỗ trợ:**
   - Admin có thể tạo [`InternalTransaction`](campaign-service/src/main/java/com/trustfund/model/InternalTransaction.java:21) chuyển tiền từ General Fund sang chiến dịch thiếu hụt
   - Giao dịch được ghi nhận minh bạch: `fromCampaignId = 1 (General Fund)`, `toCampaignId = X`, `type = SUPPORT`

3. **Tạm dừng hoặc đóng:**
   - Staff có thể `PAUSE` chiến dịch để chờ thêm donate
   - Hoặc `CLOSE` nếu không có khả năng hoàn thành → balance chuyển về General Fund

4. **Cộng đồng hỗ trợ:**
   - Fund Owner đăng bài trên Feed Post thông báo tình hình → community có thể donate thêm
   - Chiến dịch vẫn nhận donate khi `status = "APPROVED"`

---

# 4. DONOR DONATE VƯỢT MỤC TIÊU (Overfunding)

## Q: Nếu người quyên góp donate một khoản lớn vượt xa mục tiêu (VD: mục tiêu 10 triệu, ai đó donate 50 triệu), hệ thống xử lý ra sao?

### Trả lời:

1. **FE cảnh báo trước:** [`DonationExceedWarningModal`](../danbox/src/components/donation/DonationExceedWarningModal.tsx:19) tính:
   - `remaining = goalAmount - raisedAmount` (số tiền còn thiếu)
   - `excess = donationAmount - remaining` (phần vượt mục tiêu)
   - Hiển thị rõ: bao nhiêu vào đợt hiện tại, bao nhiêu giữ cho đợt tiếp

2. **Hệ thống vẫn nhận toàn bộ:** Code [`DonationService.createPayment()`](payment-service/src/main/java/com/trustfund/service/DonationService.java:68) KHÔNG có check giới hạn donate amount. Toàn bộ `donationAmount` cộng vào `campaign.balance`.

3. **Tiền vào quỹ tổng, không phải ví riêng từng đợt:** Điều 5.5 trong điều khoản: *"Mọi khoản đóng góp được ghi nhận vào quỹ tổng duy nhất của Chiến dịch. Các hạng mục chi phí là dự toán ngân sách, không phải ví riêng biệt."*

4. **Phần vượt dùng cho đợt tiếp theo:** Nếu chiến dịch có nhiều milestone/đợt, tiền dư tự động available cho đợt sau mà không cần chuyển.

5. **Nếu chiến dịch kết thúc mà vẫn dư:** Thu hồi về General Fund khi `closeCampaign()`.

**Hạn chế:** Chưa có cơ chế **hoàn tiền tự động cho donor** khi overfund. Cần bổ sung chính sách refund-to-donor rõ ràng.

---

# 5. CHIẾN DỊCH KHÔNG AI DONATE (Underfunding)

## Q: Nếu chiến dịch được duyệt nhưng không ai donate trong thời gian dài, xử lý thế nào?

### Trả lời:

1. **Chiến dịch có `startDate` và `endDate`:** [`Campaign.endDate`](campaign-service/src/main/java/com/trustfund/model/Campaign.java:62). Tuy nhiên, code hiện tại **KHÔNG tự động đóng** chiến dịch khi hết hạn.

2. **Staff giám sát:** Staff có thể:
   - `PAUSE` chiến dịch tạm dừng
   - `CLOSE` đóng hoàn toàn
   - Liên hệ Fund Owner qua Chat Service

3. **Balance = 0 khi đóng:** Nếu không ai donate, `balance = 0` → không có tiền cần thu hồi → chỉ đổi status.

4. **Không phát sinh chi phí cho donor:** Donor chưa donate thì không bị ảnh hưởng.

**Cần cải thiện:**
- Thêm **Scheduled Task** kiểm tra chiến dịch quá `endDate` mà chưa đóng → tự động đóng hoặc gửi cảnh báo cho staff.
- Thêm chính sách "chiến dịch tối thiểu X ngày hoạt động, nếu dưới 10% mục tiêu → tự động PAUSE và thông báo".

---

# 6. FUND OWNER RÚT TIỀN NHƯNG KHÔNG MUA HÀNG

## Q: Nếu Fund Owner được giải ngân rồi không mua hàng, không nộp minh chứng, hệ thống xử lý ra sao?

### Trả lời:

Đây là **tình huống nghiêm trọng nhất** và hệ thống có **cơ chế enforcement tự động:**

**1. Evidence Deadline (Thời hạn nộp minh chứng):**
- Sau giải ngân, hệ thống tạo [`ExpenditureEvidence`](campaign-service/src/main/java/com/trustfund/model/ExpenditureEvidence.java:22) với `dueAt` = thời điểm hiện tại + `EXPENDITURE_EVIDENCE_DEADLINE_HOURS` (mặc định 48h, configurable).

**2. Casso tự động phát hiện chi tiêu:**
- Khi tài khoản chiến dịch chi tiền (amount < 0), [`CassoWebhookService`](payment-service/src/main/java/com/trustfund/service/CassoWebhookService.java:156) tự động gọi Campaign Service tạo `evidence requirement`:
  ```
  POST /api/expenditures/internal/evidence-requirement
  ```

**3. Enforcement Scheduler chạy tự động:**
- [`ExpenditureEnforcementScheduler`](campaign-service/src/main/java/com/trustfund/scheduler/ExpenditureEnforcementScheduler.java:23) chạy mỗi phút, kiểm tra evidence quá hạn:
  - Tìm evidence có `status = "PENDING"` và `dueAt < now()`
  - **ĐÓNG chiến dịch:** `campaignService.closeCampaign(campaignId, 1L)`
  - **TRỪ điểm uy tín:** `trustScoreService.addScore(userId, "LATE_SUBMIT", ...)`
  - **Đánh dấu evidence:** `status = "OVERDUE"`
  - **Gửi thông báo:** type `"CAMPAIGN_LOCKED_OVERDUE"` — *"Quỹ đã bị đóng do vi phạm minh bạch"*

**4. Hệ quả pháp lý:**
- Điều 3.4 trong điều khoản: *"Người tạo chiến dịch phải chịu hoàn toàn trách nhiệm trước pháp luật nếu có hành vi gian lận, giả mạo thông tin, làm giả chứng từ hoặc chiếm đoạt tiền đóng góp."*
- Điều 6.3: *"Trong trường hợp có dấu hiệu vi phạm pháp luật hình sự, Nền tảng có quyền và nghĩa vụ báo cáo toàn bộ thông tin, tài liệu liên quan đến cơ quan chức năng."*
- Hệ thống đã có CCCD, ảnh selfie, face descriptor qua KYC → đủ thông tin để truy cứu.

**🔧 Chi tiết kỹ thuật (Tech Deep-dive):**

- **Enforcement Scheduler config:** `ENFORCEMENT_INTERVAL_MINUTES` lưu trong `system_config` table (database-driven config). `@Scheduled(fixedDelay = 60000)` check mỗi phút nhưng chỉ chạy logic khi đủ interval. `SystemConfigRepository.findByConfigKey()` cho phép Admin thay đổi interval runtime mà không cần restart.
- **Evidence requirement flow (khi Casso phát hiện giao dịch chi):**
  ```
  Casso webhook (amount < 0) → CassoWebhookService
  → POST /api/expenditures/internal/evidence-requirement
  → Tạo ExpenditureEvidence { status: "PENDING", dueAt: now + 48h, cassoTransactionId: tid }
  → Scheduler check mỗi phút → nếu dueAt < now && status == "PENDING" → OVERDUE → closeCampaign + trừ TrustScore
  ```
- **Trust Score integration:** `TrustScoreService.addScore()` ghi `TrustScoreLog` (campaign-service DB) rồi gọi REST `PUT /api/users/{id}/trust-score` trong identity-service để sync score. Cả 2 bước trong try-catch — nếu identity-service down, log vẫn ghi nhưng score user chưa sync.
- **Notification flow:** `NotificationServiceClient.sendNotification()` → REST POST tới notification-service → Pusher push real-time → FE `NotificationBell` component nhận event → hiển thị badge + toast.

---

# 7. GIÁ THỊ TRƯỜNG BIẾN ĐỘNG SAU KHI LẬP KẾ HOẠCH

## Q: Nếu Fund Owner lập kế hoạch mua gạo 15.000đ/kg nhưng khi mua thực tế giá tăng lên 20.000đ/kg, xử lý như thế nào?

### Trả lời:

1. **`ExpenditureItem` có cả `expectedPrice` và `actualPrice`:**
   - [`ExpenditureItem.expectedPrice`](campaign-service/src/main/java/com/trustfund/model/ExpenditureItem.java:57): giá dự kiến khi lập kế hoạch
   - [`ExpenditureItem.actualPrice`](campaign-service/src/main/java/com/trustfund/model/ExpenditureItem.java:53): giá thực tế khi mua

2. **Fund Owner cập nhật actual sau khi mua:** API `updateExpenditureActuals()` cho phép cập nhật `actualPrice`, `actualQuantity`, `actualBrand`, `actualUnit`.

3. **Variance tracking:** `Expenditure.variance = totalReceivedAmount - totalAmount` → nếu variance < 0 (chi vượt), staff được cảnh báo.

4. **Minh chứng bằng hóa đơn:** Fund Owner nộp ảnh hóa đơn chứng minh giá thực tế → staff verify.

5. **AI audit giá thị trường (Perplexity):** Hệ thống tích hợp AI phân tích giá khai báo có hợp lý so với thị trường.

6. **Chênh lệch từ quỹ tổng:** Vì tiền vào quỹ tổng (không phải ví riêng từng item), chênh lệch giá được bù từ tổng balance miễn đủ tiền.

---

# 8. FUND OWNER KHAI GIÁ CAO ĐỂ CẮT TIỀN

## Q: Nếu Fund Owner cố ý khai giá cao hơn thực tế (VD: gạo giá 15.000đ nhưng khai 30.000đ) để bớt tiền, hệ thống phát hiện được không?

### Trả lời:

**Nhiều tầng bảo vệ:**

1. **AI Market Analysis (Perplexity):** Khi staff duyệt kế hoạch chi tiêu, hệ thống gửi danh sách items cho Perplexity AI phân tích giá thị trường. AI trả về:
   - Giá thị trường trung bình cho từng mặt hàng
   - Đánh giá giá khai báo có hợp lý không
   - Cảnh báo nếu có bất thường

2. **Staff manual review:** Staff so sánh `expectedPrice` với giá thị trường do AI cung cấp. Nếu nghi ngờ → từ chối kế hoạch chi tiêu, yêu cầu giải trình.

3. **Minh chứng hóa đơn thực tế:** Sau khi mua, Fund Owner phải nộp hóa đơn. `actualPrice` trên hóa đơn phải khớp hoặc gần với `expectedPrice`.

4. **Cộng đồng giám sát:** Điều 5.2b: Community Review — Nhà tài trợ và cộng đồng tham gia giám sát, phản biện minh chứng chi tiêu.

5. **Trust Score penalty:** Nếu bị phát hiện khai gian → Trust Score bị trừ nặng, ảnh hưởng uy tín cho các chiến dịch sau.

6. **Xử phạt theo Điều 6.2:** Đình chỉ chiến dịch, đóng băng số dư, hoàn trả nhà tài trợ, cấm tạo chiến dịch mới.

---

# 9. 2 NGƯỜI DONATE CÙNG LÚC → RACE CONDITION

## Q: Khi 2 người donate cùng lúc vào cùng 1 vật phẩm ITEMIZED (VD: chỉ còn 5 gói gạo, cả 2 cùng donate 5), có bị tràn số lượng không?

### Trả lời:

**Cơ chế hiện tại:**

1. **Pre-check trước khi tạo donation:** [`DonationService.checkExpenditureItemLimit()`](payment-service/src/main/java/com/trustfund/service/DonationService.java:239) gọi Campaign Service kiểm tra `quantityLeft`.

2. **Immediate deduction:** [`DonationService.processQuantityUpdate()`](payment-service/src/main/java/com/trustfund/service/DonationService.java:294) trừ `quantityLeft` ngay khi tạo donation (PENDING), không đợi PAID.

3. **Rollback khi FAILED/CANCELLED:** [`processQuantityRollback()`](payment-service/src/main/java/com/trustfund/service/DonationService.java:327) cộng lại `quantityLeft` nếu donation thất bại.

4. **Cleanup stale donations:** [`PaymentCleanupTask`](payment-service/src/main/java/com/trustfund/service/PaymentCleanupTask.java:17) chạy mỗi phút, PENDING quá 10 phút → FAILED + rollback quantity.

**Race condition vẫn có thể xảy ra:**
- 2 request check cùng lúc, cả 2 thấy `quantityLeft = 5`, cả 2 tạo donation → vượt số lượng.
- Lý do: check và deduct là **2 HTTP call riêng biệt** (Payment Service → Campaign Service), không trong cùng transaction.

**Hướng cải thiện:**
- **Pessimistic locking:** `SELECT ... FOR UPDATE` trên `quantityLeft` khi deduct.
- **Atomic operation:** Dùng `UPDATE SET quantityLeft = quantityLeft - :amount WHERE quantityLeft >= :amount` → nếu 0 rows updated thì reject.
- **Redis distributed lock:** Lock trên `expenditureItemId` khi xử lý donation.

---

# 10. DONOR CHUYỂN TIỀN SAI NỘI DUNG

## Q: Donor quét QR nhưng ghi sai nội dung chuyển khoản (VD: ghi "ung ho" thay vì "TF 12345"), tiền có bị mất không?

### Trả lời:

**Tiền KHÔNG bị mất.** Hệ thống có 3 chiến lược match:

1. **Strategy 1 — Match "TF {orderCode}":** [`CassoWebhookService.processTransactionDescription()`](payment-service/src/main/java/com/trustfund/service/CassoWebhookService.java:294) dùng regex `TF[-_\s]*(\d+)` tìm trong description. Nếu match → link với donation.

2. **Strategy 2 — Match by amount + campaignId:** Nếu Strategy 1 fail, tìm donation PENDING có cùng `campaignId` (dựa trên tài khoản nhận) và cùng `totalAmount` hoặc `donationAmount`.

3. **Strategy 3 — Fallback to most recent PENDING:** Nếu Strategy 2 cũng fail, lấy donation PENDING gần nhất của campaign đó.

4. **Anonymous Donation:** Nếu cả 3 strategy đều fail → tạo `Donation` mới với `isAnonymous = true`, `status = "PAID"`, `isBalanceSynchronized = true`. Tiền vẫn cộng vào balance.

**Quan trọng:** Vì hệ thống dùng Casso giám sát tài khoản ngân hàng, **mọi giao dịch vào tài khoản liên kết với chiến dịch** đều được ghi nhận và cộng balance, bất kể nội dung ghi gì.

---

# 11. CASSO WEBHOOK KHÔNG HOẠT ĐỘNG

## Q: Nếu Casso bị sập, webhook không gửi được, donor đã chuyển tiền nhưng hệ thống không ghi nhận, xử lý ra sao?

### Trả lời:

1. **Casso có cơ chế retry:** Khi Casso không nhận được response 200, sẽ retry webhook.

2. **Manual sync từ FE:** Endpoint `POST /api/payments/donations/{id}/sync-balance` cho phép đồng bộ thủ công.

3. **PayOS verify:** [`DonationService.verifyPayment()`](payment-service/src/main/java/com/trustfund/service/DonationService.java:173) gọi PayOS API kiểm tra trạng thái thực, đồng bộ nếu khác.

4. **Cleanup Task:** Donation PENDING quá 10 phút → tự động FAILED. Nhưng tiền thật vẫn trong tài khoản ngân hàng. Khi Casso phục hồi, webhook sẽ tạo Anonymous Donation.

5. **Idempotency:** `CassoTransaction.tid` có UNIQUE constraint + check `existsByTid()` → không bị duplicate khi Casso retry.

6. **In-memory dedup:** [`CassoWebhookService.processingTids`](payment-service/src/main/java/com/trustfund/service/CassoWebhookService.java:33) — `Set<String>` synchronized ngăn 2 thread xử lý cùng tid.

**🔧 Chi tiết kỹ thuật (Tech Deep-dive) — Toàn bộ Payment Pipeline:**

```
[Donor quét QR] → [Chuyển khoản ngân hàng] → [Casso phát hiện giao dịch]
    → [Casso POST webhook đến Vercel webhook-forwarder]
    → [webhook-forwarder/api/casso.js → Pusher.trigger('casso-webhook', 'transaction', payload)]
    → [Payment Service PusherCassoListener.java bind('transaction') → gọi CassoWebhookService]
    → [CassoWebhookService.handleCassoWebhook()]:
        1. Dedup: processingTids.add(tid) + existsByTid(tid)
        2. Verify: verifyCassoWebhookKey(accountNumber, bankCode, signature)
        3. Resolve: getCampaignIdFromAccount() → REST call Identity Service
        4. Save: cassoTransactionRepository.save() ← @Transactional rollback point
        5. Balance: updateBalanceOnlyInCampaignService() → REST PUT campaign-service
        6. Match: processTransactionDescription() → 3 strategies → set donation PAID
        7. Audit: createAuditLogForTransaction() → REST POST identity-service /api/audit
```

- **Pusher as message broker:** Thay vì Casso gọi thẳng backend (cần public URL), dùng Vercel serverless function (public) làm proxy → Pusher (pub/sub) → Payment Service subscribe. Pattern: Webhook → Serverless Proxy → Message Broker → Consumer.
- **VietQR URL format:** `https://img.vietqr.io/image/{bankCode}-{accNum}-compact2.png?amount={amount}&addInfo=TF%20{orderCode}&accountName={name}` — chuẩn Napas, hỗ trợ mọi ngân hàng VN.
- **RestTemplate timeout:** Payment Service có 2 RestTemplate: default (5s connect/read timeout cho campaign-service) và `campaignEnrichmentRestTemplate` (2s timeout cho enrichment calls không critical).

---

# 12. ĐÓNG CHIẾN DỊCH — SỐ DƯ XỬ LÝ NHƯ THẾ NÀO

## Q: Khi staff đóng chiến dịch, số dư còn lại trong balance đi đâu?

### Trả lời:

[`CampaignServiceImpl.closeCampaign()`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:355):

```java
campaign.setStatus("CLOSED");
campaignRepository.save(campaign);
```

**Hiện tại code chỉ đổi status.** Phần thu hồi balance về General Fund được xử lý thông qua [`InternalTransactionService`](campaign-service/src/main/java/com/trustfund/model/InternalTransaction.java:21) với `type = RECOVERY`:

- Admin/Staff tạo giao dịch nội bộ: `fromCampaignId = X` → `toCampaignId = 1 (General Fund)`
- Trừ balance campaign X, cộng balance General Fund
- Giao dịch được ghi nhận minh bạch

**Hạn chế:** Code `closeCampaign()` **chưa tự động** tạo RECOVERY transaction. Staff phải làm thủ công qua dashboard.

**Cần cải thiện:** Thêm logic tự động vào `closeCampaign()`:
```
if (balance > 0) {
    internalTransactionService.createRecovery(campaignId, balance);
    updateBalance(campaignId, -balance);
}
```

---

# 13. CHIẾN DỊCH BỊ FLAG NHIỀU LẦN

## Q: Nếu chiến dịch bị nhiều người tố cáo (flag), hệ thống tự động xử lý hay chờ staff?

### Trả lời:

1. **Chống duplicate flag:** Mỗi user chỉ flag 1 lần cho mỗi campaign. `existsByUserIdAndCampaignId()` check trước khi tạo.

2. **Thông báo follower:** Khi có flag mới, tất cả follower nhận notification cảnh báo.

3. **Staff review:** Flag tạo `ApprovalTask` assign cho staff. Staff quyết định:
   - `RESOLVED` — flag không có cơ sở
   - `PAUSE` chiến dịch để điều tra
   - `CLOSE` chiến dịch nếu vi phạm

4. **Hiện tại KHÔNG tự động đóng:** Dù có 100 flag, hệ thống vẫn **chờ staff** quyết định.

5. **AI Flag Analysis:** Hệ thống gửi nội dung flag cho AI phân tích → giúp staff đánh giá.

**Cần cải thiện:**
- Thêm threshold: VD 10+ flags trong 24h → tự động `PAUSE` chiến dịch
- Thêm priority queue: chiến dịch nhiều flag được staff review trước

---

# 14. PAYMENT SERVICE CHẾT GIỮA GIAO DỊCH

## Q: Nếu Payment Service bị crash giữa lúc đang xử lý giao dịch Casso webhook, tiền có bị mất?

### Trả lời:

Phân tích flow trong [`CassoWebhookService.handleCassoWebhook()`](payment-service/src/main/java/com/trustfund/service/CassoWebhookService.java:43):

| Bước | Hành động | Crash tại đây? | Hệ quả |
|------|-----------|----------------|--------|
| 1 | `cassoTransactionRepository.save(transaction)` | Crash trước save → Casso retry | Không mất |
| 2 | `updateBalanceOnlyInCampaignService()` | Crash giữa → CassoTx đã save, balance chưa update | **Inconsistent** |
| 3 | `processTransactionDescription()` → match donation | Crash giữa → donation chưa match | PENDING donation sẽ bị cleanup thành FAILED |
| 4 | Anonymous donation fallback | Crash → không tạo anonymous | Tiền trong bank, balance đã cập nhật ở bước 2 |

**Kịch bản nguy hiểm nhất:** Bước 2 thành công (balance đã tăng) nhưng bước 3 fail (donation chưa match). Kết quả:
- Balance campaign tăng (đúng, vì tiền thật đã vào bank)
- Donation vẫn PENDING → sau 10 phút bị cleanup thành FAILED → quantity bị rollback
- **Nhưng balance KHÔNG bị rollback** (vì balance đã cập nhật trực tiếp từ Casso transaction, không qua donation)

**Giải pháp hiện tại:**
- `CassoTransaction` đã lưu → có audit trail
- `isBalanceSynchronized` flag ngăn double-count
- Staff đối soát qua dashboard

---

# 15. DISTRIBUTED TRANSACTION — BALANCE KHÔNG ĐỒNG BỘ

## Q: Khi Donation status = PAID nhưng Campaign balance chưa tăng (vì updateBalance() fail), xử lý thế nào?

### Trả lời:

**Đây là vấn đề kinh điển của microservices** — không có distributed transaction.

**Flow nguy hiểm:**
```
1. CassoWebhookService lưu CassoTransaction ✅
2. updateBalanceOnlyInCampaignService() gọi Campaign Service ❌ FAIL
3. processTransactionDescription() → set donation PAID ✅
```

Kết quả: Tiền vào bank, donation PAID, **nhưng campaign.balance không tăng**.

**Cơ chế hiện tại:**
- `isBalanceSynchronized` flag trên Donation: nếu false → chưa sync
- `DonationService.syncBalanceForDonation()` cho phép retry thủ công
- `updateBalanceOnlyInCampaignService()` throw exception nếu fail → toàn bộ `@Transactional` trong CassoWebhookService rollback (CassoTransaction cũng rollback!)

**Thực tế:** Vì `@Transactional` bao toàn bộ `handleCassoWebhook()`, nếu `updateBalance` fail → cả CassoTransaction cũng rollback. Casso sẽ retry → lần sau xử lý lại từ đầu. **Đây là thiết kế "all-or-nothing" an toàn.**

**Hạn chế:** Nếu Campaign Service down dài hạn → Casso retry nhiều lần cũng fail → giao dịch bị kẹt.

**Hướng cải thiện:**
1. **Outbox Pattern:** Save event "cần update balance" cùng transaction với CassoTransaction. Scheduler retry cho đến khi thành công.
2. **Reconciliation Job:** Cron job chạy đêm, so sánh CassoTransaction vs Campaign.balance, tự sửa inconsistency.

---

# 16. BALANCE ÂM — CÓ THỂ XẢY RA KHÔNG?

## Q: Campaign balance có thể âm không? Hệ thống có check không?

### Trả lời:

**Có thể xảy ra.** [`CampaignServiceImpl.updateBalance()`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:365):

```java
BigDecimal oldBalance = campaign.getBalance() != null ? campaign.getBalance() : BigDecimal.ZERO;
campaign.setBalance(oldBalance.add(amount)); // amount có thể âm!
```

**KHÔNG có check `balance >= 0`**. Nếu `amount` là số âm lớn (VD: giao dịch chi tiêu âm từ Casso), balance có thể âm.

**Khi nào xảy ra:**
- Casso phát hiện giao dịch chi (amount < 0) → `updateBalanceOnlyInCampaignService(campaignId, negativeAmount)` → balance giảm, có thể âm.
- Staff giải ngân (disbursement) → balance giảm.

**Cần cải thiện:**
```java
BigDecimal newBalance = oldBalance.add(amount);
if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
    log.warn("Balance would be negative for campaign {}: {}", id, newBalance);
    // Có thể throw hoặc chỉ cảnh báo tùy policy
}
campaign.setBalance(newBalance);
```

---

# 17. GIAO DỊCH CHI ÂM TỪ TÀI KHOẢN CHIẾN DỊCH

## Q: Khi Casso phát hiện tiền bị rút khỏi tài khoản chiến dịch (giao dịch âm), hệ thống xử lý ra sao?

### Trả lời:

[`CassoWebhookService`](payment-service/src/main/java/com/trustfund/service/CassoWebhookService.java:156) xử lý giao dịch âm:

```java
if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
    // 1. Cập nhật balance (trừ)
    donationService.updateBalanceOnlyInCampaignService(campaignId, amount); // amount < 0

    // 2. Tạo evidence requirement (yêu cầu nộp minh chứng)
    POST /api/expenditures/internal/evidence-requirement
}
```

**Flow:**
1. **Balance giảm tự động:** Phản ánh đúng thực tế — tiền đã rời tài khoản.
2. **Evidence requirement tạo tự động:** Hệ thống tạo [`ExpenditureEvidence`](campaign-service/src/main/java/com/trustfund/model/ExpenditureEvidence.java:22) với `status = "PENDING"`, `dueAt` = now + deadline hours.
3. **Fund Owner phải nộp minh chứng:** Upload hóa đơn, ảnh chứng minh mục đích chi.
4. **Nếu quá hạn:** `ExpenditureEnforcementScheduler` tự động đóng chiến dịch + trừ trust score.

**Đây là cơ chế "giám sát ngân hàng tự động" — điểm nổi bật nhất của TrustFundMe.**

---

# 18. ĐIỀU KHOẢN PHÁP LÝ — NỀN TẢNG MIỄN TRỪ TRÁCH NHIỆM

## Q: Nền tảng TrustFundMe miễn trừ trách nhiệm như thế nào? Nếu Fund Owner lừa đảo, nền tảng có bị liên đới?

### Trả lời:

Theo [`fullRiskTermsVietnamese`](../danbox/src/components/campaign/new-campaign-test/mockData.ts:20):

**Điều 7 — Miễn trừ trách nhiệm:**

1. **7.1:** Nền tảng là **trung gian công nghệ**, không chịu trách nhiệm về kết quả chiến dịch hoặc hành vi Fund Owner.

2. **7.2:** Không đảm bảo chiến dịch đạt mục tiêu gây quỹ.

3. **7.3:** Bất khả kháng (force majeure): thiên tai, dịch bệnh, sự cố hạ tầng...

4. **7.4:** Tổng mức bồi thường **không vượt quá phí dịch vụ** đã thu từ chiến dịch.

**Bảo vệ nền tảng:**
- Fund Owner **ký cam kết** (Commitment) khi tạo chiến dịch, ghi nhận CCCD, chữ ký số, IP address.
- Nền tảng **không giữ tiền** — tiền vào thẳng tài khoản ngân hàng Fund Owner → giảm rủi ro pháp lý.
- Điều 6.3: Nền tảng **báo cáo cơ quan chức năng** nếu phát hiện vi phạm hình sự.

**Quan trọng cho bảo vệ:** Nền tảng hoạt động tương tự GoFundMe, Kitabisa — là **trung gian kết nối**, không phải tổ chức tài chính.

---

# 19. CHIẾN DỊCH ITEMIZED VS AUTHORIZED — KHI NÀO DÙNG

## Q: Có 2 loại chiến dịch: ITEMIZED và AUTHORIZED. Khi nào dùng cái nào? Sự khác biệt?

### Trả lời:

| Tiêu chí | ITEMIZED | AUTHORIZED |
|----------|----------|------------|
| **Mục đích** | Mua vật phẩm cụ thể (gạo, sữa, sách) | Chi tiền trực tiếp cho mục đích chung |
| **Donor chọn** | Chọn vật phẩm + số lượng | Chọn số tiền |
| **quantityLeft** | Có — giảm khi donate | Không |
| **Duyệt chi tiêu** | Staff duyệt → Fund Owner yêu cầu rút → Staff giải ngân | Staff duyệt → Tự động WITHDRAWAL_REQUESTED → Staff giải ngân |
| **Ví dụ** | "Mua 100 phần gạo cho bà con vùng lũ" | "Hỗ trợ xây trường học cho trẻ em vùng cao" |

**ITEMIZED:**
- Minh bạch hơn: donor thấy chính xác mua gì, giá bao nhiêu
- [`ExpenditureItem.quantityLeft`](campaign-service/src/main/java/com/trustfund/model/ExpenditureItem.java:51) giảm khi có donation
- [`DonationItem`](payment-service/src/main/java/com/trustfund/model/DonationItem.java) ghi nhận từng item trong donation

**AUTHORIZED:**
- Linh hoạt hơn: phù hợp dự án phức tạp không thể liệt kê từng item
- Chi tiêu theo kế hoạch tổng, staff duyệt tổng thể

---

# 20. COMMITMENT (CAM KẾT) CÓ GIÁ TRỊ PHÁP LÝ KHÔNG

## Q: CampaignCommitment mà Fund Owner ký khi tạo chiến dịch có giá trị pháp lý thực sự không?

### Trả lời:

[`CampaignCommitment`](campaign-service/src/main/java/com/trustfund/model/CampaignCommitment.java:18) lưu trữ:
- `fullName`, `idNumber` (CCCD), `address`, `workplace`, `phoneNumber`
- `issuePlace`, `issueDate` (nơi cấp, ngày cấp CCCD)
- `content` (nội dung cam kết — TEXT)
- `signatureUrl` (chữ ký số — LONGTEXT, base64)
- `ipAddress` (IP khi ký)
- `status = "SIGNED"`

**Giá trị pháp lý:**

1. **Theo Luật Giao dịch điện tử 2023 (Luật số 20/2023/QH15):** Hợp đồng điện tử, chữ ký điện tử có giá trị pháp lý tương đương hợp đồng giấy nếu đáp ứng các điều kiện về tính toàn vẹn, định danh.

2. **Commitment lưu đầy đủ:**
   - Định danh: CCCD + KYC face verification
   - Nội dung cam kết rõ ràng
   - Chữ ký số (vẽ trên canvas → base64)
   - IP address + timestamp → bằng chứng thời điểm ký
   - Không thể sửa sau khi ký (immutable)

3. **Kết hợp KYC:** Commitment liên kết với user đã qua KYC (CCCD + face) → xác minh người ký.

4. **Giá trị trong tố tụng:** Dù không phải chữ ký số theo chuẩn VNPT/Viettel CA, bộ dữ liệu (CCCD + face + signature + IP + content) đủ để làm **bằng chứng điện tử** trong tố tụng dân sự.

---

# 21. TẠI SAO TIỀN VÀO THẲNG TÀI KHOẢN CHỦ QUỸ

## Q: Tại sao hệ thống cho tiền vào thẳng tài khoản ngân hàng của Fund Owner thay vì giữ tiền qua nền tảng (escrow)?

### Trả lời:

**Lý do thiết kế:**

1. **Pháp lý:** Để giữ tiền trung gian (escrow), nền tảng cần **giấy phép hoạt động thanh toán trung gian** theo Nghị định 101/2012/NĐ-CP. Đây là yêu cầu pháp lý phức tạp, tốn kém cho đồ án sinh viên.

2. **Giảm rủi ro pháp lý:** Nền tảng không giữ tiền → không bị coi là tổ chức tài chính → giảm nghĩa vụ pháp lý.

3. **Minh bạch hơn:** Donor chuyển tiền trực tiếp vào tài khoản tách bạch → có thể tự kiểm tra sao kê ngân hàng.

4. **Nhanh hơn:** Không cần chờ nền tảng giải ngân từ escrow → Fund Owner nhận tiền ngay.

**Bù đắp nhược điểm:**
- **Casso webhook** giám sát tài khoản 24/7 → mọi giao dịch vào/ra đều được ghi nhận
- **Evidence requirement tự động** khi có giao dịch chi
- **Enforcement scheduler** đóng quỹ nếu vi phạm
- **KYC bắt buộc** + **Commitment** → truy cứu nếu gian lận

**Theo điều khoản FE:** [`decree93SeparateAccountNotice`](../danbox/src/components/campaign/new-campaign-test/mockData.ts:10): *"Chủ tài khoản cam kết sử dụng tài khoản thanh toán tách bạch, phục vụ xử lý dòng tiền gây quỹ minh bạch và đối soát, không trộn lẫn mục đích cá nhân."*

---

# 22. NẾU NHÀ TÀI TRỢ YÊU CẦU HOÀN TIỀN

## Q: Donor đã quyên góp nhưng muốn lấy lại tiền, hệ thống hỗ trợ không?

### Trả lời:

**Hiện tại: KHÔNG hỗ trợ refund-to-donor tự động.**

**Lý do:**
- Tiền đã chuyển trực tiếp vào tài khoản Fund Owner → nền tảng không giữ tiền → không thể hoàn.
- Việc hoàn tiền cần thương lượng giữa donor và Fund Owner.

**Điều khoản đã quy định:**
- Điều 4.3: *"Nhà tài trợ hiểu rõ và đồng ý rằng mọi khoản đóng góp mang tính tự nguyện. Trừ trường hợp Chiến dịch bị hủy do vi phạm quy định, các khoản đóng góp đã thực hiện sẽ không được hoàn lại."*

**Trường hợp đặc biệt — Chiến dịch bị hủy do vi phạm:**
- Điều 6.2c: *"Tiến hành hoàn trả số tiền còn lại cho Nhà tài trợ theo tỷ lệ đóng góp tương ứng."*
- Nhưng code **chưa implement** tính năng này. Hiện tại: balance bị freeze → chuyển về General Fund → Admin xử lý thủ công.

**Hướng cải thiện:**
- Thêm tính năng "Yêu cầu hoàn tiền" cho donor
- Khi chiến dịch bị hủy, tự động tính tỷ lệ refund cho từng donor
- Gửi yêu cầu hoàn tiền đến Fund Owner

---

# 23. TRUST SCORE BỊ THAO TÚNG

## Q: Fund Owner có thể "farm" Trust Score bằng cách tạo nhiều chiến dịch nhỏ, tự donate, rồi nộp minh chứng để cộng điểm?

### Trả lời:

**Rủi ro thực tế.** Tuy nhiên, nhiều barrier:

1. **KYC bắt buộc:** Mỗi CCCD chỉ đăng ký 1 tài khoản → không tạo được nhiều account.

2. **Staff duyệt mỗi chiến dịch:** Chiến dịch "giả" sẽ bị staff phát hiện khi review.

3. **Duplicate check cho DAILY_POST:** `existsByUserIdAndRuleKeyAndCreatedAtAfter()` → chỉ cộng 1 lần/ngày.

4. **Reference check:** `findByUserIdAndRuleKeyAndReference()` → không cộng 2 lần cho cùng campaign.

5. **Self-donate:** Code **KHÔNG chặn** Fund Owner donate vào chính chiến dịch của mình. Đây là lỗ hổng cần fix.

**Hướng cải thiện:**
- Check `donorId !== fundOwnerId` khi tạo donation
- Decay mechanism: Trust Score cũ giảm dần theo thời gian
- Weighted scoring: chiến dịch lớn (nhiều donor unique) cộng nhiều điểm hơn chiến dịch nhỏ

---

# 24. GENERAL FUND — AI KIỂM SOÁT QUỸ CHUNG

## Q: General Fund (quỹ chung) có bao nhiêu tiền? Ai có quyền rút? Có bị lạm dụng không?

### Trả lời:

**General Fund** là [`Campaign`](campaign-service/src/main/java/com/trustfund/model/Campaign.java:81) với `type = "GENERAL_FUND"`, `id = 1`:

**Nguồn thu:**
- Tip từ donor (`tipAmount`)
- Recovery khi đóng chiến dịch (balance còn lại)
- Điều chuyển nội bộ

**Chi tiêu:**
- Chỉ **ADMIN** có quyền tạo `InternalTransaction` từ General Fund
- Mỗi giao dịch phải có `reason`, `createdByStaffId`, `evidenceImageId`
- Status flow: `PENDING → APPROVED/COMPLETED`

**Chống lạm dụng:**
- `InternalTransaction` ghi nhận `createdByStaffId` → audit trail
- Balance check: `fromCampaign.balance >= amount` trước khi chuyển
- Không hiển thị General Fund trong danh sách công khai

**Hạn chế:**
- Chỉ 1 Admin level, không có "4 eyes principle" (2 người duyệt)
- Không có giới hạn số tiền Admin chuyển
- Cần thêm: approval workflow cho giao dịch General Fund lớn

---

# 25. TẠI SAO CẦN KYC TRƯỚC KHI TẠO CHIẾN DỊCH

## Q: Tại sao bắt buộc KYC? Nếu bỏ KYC thì sao?

### Trả lời:

**KYC (Know Your Customer) bắt buộc vì:**

1. **Pháp lý:** Nghị định 93/2021/NĐ-CP yêu cầu xác minh danh tính tổ chức/cá nhân vận động quyên góp.

2. **Chống lừa đảo:** Xác minh CCCD + face verification → biết chính xác ai tạo chiến dịch → truy cứu nếu gian lận.

3. **Trust building:** Donor tin tưởng hơn khi biết Fund Owner đã được xác minh.

4. **Code enforcement:**
   - [`reviewCampaign()`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:190): `if (!verificationStatus.isKycVerified()) throw "Cannot approve campaign"` → **không thể duyệt** chiến dịch nếu chưa KYC.
   - BR-21: *"Campaign can only be APPROVED when fund owner has verified KYC."*

**Nếu bỏ KYC:**
- Ai cũng tạo được chiến dịch giả → lừa đảo tràn lan
- Không có thông tin truy cứu khi vi phạm
- Donor mất niềm tin → nền tảng chết
- Vi phạm quy định pháp luật

---

# PHỤ LỤC: BẢNG TÓM TẮT CÂU HỎI & CƠ CHẾ

| # | Tình huống | Cơ chế xử lý | Evidence trong code |
|---|-----------|---------------|---------------------|
| 1 | Ngân sách quá thấp | Tái cấu trúc milestone + General Fund hỗ trợ | `InternalTransaction`, `riskPolicyExcerpt` |
| 2 | Ngân sách quá cao | FE warning modal + Tiền vào quỹ tổng | `DonationExceedWarningModal.tsx` |
| 3 | Chi vượt mức | Variance tracking + Minh chứng bắt buộc | `Expenditure.variance`, `ExpenditureEvidence` |
| 4 | Ngân sách thừa cuối kỳ | Refund + Recovery về General Fund | `ExpenditureTransaction.type=REFUND` |
| 5 | Fund Owner không nộp minh chứng | Enforcement scheduler đóng quỹ + trừ Trust Score | `ExpenditureEnforcementScheduler.java` |
| 6 | Giá biến động | actualPrice vs expectedPrice + AI audit | `ExpenditureItem.actualPrice` |
| 7 | Khai giá cao | AI Perplexity market analysis + Staff review | `ai_market_analysis_prompt.txt` |
| 8 | Race condition quantity | Immediate deduction + Cleanup task | `processQuantityUpdate()`, `PaymentCleanupTask` |
| 9 | Chuyển tiền sai nội dung | 3 strategies match + Anonymous donation fallback | `CassoWebhookService.processTransactionDescription()` |
| 10 | Casso down | Manual sync + PayOS verify + Casso retry | `syncBalanceForDonation()`, `verifyPayment()` |
| 11 | Đóng chiến dịch | Balance → General Fund | `closeCampaign()`, `InternalTransaction.RECOVERY` |
| 12 | Flag spam | Dedup per user + Staff review | `existsByUserIdAndCampaignId()` |
| 13 | Payment crash | @Transactional rollback + Casso retry | `handleCassoWebhook()` annotation |
| 14 | Balance không đồng bộ | isBalanceSynchronized + manual sync | `Donation.isBalanceSynchronized` |
| 15 | Balance âm | Casso giao dịch chi → auto evidence | `CassoWebhookService` negative amount |
| 16 | Hoàn tiền donor | Theo điều khoản — chỉ khi vi phạm | Điều 4.3, 6.2c |
| 17 | Trust Score farm | KYC 1:1 + Staff review + Duplicate check | `existsByUserIdAndRuleKeyAndCreatedAtAfter()` |
| 18 | General Fund lạm dụng | Audit trail + Balance check | `InternalTransaction.createdByStaffId` |

---

# LƯU Ý KHI BẢO VỆ

1. **Luôn dẫn chứng code cụ thể:** Khi trả lời, nêu tên file, tên method, số dòng.
2. **Nhấn mạnh 3 điểm nổi bật nhất:**
   - **Casso auto-monitoring:** Giám sát ngân hàng tự động 24/7
   - **Enforcement scheduler:** Đóng quỹ tự động khi vi phạm
   - **AI market audit:** Perplexity kiểm tra giá thị trường
3. **Thừa nhận hạn chế khi được hỏi:** VD: "Chưa có check balance >= 0" hoặc "Chưa có auto-close khi hết endDate" → rồi đề xuất cách fix.
4. **Liên hệ điều khoản pháp lý:** Mỗi câu trả lời nên tham chiếu Điều X trong `fullRiskTermsVietnamese` nếu liên quan.
5. **Demo live nếu có thể:** Tạo donation → Xem QR → Chuyển tiền → Xem balance cập nhật → Xem evidence requirement.

---

*Tài liệu được tạo dựa trên phân tích toàn bộ source code TrustFundMe-BE, danbox (FE), và điều khoản sử dụng.*
*Tổng: 25 câu hỏi ngặt nghèo nhất liên quan đến phần chiến dịch.*
*Cập nhật: 2026-05-06*
