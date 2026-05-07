# KỊCH BẢN TRÌNH BÀY LUỒNG 2 — TẠO CHIẾN DỊCH & DUYỆT

> **Người trình bày:** Trinh
> **Cập nhật:** 2026-05-07
> **Lưu ý:** Hội đồng không hỏi thì **ĐỪNG** giải thích khúc cảnh báo không trùng tên chủ tài khoản.

---

## TỔNG QUAN LUỒNG 2

```
USER (Hùng) tạo chiến dịch  ──→  Nhập STK + Casso  ──→  Nhập Step 2~5  ──→  Gửi (Random assign)
      ↓
STAFF nhận task (random)  ──→  Review thông tin  ──→  Gửi cam kết (email)
      ↓
USER (Hùng) vào mail  ──→  Ký cam kết (commitment)  ──→  Xác thực
      ↓
STAFF xác nhận cam kết  ──→  Duyệt chiến dịch  ──→  APPROVED → Chiến dịch hoạt động
```

---

## PHẦN 1: USER — TẠO CHIẾN DỊCH (Trinh demo)

### 1.1. Vào trang tạo chiến dịch

> **Trinh nói:**
>
> "Bây giờ em sẽ demo luồng tạo chiến dịch. User Hùng đã hoàn tất KYC ở luồng trước, bây giờ tiến hành tạo chiến dịch mới."

- Truy cập `/new-campaign-test`
- Hệ thống load **Step 1 – Eligibility** (Kiểm tra điều kiện)

---

### 1.2. Step 1 — Điều kiện tiên quyết & Nhập STK

> **Trinh nói:**
>
> "Step 1 kiểm tra điều kiện tiên quyết. Hệ thống tự động load KYC đã verified ở luồng trước. Tiếp theo, user cần nhập **số tài khoản ngân hàng** để nhận tiền quyên góp."

**Demo nhập STK:**

- Chọn ngân hàng (dropdown VietQR API — hỗ trợ toàn bộ ngân hàng VN)
- Nhập số tài khoản
- Nhập tên chủ tài khoản

> **Trinh nhắc tới Nghị định 31 Điều 17.2:**
>
> "Theo **Nghị định 31/2024/NĐ-CP**, Điều 17 khoản 2 quy định về việc xác minh thông tin chủ tài khoản — tài khoản thanh toán phải trùng khớp thông tin với người đăng ký. Hệ thống TrustFundMe **bắt buộc tên chủ tài khoản phải trùng khớp với KYC** đã xác minh trước đó. Đây là yêu cầu pháp lý để đảm bảo minh bạch dòng tiền."

> **Trinh giải thích thêm về tài khoản tách bạch:**
>
> "Ngoài ra, theo **Nghị định 93/2021/NĐ-CP** về vận động quyên góp, chủ tài khoản cam kết sử dụng tài khoản thanh toán tách bạch — tức tài khoản này chỉ dùng cho mục đích gây quỹ, không trộn lẫn mục đích cá nhân."

**📌 Code evidence:**
- FE: [`Step1Eligibility.tsx`](../danbox/src/components/campaign/new-campaign-test/steps/Step1Eligibility.tsx) — component nhập bank info
- BE: [`BankAccountService`](identity-service/src/main/java/com/trustfund/service/BankAccountService.java) — lưu và validate bank account
- Quy định: [`decree93SeparateAccountNotice`](../danbox/src/components/campaign/new-campaign-test/mockData.ts:10)

---

### 1.3. Trình bày Casso — Webhook giám sát ngân hàng

> **Trinh nói:**
>
> "Khi user nhập STK, hệ thống sẽ liên kết tài khoản này với Casso. Em giải thích Casso hoạt động như thế nào."

> **Trinh giải thích Casso:**
>
> "**Casso** là dịch vụ giám sát giao dịch ngân hàng. Cơ chế hoạt động: **Casso chủ động gọi về server của mình mỗi khi có giao dịch mới phát sinh trên tài khoản ngân hàng đã kết nối** (webhook pattern).
>
> Nghĩa là, khi có người chuyển tiền vào tài khoản chiến dịch, Casso **tự động** gửi webhook đến hệ thống TrustFundMe chứa thông tin giao dịch: số tiền, nội dung chuyển khoản, thời gian, mã giao dịch. Hệ thống nhận webhook rồi match với donation tương ứng.
>
> Tương tự, khi chủ quỹ **chi tiền** từ tài khoản (giao dịch âm), Casso cũng phát hiện và báo về — hệ thống tự động tạo yêu cầu nộp minh chứng chi tiêu."

> **Trinh nói thêm (nếu có thời gian):**
>
> "Flow kỹ thuật cụ thể:
> 1. Casso phát hiện giao dịch → gửi webhook đến Vercel serverless function (public endpoint)
> 2. Serverless function forward qua **Pusher** (message broker)
> 3. Payment Service subscribe Pusher channel → nhận và xử lý giao dịch
>
> Pattern này giúp backend không cần expose public URL — Pusher đóng vai trò trung gian."

**📌 Code evidence:**
- [`CassoWebhookService.java`](payment-service/src/main/java/com/trustfund/service/CassoWebhookService.java) — xử lý webhook
- [`PusherCassoListener.java`](payment-service/src/main/java/com/trustfund/config/PusherCassoListener.java) — subscribe Pusher
- [`webhook-forwarder/api/casso.js`](webhook-forwarder/api/casso.js) — Vercel proxy

---

### 1.4. Step 2 — Thông tin chiến dịch

> **Trinh nói:**
>
> "Bước 2, user nhập thông tin chiến dịch: tiêu đề, mô tả, danh mục, ảnh bìa, ngày bắt đầu và kết thúc."

**Demo:**
- Nhập tiêu đề: VD "Hỗ trợ 100 phần gạo cho bà con vùng lũ Quảng Nam"
- Chọn danh mục: "Thiên tai & Cứu trợ"
- Nhập mô tả (hoặc dùng AI generate)
- Upload ảnh bìa
- Chọn ngày bắt đầu / kết thúc

> **Trinh nói:**
>
> "Hệ thống có tích hợp **AI tạo mô tả** — user nhập thông tin cơ bản, AI sẽ gợi ý mô tả chiến dịch phù hợp. Tất nhiên, user có thể sửa lại theo ý mình."

**📌 Code evidence:**
- FE: [`Step2CampaignForm.tsx`](../danbox/src/components/campaign/new-campaign-test/steps/Step2CampaignForm.tsx)
- AI: [`AIDescriptionModal.tsx`](../danbox/src/components/campaign/creation/AIDescriptionModal.tsx)

---

### 1.5. Step 3 — Ngân sách & Milestones

> **Trinh nói:**
>
> "Bước 3, user lập kế hoạch ngân sách theo đợt (milestones). Mỗi đợt có danh sách hạng mục chi tiêu, từng hạng mục có các vật phẩm cụ thể với giá dự kiến và số lượng."

**Demo:**
- Thêm milestone: "Đợt 1 — Mua lương thực"
- Thêm hạng mục: "Lương thực"
  - Gạo 5kg — 150,000đ × 100
  - Mì tôm — 5,000đ × 200
- Tổng dự kiến: 16,000,000đ

> **Trinh nói:**
>
> "Kế hoạch chi tiêu chi tiết giúp donor biết tiền được dùng vào việc gì. Sau khi staff duyệt, hệ thống dùng **Perplexity AI** kiểm tra giá khai báo có hợp lý so với thị trường không — phát hiện nếu chủ quỹ khai giá cao."

**📌 Code evidence:**
- FE: [`Step3Milestones.tsx`](../danbox/src/components/campaign/new-campaign-test/steps/Step3Milestones.tsx)
- BE: [`Expenditure.java`](campaign-service/src/main/java/com/trustfund/model/Expenditure.java), [`ExpenditureItem.java`](campaign-service/src/main/java/com/trustfund/model/ExpenditureItem.java)

---

### 1.6. Step 4 — Tài khoản nhận tiền (xác nhận)

> **Trinh nói:**
>
> "Step 4 xác nhận lại thông tin tài khoản ngân hàng đã nhập ở Step 1. Hệ thống hiển thị rõ: tên chủ tài khoản, số tài khoản, ngân hàng, chi nhánh."

**📌 Code evidence:**
- FE: [`Step4BankTerms.tsx`](../danbox/src/components/campaign/new-campaign-test/steps/Step4BankTerms.tsx)

---

### 1.7. Step 5 — Điều khoản & Rủi ro

> **Trinh nói:**
>
> "Step 5 là phần quan trọng — user phải đọc và đồng ý toàn bộ **điều khoản sử dụng nền tảng**. Điều khoản bao gồm 10 điều với nội dung:
> - Trách nhiệm minh bạch chi tiêu
> - Nghĩa vụ nộp minh chứng
> - Chế tài vi phạm (đóng quỹ, trừ uy tín, báo cơ quan chức năng)
> - Miễn trừ trách nhiệm nền tảng
> - Bảo mật thông tin cá nhân
>
> User phải cuộn xuống cuối để đọc hết rồi tick đồng ý."

**📌 Code evidence:**
- FE: [`Step5RiskTerms.tsx`](../danbox/src/components/campaign/new-campaign-test/steps/Step5RiskTerms.tsx)
- Nội dung: [`fullRiskTermsVietnamese`](../danbox/src/components/campaign/new-campaign-test/mockData.ts:20)

---

### 1.8. Step 6 — Review & Gửi + Tính năng Random

> **Trinh nói:**
>
> "Step 6 là trang tổng hợp — hệ thống tóm tắt lại toàn bộ thông tin: chiến dịch, ngân sách, tài khoản, điều khoản. User kiểm tra lần cuối rồi nhấn **Gửi**."

**Nhấn nút Gửi — Demo:**

> **Trinh nói về tính năng Random:**
>
> "Sau khi user gửi chiến dịch, hệ thống tự động tạo **ApprovalTask** và **random assign** cho 1 staff trong danh sách. Cơ chế random này nhằm:
> 1. **Chống thông đồng:** Staff không biết trước sẽ nhận task nào → không thể sắp xếp trước với chủ quỹ.
> 2. **Phân bổ đều:** Mỗi staff đều có cơ hội nhận task, tránh 1 người bị quá tải.
> 3. **Minh bạch:** Task được ghi nhận `staffId` → truy vết được ai duyệt.
>
> Nếu Admin thấy cần đổi người, có API **reassignTask** để giao lại cho staff khác."

**📌 Code evidence — Random Assignment:**
```java
// ApprovalTaskServiceImpl.java
List<Long> staffIds = identityServiceClient.getStaffIds();
Long assignedStaffId = staffIds.get(random.nextInt(staffIds.size()));

ApprovalTask task = ApprovalTask.builder()
    .type("CAMPAIGN")
    .targetId(campaignId)
    .staffId(assignedStaffId)
    .status("PENDING")
    .build();
```
- BE: [`ApprovalTaskServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/ApprovalTaskServiceImpl.java:26)
- FE: [`Step6ReviewSubmit.tsx`](../danbox/src/components/campaign/new-campaign-test/steps/Step6ReviewSubmit.tsx)

---

## PHẦN 2: STAFF — REVIEW & GỬI CAM KẾT (Trinh demo)

### 2.1. Vào tab Staff — Nhận task random

> **Trinh nói:**
>
> "Bây giờ em chuyển sang tài khoản Staff. Khi staff đăng nhập, vào dashboard thấy danh sách **approval tasks** — trong đó có task vừa được random assign."

**Demo:**
- Đăng nhập staff
- Vào tab "Duyệt chiến dịch" / "Approval Tasks"
- Thấy task mới: "Chiến dịch #X — Hỗ trợ 100 phần gạo..." — Status: PENDING

---

### 2.2. Show thông tin user đã nộp

> **Trinh nói:**
>
> "Staff click vào task để xem chi tiết. Hệ thống hiển thị **toàn bộ** thông tin user đã nộp:"

**Demo — show từng phần:**

| Mục | Nội dung hiển thị |
|-----|-------------------|
| **Thông tin chiến dịch** | Tiêu đề, mô tả, danh mục, ảnh bìa, ngày bắt đầu/kết thúc |
| **Thông tin chủ quỹ** | Họ tên, email, vai trò (FUND_OWNER) |
| **KYC** | Số CCCD, ảnh mặt trước/sau, ảnh selfie, trạng thái VERIFIED |
| **Tài khoản ngân hàng** | Tên chủ TK, số TK, ngân hàng, chi nhánh |
| **Kế hoạch ngân sách** | Các milestone, hạng mục, vật phẩm, giá, số lượng |
| **Điều khoản** | User đã đồng ý Terms & Conditions |

> **Trinh nói:**
>
> "Staff kiểm tra toàn bộ thông tin: KYC đã verified chưa, thông tin chiến dịch hợp lý không, kế hoạch ngân sách có khả thi không. Nếu cần, staff có thể yêu cầu AI kiểm tra giá thị trường."

---

### 2.3. Gửi cam kết (Commitment Email)

> **Trinh nói:**
>
> "Sau khi thấy thông tin ổn, staff nhấn nút **Gửi cam kết** (Send Commitment). Hệ thống sẽ:
> 1. Lấy thông tin KYC của chủ quỹ (CCCD, địa chỉ, nơi cấp, ngày cấp)
> 2. Gửi email đến chủ quỹ chứa link ký cam kết
> 3. Gửi notification trong app nhắc nhở ký cam kết"

**Demo:** Click nút "Gửi cam kết"

> **Trinh nói:**
>
> "Bản cam kết bao gồm thông tin cá nhân từ KYC (tên, CCCD, địa chỉ, nơi cấp), nội dung cam kết sử dụng tiền đúng mục đích, và yêu cầu chữ ký số. Đây là bằng chứng pháp lý theo **Luật Giao dịch điện tử 2023** (Luật số 20/2023/QH15)."

**📌 Code evidence:**
- [`CampaignCommitmentController.sendCommitmentEmail()`](campaign-service/src/main/java/com/trustfund/controller/CampaignCommitmentController.java:35) — staff gửi email
- [`EmailServiceClient.sendCommitmentRequestEmail()`](campaign-service/src/main/java/com/trustfund/client/EmailServiceClient.java:30) — gửi email với KYC data
- [`CampaignCommitment.java`](campaign-service/src/main/java/com/trustfund/model/CampaignCommitment.java:18) — entity lưu cam kết

---

## PHẦN 3: USER — VÀO MAIL & KÝ CAM KẾT (Trinh demo)

### 3.1. Chuyển về tài khoản User Hùng

> **Trinh nói:**
>
> "Bây giờ em chuyển lại tài khoản User Hùng để ký cam kết."

**Demo:**
- Đăng nhập lại user Hùng
- Hoặc mở mail

---

### 3.2. Vào mail xác thực cam kết

> **Trinh nói:**
>
> "Hùng nhận được email từ hệ thống. Email chứa:
> - Nội dung cam kết trách nhiệm
> - Thông tin cá nhân từ KYC (tên, CCCD, địa chỉ)
> - **Link ký cam kết**"

**Demo:**
- Mở email
- Click link ký cam kết → redirect đến trang ký

---

### 3.3. Ký cam kết (Commitment Signing)

> **Trinh nói:**
>
> "Trang ký cam kết hiển thị:
> - Thông tin cá nhân (auto-fill từ KYC — không sửa được)
> - Nội dung cam kết đầy đủ
> - **Ô vẽ chữ ký** (canvas)
>
> User đọc cam kết, vẽ chữ ký tay trên canvas, rồi nhấn **Ký cam kết**."

**Demo:**
- Kiểm tra thông tin pre-fill
- Vẽ chữ ký
- Nhấn "Ký cam kết"

> **Trinh nói:**
>
> "Khi ký, hệ thống lưu **CampaignCommitment** gồm:
> - `fullName`, `idNumber` (CCCD), `address`, `workplace`, `phoneNumber` — từ KYC
> - `content` — nội dung cam kết
> - `signatureUrl` — chữ ký base64
> - `ipAddress` — IP khi ký
> - `status = 'SIGNED'`
> - `createdAt` — thời điểm ký
>
> Tất cả thông tin này tạo thành **bằng chứng điện tử** đủ giá trị pháp lý trong tố tụng dân sự."

**📌 Code evidence:**
- [`CampaignCommitmentController.signCommitment()`](campaign-service/src/main/java/com/trustfund/controller/CampaignCommitmentController.java:115) — API ký cam kết
- [`CampaignCommitment.java`](campaign-service/src/main/java/com/trustfund/model/CampaignCommitment.java:18):
  ```java
  @Column(name = "full_name", nullable = false) private String fullName;
  @Column(name = "id_number", nullable = false) private String idNumber;
  @Column(name = "address", length = 1000) private String address;
  @Lob @Column(name = "signature_url", columnDefinition = "LONGTEXT") private String signatureUrl;
  @Column(name = "ip_address") private String ipAddress;
  @Builder.Default private String status = "SIGNED";
  ```

---

## PHẦN 4: STAFF — XÁC NHẬN CAM KẾT & DUYỆT (Trinh demo)

### 4.1. Chuyển về tài khoản Staff

> **Trinh nói:**
>
> "Bây giờ em quay lại tài khoản Staff. Sau khi user Hùng đã ký cam kết, staff tiến hành bước cuối."

---

### 4.2. Xác nhận cam kết đã ký

> **Trinh nói:**
>
> "Staff vào lại task chiến dịch. Hệ thống kiểm tra commitment đã được ký chưa qua API `GET /api/campaigns/commitments/check/{campaignId}` — trả về `true` nếu đã ký. Staff thấy trạng thái cam kết: **Đã ký** ✅."

**📌 Code evidence:**
- [`CampaignCommitmentController.isSigned()`](campaign-service/src/main/java/com/trustfund/controller/CampaignCommitmentController.java:161):
  ```java
  boolean signed = commitmentRepository.existsByCampaignIdAndStatus(campaignId, "SIGNED");
  ```

---

### 4.3. Duyệt chiến dịch → APPROVED

> **Trinh nói:**
>
> "Cuối cùng, staff nhấn **Duyệt** (Approve). Hệ thống thực hiện:
>
> 1. **Kiểm tra KYC:** Xác minh chủ quỹ đã KYC chưa — nếu chưa thì không cho duyệt.
> 2. **Cập nhật status:** Chiến dịch chuyển sang `APPROVED` — bắt đầu hoạt động và nhận quyên góp.
> 3. **Hoàn tất ApprovalTask:** Task chuyển sang `COMPLETED`.
> 4. **Gửi notification:** Thông báo cho chủ quỹ: 'Chiến dịch đã được duyệt!'
> 5. **Cộng Trust Score:** Chủ quỹ được cộng điểm uy tín `CAMPAIGN_APPROVED`."

**Demo:** Click nút "Duyệt" → thấy chiến dịch chuyển sang APPROVED

> **Trinh nói:**
>
> "Chiến dịch đã hoạt động! Từ lúc này, donor có thể vào chiến dịch, quét QR và quyên góp. Mọi giao dịch sẽ được Casso giám sát tự động."

**📌 Code evidence:**
- [`CampaignServiceImpl.reviewCampaign()`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:190):
  ```java
  // Kiểm tra KYC
  if (!verificationStatus.isKycVerified()) {
      throw "Cannot approve campaign. Owner's KYC is not verified.";
  }
  campaign.setStatus("APPROVED");
  // Gửi notification
  notificationServiceClient.sendNotification(...);
  // Cộng Trust Score
  trustScoreService.addScore(fundOwnerId, "CAMPAIGN_APPROVED", ...);
  ```

---

## TÓM TẮT LUỒNG 2 — BẢNG TỔNG HỢP

| # | Bước | Người thực hiện | Hành động | Kết quả |
|---|------|-----------------|-----------|---------|
| 1 | Nhập STK | USER (Hùng) | Nhập tài khoản ngân hàng tách bạch | Liên kết với Casso |
| 2 | Step 2 | USER (Hùng) | Nhập thông tin chiến dịch | Tiêu đề, mô tả, ảnh, danh mục |
| 3 | Step 3 | USER (Hùng) | Lập kế hoạch ngân sách | Milestones, hạng mục, vật phẩm |
| 4 | Step 4 | USER (Hùng) | Xác nhận tài khoản | Đối chiếu STK vs KYC |
| 5 | Step 5 | USER (Hùng) | Đồng ý điều khoản | Accept ToS |
| 6 | Step 6 | USER (Hùng) | Gửi chiến dịch | `PENDING_APPROVAL` + **Random assign staff** |
| 7 | Review | STAFF | Xem toàn bộ thông tin | Kiểm tra KYC, ngân sách, STK |
| 8 | Gửi cam kết | STAFF | Click "Gửi cam kết" | Email + Notification đến user |
| 9 | Ký cam kết | USER (Hùng) | Vào mail, ký commitment | `CampaignCommitment.status = SIGNED` |
| 10 | Duyệt | STAFF | Click "Duyệt" | `Campaign.status = APPROVED` ✅ |

---

## ĐIỂM NỔI BẬT CẦN NHẤN MẠNH

### 🎲 Random Assignment
- Code: `staffIds.get(random.nextInt(staffIds.size()))` — [`ApprovalTaskServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/ApprovalTaskServiceImpl.java:33)
- Chống thông đồng staff + phân bổ đều tải

### 🏦 Nghị định 31/2024 Điều 17.2
- Xác minh chủ tài khoản trùng KYC
- Tài khoản thanh toán tách bạch

### 📡 Casso Webhook
- Casso **chủ động gọi về server** khi có giao dịch mới trên tài khoản đã kết nối
- Giám sát 24/7, cả giao dịch vào (donate) lẫn giao dịch ra (chi tiêu)
- Pattern: Casso → Vercel Proxy → Pusher → Payment Service

### 📝 Commitment (Cam kết pháp lý)
- CCCD + chữ ký số + IP + timestamp
- Giá trị theo Luật Giao dịch điện tử 2023

### ✅ KYC Gate
- Không có KYC verified → không duyệt chiến dịch (`BR-21`, `BR-33`)

---

## CÂU HỎI CÓ THỂ BỊ HỎI (VÀ CÁCH TRẢ LỜI)

### Q: Tại sao random mà không cho admin chỉ định staff?

> "Hệ thống **có cả 2**: default là random để chống thông đồng, nhưng Admin có API `reassignTask()` để giao lại cho staff khác nếu cần. Random là mặc định để đảm bảo công bằng."

### Q: Nếu staff duyệt mà user chưa ký cam kết thì sao?

> "Frontend kiểm tra trạng thái cam kết qua `GET /api/campaigns/commitments/check/{id}`. Nếu chưa ký thì nút Duyệt bị disable. Backend cũng kiểm tra KYC verified trước khi duyệt."

### Q: Casso webhook bị lỗi thì sao?

> "Casso có cơ chế retry. Hệ thống có dedup bằng `tid` unique constraint — gọi lại không bị duplicate. Ngoài ra có `PaymentCleanupTask` dọn dẹp donation PENDING quá 10 phút."

### Q: Commitment có giá trị pháp lý không?

> "Theo Luật Giao dịch điện tử 2023 (Luật số 20/2023/QH15), chữ ký điện tử có giá trị pháp lý tương đương chữ ký tay nếu đáp ứng điều kiện định danh. Hệ thống lưu CCCD + KYC face + chữ ký + IP + timestamp — đủ làm bằng chứng điện tử trong tố tụng dân sự."

### Q: Tại sao tiền vào thẳng tài khoản chủ quỹ mà không qua escrow?

> "Để giữ tiền trung gian cần giấy phép hoạt động thanh toán trung gian theo Nghị định 101/2012/NĐ-CP. Thay vào đó, hệ thống dùng Casso giám sát tài khoản + enforcement tự động để đảm bảo minh bạch."

---

## ⚠️ NHẮC NHỞ KHI DEMO

1. **ĐỪNG** giải thích cảnh báo không trùng tên (chỉ trả lời nếu hội đồng hỏi)
2. **NHỚ** nhắc Nghị định 31/2024 Điều 17.2 khi nhập STK
3. **NHỚ** giải thích Casso là webhook chủ động gọi về
4. **NHỚ** nhấn mạnh tính năng random khi gửi chiến dịch
5. Chuẩn bị sẵn tài khoản **User Hùng** và **Staff** để switch nhanh
6. Chuẩn bị sẵn tab **email** (Gmail) để demo ký cam kết

---

*Kịch bản luồng 2 — Tạo chiến dịch & Duyệt*
*Cập nhật: 2026-05-07*
