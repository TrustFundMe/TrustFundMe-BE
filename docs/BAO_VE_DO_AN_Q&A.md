# TrustFundMe - TI LE TAI LIEU BAO VE DO AN
## Toan bo cau hoi & tra loi chi tiet cho he thong Quy Thien Nguyen TrustFundMe

> Tai lieu nay bao gom tat ca cau hoi co the bi hoi khi bao ve do an, kem tra loi chi tiet dua tren code thuc te.
> Moi phan duoc sap xep theo chu de: Kien truc, Nghiep vu, Bao mat, Database, Payment, KYC, AI, va cac tinh huong bad-case.

---

# MUC LUC

1. [KIEN TRUC HE THONG (Architecture)](#1-kien-truc-he-thong)
2. [AUTHENTICATION & AUTHORIZATION](#2-authentication--authorization)
3. [CAMPAIGN - NGHIEP VU QUY THIEN NGUYEN](#3-campaign---nghiep-vu-quy-thien-nguyen)
4. [THANH TOAN & DONG TIEN (Payment Flow)](#4-thanh-toan--dong-tien)
5. [KYC - XAC MINH DANH TINH](#5-kyc---xac-minh-danh-tinh)
6. [CHI TIEU & GIAI NGAN (Expenditure & Disbursement)](#6-chi-tieu--giai-ngan)
7. [TRUST SCORE - DIEM UY TIN](#7-trust-score---diem-uy-tin)
8. [FLAG & TO CAO](#8-flag--to-cao)
9. [FEED POST & CONG DONG](#9-feed-post--cong-dong)
10. [NOTIFICATION & REAL-TIME](#10-notification--real-time)
11. [CHAT SERVICE](#11-chat-service)
12. [MEDIA SERVICE](#12-media-service)
13. [AI INTEGRATION](#13-ai-integration)
14. [QUY CHUNG (General Fund)](#14-quy-chung-general-fund)
15. [BAO MAT & VULNERABILITY](#15-bao-mat--vulnerability)
16. [FRONTEND (DANBOX - NEXT.JS)](#16-frontend-danbox---nextjs)
17. [DEPLOYMENT & DEVOPS](#17-deployment--devops)
18. [DATABASE DESIGN](#18-database-design)
19. [TESTING](#19-testing)
20. [BAD CASE & TINH HUONG XAU](#20-bad-case--tinh-huong-xau)
21. [TAI SAO CHON CONG NGHE NAY](#21-tai-sao-chon-cong-nghe-nay)
22. [SO SANH VOI CAC HE THONG KHAC](#22-so-sanh-voi-cac-he-thong-khac)
23. [SCABILITY & PERFORMANCE](#23-scalability--performance)

---

# 1. KIEN TRUC HE THONG

## Q1.1: He thong cua ban duoc thiet ke theo kien truc gi? Tai sao chon kien truc do?

**Tra loi:** He thong TrustFundMe duoc xay dung theo kien truc **Microservices** voi **10 service rieng biet**:

| Service | Port | Chuc nang |
|---------|------|-----------|
| **API Gateway** | 8080 | Dinh tuyen, xac thuc JWT, CORS, load balancing |
| **Discovery Server** | 8761 | Service registry (Eureka) |
| **Identity Service** | 8081 | Dang ky, dang nhap, KYC, quan ly user, bank account |
| **Campaign Service** | 8082 | Chien dich, chi tieu, trust score, feed post, flag |
| **Media Service** | 8083 | Upload/quang ly file anh/video (Supabase Storage) |
| **Chat Service** | 8086 | Nhan tin, lich hen (WebSocket) |
| **Payment Service** | 8087 | Thanh toan, donate, Casso webhook |
| **Notification Service** | 8088 | Thong bao real-time (Pusher), email |
| **Feed Service** | (trong campaign-service) | Bai viet, binh luan, like |
| **Flag Service** | (trong campaign-service) | Bao cao vi pham |

**Tai sao chon Microservices thay vi Monolith?**
- **Tach biet nghiep vu:** Moi service phu trach 1 domain rieng (Single Responsibility). Payment xu ly tien tach biet voi Campaign xu ly chien dich.
- **Doc lap deploy:** Co the deploy rieng tung service ma khong anh huong cac service khac.
- **Scalability:** Co the scale rieng service chiu tai cao (Payment, Campaign) ma khong can scale toan bo he thong.
- **Cach ly loi (Fault Isolation):** Neu Payment bi loi, Campaign van hoat dong binh thuong.
- **Phu hop voi nhom:** Cac thanh vien co the phat trien song song tren cac service khac nhau.

## Q1.2: API Gateway hoat dong nhu the nao? Tai sao can Gateway?

**Tra loi:** API Gateway su dung **Spring Cloud Gateway** (Reactive/WebFlux), la diem vao duy nhat cua he thong:

1. **Routing:** Dinh tuyen request den dung service dua tren URL path. Vi du `/api/campaigns/**` -> campaign-service, `/api/payments/**` -> payment-service.
2. **JWT Authentication:** Global filter kiem tra JWT token truoc khi cho request di qua. Token hop le thi inject header `X-User-Id`, `X-User-Email`, `X-User-Role` vao request.
3. **Load Balancing:** Su dung prefix `lb://` ket hop voi Eureka de tu dong load balance giua cac instance cua cung 1 service.
4. **CORS:** Cau hinh global CORS cho phep frontend (localhost:3000) goi API.
5. **Retry:** Tu dong retry 3 lan khi service tra ve 503 (Service Unavailable).
6. **Public endpoints:** Mot so endpoint khong can token (GET campaigns, tao donation, webhook PayOS...).

**Tai sao can Gateway ma khong goi truc tiep tung service?**
- Frontend chi can biet 1 URL (port 8080), khong can biet tung service chay port nao.
- Tap trung logic xac thuc tai 1 diem, tranh viec moi service phai tu verify JWT.
- De dang them rate limiting, logging, monitoring tai gateway.

## Q1.3: Service Discovery (Eureka) hoat dong ra sao? Tai sao khong dung IP co dinh?

**Tra loi:** He thong su dung **Netflix Eureka** lam Service Registry:
- Moi service khi start se tu **dang ky** voi Eureka (ten service, IP, port).
- Gateway va cac service khi can goi service khac se **tim kiem** (lookup) tren Eureka thay vi hardcode IP.
- Eureka heartbeat moi 5s (`lease-renewal-interval-in-seconds=5`), neu service khong gui heartbeat trong 15s thi bi xoa khoi registry.
- Gateway fetch registry moi 5s (`registry-fetch-interval-seconds=5`) de cap nhat nhanh.

**Tai sao khong dung IP co dinh?**
- Khi deploy len cloud (Docker, K8s), IP cua container thay doi lien tuc.
- Khi scale them instance, IP moi khong ai biet truoc.
- Eureka cho phep tu dong phat hien service moi va xoa service chet.

## Q1.4: Cac service giao tiep voi nhau nhu the nao?

**Tra loi:** Cac service giao tiep qua **REST API (HTTP)** su dung `RestTemplate`:
- Campaign Service goi Identity Service de lay thong tin user, kiem tra KYC.
- Payment Service goi Campaign Service de cap nhat balance.
- Campaign Service goi Notification Service de gui thong bao.
- Payment Service goi Identity Service de lay thong tin bank account.

Moi service co cac **Client class** (vi du `IdentityServiceClient`, `PaymentServiceClient`, `NotificationServiceClient`) de wrap cac HTTP call. Cac client nay xu ly exception va fallback khi service khong kha dung.

**Han che va huong cai thien:**
- Hien tai dung **synchronous REST call** (dong bo). Neu 1 service cham, service goi cung bi cham (coupling).
- Co the cai thien bang **Message Queue** (RabbitMQ/Kafka) cho cac event khong can response ngay (send notification, update trust score).
- Hien tai chua co **Circuit Breaker** (Resilience4j), co the them de tranh cascading failure.

## Q1.5: He thong co su dung Design Pattern nao khong?

**Tra loi:** Co, he thong ap dung nhieu pattern:

1. **Service Layer Pattern:** Tach Controller -> Service Interface -> Service Implementation -> Repository. Vi du: `CampaignService` (interface) -> `CampaignServiceImpl` (implementation).
2. **Repository Pattern:** Su dung Spring Data JPA Repository de truy xuat du lieu.
3. **Builder Pattern:** Tat ca entity va DTO deu dung `@Builder` (Lombok) de tao object.
4. **DTO Pattern:** Request/Response DTO tach biet voi entity. Vi du: `CreateCampaignRequest`, `CampaignResponse`.
5. **Gateway Pattern:** API Gateway la implementation cua Gateway pattern trong Microservices.
6. **Observer Pattern (pub/sub):** Casso Webhook lang nghe su kien tu ngan hang, Pusher push event real-time.
7. **Soft Delete Pattern:** Campaign dung `status = "DELETED"` thay vi xoa thuc su.
8. **Seeder Pattern:** `GeneralFundSeeder`, `SystemConfigSeeder` tu dong tao du lieu mac dinh khi ung dung khoi dong.

---

# 2. AUTHENTICATION & AUTHORIZATION

## Q2.1: He thong xac thuc nguoi dung nhu the nao?

**Tra loi:** He thong su dung **JWT (JSON Web Token)** voi HMAC-SHA:

1. **Dang ky (`/api/auth/register`):** User gui email + password + fullName. Password duoc hash bang `BCryptPasswordEncoder`. He thong tra ve accessToken + refreshToken.
2. **Dang nhap (`/api/auth/login`):** User gui email + password. He thong verify password voi hash trong DB, neu dung thi generate JWT.
3. **Google OAuth2 (`/api/auth/google`):** User gui Google ID Token. He thong dung `GoogleIdTokenVerifier` de verify voi Google, neu user chua co thi tu dong tao tai khoan.
4. **JWT Token:** Chua `sub` (userId), `email`, `role`. Token duoc ky bang HMAC-SHA voi `JWT_SECRET` (64+ ky tu).
5. **Refresh Token:** Khi access token het han, frontend gui refresh token de lay cap token moi ma khong can dang nhap lai.

## Q2.2: Phan quyen (Authorization) hoat dong nhu the nao?

**Tra loi:** He thong co **5 role**:

| Role | Quyen |
|------|-------|
| `USER` | Xem chien dich, donate, viet bai, binh luan, flag |
| `FUND_OWNER` | Tat ca cua USER + tao chien dich, quan ly chi tieu |
| `FUND_DONOR` | (Du phong, hien tai tuong duong USER) |
| `STAFF` | Duyet chien dich, KYC, chi tieu, flag, quan ly feed post |
| `ADMIN` | Toan quyen, quan ly user, ban, cau hinh he thong |

**Co che phan quyen:**
1. **Gateway level:** JWT filter inject `X-User-Role` header vao moi request.
2. **Service level:** Dung `@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")` tren cac endpoint can bao ve. Vi du: chi STAFF/ADMIN moi duoc duyet chien dich (`PUT /api/campaigns/{id}/review`).
3. **Spring Security:** Moi service co `SecurityConfig` rieng, cau hinh endpoint nao public, endpoint nao can role cu the.
4. **Frontend:** Component `<RequireRole>` kiem tra role truoc khi render UI admin/staff.

## Q2.3: Lam sao de chong gia mao token?

**Tra loi:**
- JWT duoc ky bang **HMAC-SHA** voi secret key dai 64 ky tu, duoc luu trong bien moi truong (`JWT_SECRET`), khong hardcode trong code.
- Moi request, Gateway verify chu ky cua token. Neu token bi sua doi (payload hoac signature), `Jwts.parser().verifyWith(key)` se throw exception va tra ve 401.
- Token co thoi gian het han (`expiration`), sau do token khong con hop le.
- **Refresh token** cung duoc ky cung secret, co thoi gian song dai hon access token.

## Q2.4: Neu ai do danh cap JWT token thi sao?

**Tra loi:** Day la rui ro chung cua JWT-based authentication. Bien phap giam thieu:
- Token co **thoi gian het han ngan** (configurable qua `jwt.expiration`).
- **Refresh Token Rotation:** Moi lan refresh thi cap cu bi voidify, cap moi duoc tao.
- **HTTPS (SSL):** Trong production, tat ca traffic qua HTTPS de chong man-in-the-middle.
- **HttpOnly Cookie:** Frontend co the luu token trong HttpOnly cookie de chong XSS (tuong lai cai thien).
- **Logout:** Frontend xoa token khoi localStorage/cookie.

**Han che hien tai:** Chua co blacklist/revoke token mechanism tren server. Neu can cai thien, co the dung Redis de luu blacklist cac token da bi revoke.

## Q2.5: OTP va Reset Password hoat dong ra sao?

**Tra loi:**
1. User gui email, he thong tao **OTP 6 so** bang `SecureRandom` (100000-999999).
2. OTP duoc luu vao bang `otp_tokens` voi `expiresAt` (mac dinh 10 phut).
3. He thong gui OTP qua **email** (Gmail SMTP).
4. User gui OTP de verify, he thong check: OTP dung + chua het han + chua used.
5. Neu verify thanh cong, he thong tao **Password Reset Token** (JWT voi type="password_reset", het han 30 phut).
6. User gui token + newPassword de doi mat khau.

**Anti-abuse:** OTP chi dung 1 lan (`markAsUsed`). Neu ai do brute-force OTP, he thong van bao ve vi chi co 900000 gia tri va het han sau 10 phut.

## Q2.6: Tai sao cho phep user bi ban/deactivated van dang nhap duoc?

**Tra loi:** Day la **thiet ke co y**. Code comment ghi ro:
```
// Allow login even if account is deactivated - the frontend will handle showing the restricted view
```
- Muc dich: Khi user bi ban, ho van co the dang nhap de **xem ly do bi ban** (`banReason`) va lien he admin.
- Frontend se kiem tra `isActive` va hien thi **restricted view** thay vi dashboard binh thuong (component `BannedAccountWrapper`).
- Dieu nay nhan van hon viec hoan toan khoa tai khoan ma khong giai thich.

---

# 3. CAMPAIGN - NGHIEP VU QUY THIEN NGUYEN

## Q3.1: Quy trinh tao va duyet chien dich nhu the nao?

**Tra loi:** Quy trinh gom **5 buoc**:

```
USER tao chien dich -> PENDING_APPROVAL -> Staff duyet -> APPROVED (hoac REJECTED)
                                                        -> Chien dich hoat dong
```

**Chi tiet:**
1. User dien thong tin (title, description, category, coverImage, startDate, endDate).
2. He thong validate: category ton tai, fundOwnerId ton tai trong Identity Service.
3. Chien dich duoc tao voi `status = "PENDING_APPROVAL"`.
4. He thong tu dong **nang cap role** user len `FUND_OWNER` (goi `identityServiceClient.upgradeUserRole()`).
5. He thong tao **ApprovalTask** va **random assign cho 1 staff** de duyet.
6. Staff xem task tren dashboard, review chien dich:
   - **Neu APPROVED:** Kiem tra user da KYC chua (`kycVerified`). Neu chua KYC thi khong cho approve. Gui notification "Chien dich da duoc duyet".
   - **Neu REJECTED:** Bat buoc nhap `rejectionReason`. Gui notification "Chien dich bi tu choi".
7. **Trust Score:** Duyet thanh cong +diem, bi tu choi -diem.

## Q3.2: He thong co bao nhieu loai chien dich? Khac nhau nhu the nao?

**Tra loi:** Co **3 loai chien dich**:

| Loai | Giai thich | Flow chi tieu |
|------|-----------|--------------|
| **ITEMIZED** | Quyen gop vat pham cu the (gao, sua, sach...). Donor chon vat pham va so luong. | Balance = tong so tien donate. Fund Owner yeu cau rut tien de mua hang. |
| **AUTHORIZED** | Quyen gop tien truc tiep cho muc dich cu the. | Staff duyet chi tieu -> Tu dong chuyen sang WITHDRAWAL_REQUESTED -> Staff giai ngan. |
| **GENERAL_FUND** | Quy chung cua he thong, khong hien thi trong danh sach. Chi admin thao tac. | Admin dieu chuyen tien tu quy chung den cac chien dich. |

**Su khac biet quan trong:**
- **ITEMIZED:** Donor thay tung vat pham (ten, gia, so luong), donate theo vat pham. `quantityLeft` giam khi co nguoi donate.
- **AUTHORIZED:** Donor donate tien truc tiep, khong chon vat pham. Fund Owner len ke hoach chi tieu, staff duyet roi giai ngan.

## Q3.3: Campaign co nhung trang thai (status) nao?

**Tra loi:**

| Status | Mo ta |
|--------|-------|
| `PENDING_APPROVAL` | Vua tao, cho staff duyet |
| `APPROVED` | Da duoc duyet, dang hoat dong, nhan donate |
| `REJECTED` | Bi staff tu choi |
| `PAUSED` | Bi tam dung boi staff/admin |
| `CLOSED` | Da dong, so du duoc thu hoi ve Quy chung |
| `DISABLED` | Bi vo hieu hoa do vi pham (overdue evidence) |
| `DELETED` | Xoa mem (soft delete) |

## Q3.4: Khi dong chien dich (CLOSED), so du xu ly nhu the nao?

**Tra loi:** Khi staff/admin dong chien dich (`closeCampaign`):
1. Kiem tra `balance > 0`.
2. Neu con du, tao giao dich **RECOVERY** tu campaign ve General Fund (ID=1).
3. Set `balance = 0`.
4. Set `status = "CLOSED"`.

Dieu nay dam bao **khong co tien bi mat** khi dong chien dich. Toan bo so du duoc chuyen ve quy chung de tai su dung.

## Q3.5: Fund Owner co the tu y thay doi thong tin chien dich khong?

**Tra loi:** **Co gioi han:**
- Chi duoc sua khi `status != "DISABLED"` va `status != "APPROVED"`.
- Nghia la: chi sua duoc khi dang `PENDING_APPROVAL` hoac `REJECTED` (de sua va nop lai).
- Khi da `APPROVED` (dang hoat dong), **khong** duoc sua de dam bao thong tin minh bach voi donor.

## Q3.6: CampaignFollower la gi? Co tac dung gi?

**Tra loi:** Nguoi dung co the **follow** mot chien dich:
- Khi chien dich bi flag (to cao), he thong gui **notification den tat ca follower** de canh bao.
- Follower co the theo doi tien do, bai viet cua chien dich.
- Thong ke `followerCount` cung la chi so do luong muc do quan tam cua cong dong.

---

# 4. THANH TOAN & DONG TIEN

## Q4.1: He thong thanh toan hoat dong nhu the nao?

**Tra loi:** TrustFundMe tich hop **VietQR** ket hop **Casso Webhook** de xu ly thanh toan:

```
Donor tao donation -> He thong tao QR Code (VietQR) -> Donor quet QR chuyen khoan
    -> Ngan hang gui thong bao ve Casso -> Casso gui webhook den he thong
    -> He thong match transaction voi donation -> Cap nhat balance chien dich
```

**Chi tiet tung buoc:**
1. **Tao donation:** Donor chon chien dich, nhap so tien (va tip tuy chon). He thong tao ban ghi `Donation` voi `status = "PENDING"`.
2. **Tao QR Code:** He thong goi Identity Service lay thong tin bank account cua chien dich, tao URL VietQR voi format: `TF {donationId}` trong noi dung chuyen khoan.
3. **Donor thanh toan:** Quet QR hoac chuyen khoan thu cong voi noi dung `TF {donationId}`.
4. **Casso Webhook:** Khi ngan hang ghi nhan giao dich, Casso gui webhook den `/api/payments/webhook`. He thong:
   - Luu `CassoTransaction` (chong trung `tid`).
   - Tim `Donation` bang regex `TF {id}` trong description.
   - Neu match: cap nhat `status = "PAID"`, dong bo balance.
   - Neu khong match: tao `Anonymous Donation` (nguoi chuyen truc tiep vao tai khoan quy).
5. **Cap nhat balance:** Goi Campaign Service API `PUT /api/campaigns/{id}/update-balance`.

## Q4.2: Tai sao chon VietQR + Casso thay vi tich hop PayOS truc tiep?

**Tra loi:**
- **VietQR** la chuan quoc gia, ho tro **moi ngan hang** tai Viet Nam, mien phi tao QR.
- **Casso** la dich vu **theo doi giao dich ngan hang tu dong**, mien phi cho luong nho.
- **PayOS** duoc tich hop de **tao payment link** nhung thuc te he thong chuyen sang dung **VietQR + Casso** vi:
  - PayOS tinh phi giao dich, Casso + VietQR mien phi cho quy thien nguyen.
  - VietQR ho tro truc tiep chuyen khoan vao tai khoan ngan hang cua **chinh chu quy** thay vi qua trung gian.
  - Phu hop voi muc dich minh bach: tien vao thang tai khoan duoc lien ket voi chien dich.

## Q4.3: Lam sao dam bao khong bi trung (duplicate payment)?

**Tra loi:** He thong co nhieu lop chong trung:
1. **Casso Transaction ID (tid):** Truoc khi xu ly, kiem tra `cassoTransactionRepository.existsByTid(tid)`. Neu da xu ly thi skip.
2. **Database Unique Constraint:** Field `tid` trong `CassoTransaction` co unique constraint. Neu 2 thread ghi cung luc, `DataIntegrityViolationException` se xay ra va thread sau bi skip.
3. **Balance Sync Flag:** Moi `Donation` co flag `isBalanceSynchronized`. Chi cap nhat balance 1 lan, cac lan goi sau se bi skip (`Balance already synchronized`).

## Q4.4: Donor co the donate ma khong can dang nhap khong?

**Tra loi:** **Co.** He thong ho tro **Guest Donation**:
- Endpoint `POST /api/payments/create` duoc de la **public** trong Gateway filter.
- `donorId` co the null. Khi do `isAnonymous = true` (mac dinh).
- Guest chi can nhap so tien, quet QR, chuyen khoan. Khong can tai khoan.

## Q4.5: Tip (tien ung ho he thong) xu ly nhu the nao?

**Tra loi:** Donation co 3 truong tien:
- `donationAmount`: So tien ung ho cho chien dich.
- `tipAmount`: So tien ung ho cho he thong (tuy chon, mac dinh 0).
- `totalAmount = donationAmount + tipAmount`.

Chi co `donationAmount` duoc cong vao balance cua chien dich. `tipAmount` giup he thong duy tri hoat dong ma khong thu phi bat buoc.

## Q4.6: Voi chien dich ITEMIZED, khi donor chon vat pham, so luong duoc xu ly nhu the nao?

**Tra loi:**
1. Truoc khi tao donation, frontend goi `GET /api/payments/check-item-limit?expenditureItemId=X&quantity=Y` de kiem tra `quantityLeft`.
2. Khi tao donation, he thong **tru ngay `quantityLeft`** (immediate deduction) de chong tran (2 nguoi donate cung luc).
3. Neu donation bi FAILED/CANCELLED, he thong **hoan lai so luong** (rollback).
4. Logic:
   - `processQuantityUpdate()`: Goi Campaign Service `PUT /api/expenditures/items/{id}/update-quantity?amount=N` de tru `quantityLeft`.
   - `processQuantityRollback()`: Gui `amount = -N` de cong lai.

## Q4.7: Casso Webhook bi replay attack thi sao?

**Tra loi:**
- **Dedup bang tid:** Moi transaction co `tid` duy nhat tu Casso. He thong kiem tra `existsByTid(tid)` truoc khi xu ly.
- **Webhook Key Verification:** He thong verify `Secure-Token` header voi `webhookKey` da luu trong Identity Service. Neu khong khop thi reject.
- **HMAC Signature:** Ho tro verify Casso V2 signature format `t=timestamp,v1=hmac_sha256`.

**Luu y:** Code hien tai co bypass verification cho testing (`REMOVED BYPASS FOR TESTING`). Trong production phai bat lai verification.

## Q4.8: Cleanup Donation: neu donor tao donation ma khong thanh toan thi sao?

**Tra loi:** Co `PaymentCleanupTask` (Scheduled Task) chay dinh ky:
- Tim cac `Donation` co `status = "PENDING"` va da tao qua X phut.
- Tu dong chuyen sang `FAILED`.
- Rollback `quantityLeft` (neu la ITEMIZED donation).

---

# 5. KYC - XAC MINH DANH TINH

## Q5.1: Quy trinh KYC nhu the nao?

**Tra loi:** KYC (Know Your Customer) gom **4 buoc**:

```
User nop ho so -> PENDING -> Staff duyet -> APPROVED (hoac REJECTED)
```

1. **Nop ho so** (`POST /api/kyc/submit`): User gui:
   - Loai giay to (`idType`): CCCD, ho chieu...
   - So dinh danh (`idNumber`): So CCCD.
   - Ngay cap, ngay het han, noi cap.
   - **Anh mat truoc** va **anh mat sau** CCCD.
   - **Anh selfie** (chup truc tiep).
   - **Face Descriptor** (128-dim vector tu face-api.js).
   - **Liveness Metadata** (du lieu chung minh nguoi that, khong phai anh chup tu man hinh).
   - **Face Mesh Sample** (diem moc 3D khuon mat).
2. **AI OCR:** Frontend su dung AI de doc thong tin tu anh CCCD (ten, ngay sinh, dia chi) va tu dong dien vao form.
3. **Face Verification:** Frontend dung face-api.js de:
   - So sanh khuon mat trong selfie voi anh tren CCCD.
   - Thuc hien liveness detection (quay dau trai/phai, nham mat).
4. **Staff duyet:** Staff xem ho so KYC, so sanh thong tin, duyet hoac tu choi.

## Q5.2: He thong chong gia mao KYC nhu the nao?

**Tra loi:** Nhieu tang bao ve:

1. **Chong trung CCCD:** `userKYCRepository.findFirstByIdNumber(idNumber)` kiem tra CCCD da duoc dang ky boi nguoi khac chua. Neu co -> tu choi.
2. **Face Descriptor (128-dim vector):** Luu face embedding de co the so sanh 2 khuon mat.
3. **Liveness Detection:** `livenessMetadata` ghi nhan cac buoc verify (quay dau, nham mat, thoi gian) chung minh nguoi that ngoi truoc camera.
4. **Face Mesh Sample:** 3D face landmarks giup xac nhan do sau khuon mat (chong anh 2D).
5. **Staff Manual Review:** Du AI phan tich, van can con nguoi duyet de dam bao.

## Q5.3: Tai sao KYC la bat buoc truoc khi tao chien dich?

**Tra loi:**
- Trong `reviewCampaign()`: Truoc khi duyet APPROVED, he thong kiem tra `kycVerified`:
  ```
  if (!verificationStatus.isKycVerified()) {
      throw "Cannot approve campaign. Owner's KYC is not verified.";
  }
  ```
- Dam bao **moi chu quy deu da duoc xac minh danh tinh**, chong lua dao, gia mao.
- Day la yeu cau phap ly voi to chuc tiep nhan quy thien nguyen tai Viet Nam.

## Q5.4: Neu KYC bi tu choi, user lam gi?

**Tra loi:** User co the **nop lai** (`PUT /api/kyc/resubmit`):
- Chi nop lai duoc khi `status != APPROVED` (da duyet thi khong sua duoc).
- Kiem tra CCCD moi (neu doi so CCCD) co trung khong.
- Reset `status = PENDING`, xoa `rejectionReason`.

## Q5.5: Du lieu KYC co bi ma hoa khong?

**Tra loi:** He thong co `EncryptionUtils.java` trong Identity Service. Tuy nhien:
- Hien tai du lieu KYC (so CCCD, anh) **chua duoc encrypt** tat ca truong.
- Anh KYC luu tren **Supabase Storage** (cloud).
- De tang bao mat, nen encrypt cac truong nhay cam nhu `idNumber` truoc khi luu DB.

---

# 6. CHI TIEU & GIAI NGAN (Expenditure & Disbursement)

## Q6.1: Quy trinh chi tieu va giai ngan hoat dong nhu the nao?

**Tra loi:** Day la **co che minh bach cot loi** cua TrustFundMe:

### Voi chien dich ITEMIZED:
```
Fund Owner tao Expenditure (ke hoach chi) -> PENDING_REVIEW -> Staff duyet -> APPROVED
-> Fund Owner yeu cau rut tien -> WITHDRAWAL_REQUESTED -> Staff giai ngan (DISBURSED)
-> Fund Owner mua hang va nop minh chung (evidence)
```

### Voi chien dich AUTHORIZED:
```
Fund Owner tao Expenditure -> PENDING_REVIEW -> Staff duyet -> Tu dong WITHDRAWAL_REQUESTED
-> Staff giai ngan (DISBURSED) -> Fund Owner nop minh chung
```

**Chi tiet:**
1. **Tao Expenditure:** Fund Owner len ke hoach chi tieu voi cac hang muc (categories) va vat pham (items), moi item co `expectedPrice`, `expectedQuantity`.
2. **Staff duyet:** Kiem tra ke hoach hop ly, so tien phu hop.
3. **Yeu cau rut tien:** Fund Owner gui yeu cau rut tien kem thoi han nop minh chung.
4. **Giai ngan:** Staff xac nhan chuyen tien, upload bang chung chuyen khoan (`proofUrl`).
5. **Nop minh chung:** Fund Owner nop hoa don, anh hang hoa de chung minh da chi dung muc dich.

## Q6.2: Lam sao dam bao Fund Owner khong chi tien sai muc dich?

**Tra loi:** Nhieu lop kiem soat:

1. **Ke hoach chi tieu chi tiet (Expenditure Items):** Moi muc chi phai ghi ro ten hang, gia du kien, so luong, don vi, dia diem mua. Staff kiem tra truoc khi duyet.
2. **AI Audit (Perplexity):** He thong tich hop **Perplexity AI** de kiem tra gia thi truong cua cac mat hang (`auditExpenditure`). Neu gia khai cao hon thi truong, AI se canh bao.
3. **Minh chung chi tieu (Evidence):** Sau khi chi tien, Fund Owner **bat buoc** nop hoa don/anh chung minh. He thong tao `ExpenditureEvidence` voi deadline.
4. **Thoi han nop minh chung:** Configurable qua `EXPENDITURE_EVIDENCE_DEADLINE_HOURS` (mac dinh 48h). Qua han thi bi phat.
5. **Enforcement Scheduler:** Cron job chay moi phut, kiem tra evidence qua han:
   - **Dong chien dich** (`closeCampaign`).
   - **Tru diem uy tin** (`OVERDUE_EVIDENCE`).
   - **Gui thong bao** canh bao vi pham.
6. **Ghi nhan tu dong tu ngan hang:** Khi Casso phat hien giao dich **chi** (amount < 0) tu tai khoan chien dich, he thong tu dong tao **yeu cau nop minh chung** (`createEvidenceRequirement`).

## Q6.3: "Variance" la gi? Y nghia?

**Tra loi:** `variance = totalReceivedAmount - totalAmount` (thuc chi)
- **Variance > 0:** Con du tien (chi it hon du kien).
- **Variance < 0:** Chi vuot muc (chi nhieu hon du kien).
- **Variance = 0:** Chi dung ke hoach.

He thong theo doi variance de phat hien bat thuong. Neu Fund Owner chi nhieu hon ke hoach, he thong canh bao.

## Q6.4: Refund (hoan tien) xu ly nhu the nao?

**Tra loi:** Khi chi tieu it hon so tien nhan duoc:
1. Fund Owner gui yeu cau hoan tien (`createRefund`) voi so tien va bang chung chuyen khoan.
2. He thong tao `ExpenditureTransaction` voi `type = "REFUND"`, `status = "COMPLETED"`.
3. **Cong so tien hoan lai vao balance** cua chien dich (`campaignService.updateBalance(campaign.getId(), amount)`).
4. Tien hoan duoc ghi nhan de dam bao minh bach.

## Q6.5: ExpenditureCatology (Hang muc) de lam gi?

**Tra loi:** Moi expenditure co the chia thanh nhieu **hang muc** (categories), moi hang muc co nhieu **items**:
```
Expenditure
  ├── Hang muc "Luong thuc"
  │     ├── Gao 5kg - 150,000 x 100
  │     └── Mi tom - 5,000 x 200
  ├── Hang muc "Do dung hoc tap"
  │     ├── Vo - 10,000 x 50
  │     └── But - 5,000 x 50
```
Moi hang muc co `expectedAmount`, `actualAmount` de theo doi chi tiet.

---

# 7. TRUST SCORE - DIEM UY TIN

## Q7.1: Trust Score la gi? Hoat dong nhu the nao?

**Tra loi:** Trust Score la **he thong cham diem uy tin** cho Fund Owner, giup cong dong danh gia muc do dang tin cay.

**Co che:**
- Moi hanh vi tot/xau duoc **cong/tru diem** theo cau hinh (`TrustScoreConfig`).
- Diem duoc luu trong `User.trustScore` (Identity Service) va log chi tiet trong `TrustScoreLog` (Campaign Service).

**Cac quy tac cham diem:**

| Rule Key | Mo ta | Diem |
|----------|-------|------|
| `CAMPAIGN_APPROVED` | Chien dich duoc duyet | +diem |
| `CAMPAIGN_REJECTED` | Chien dich bi tu choi | -diem |
| `ON_TIME_SUBMIT` | Nop minh chung dung han | +diem |
| `LATE_SUBMIT` | Nop minh chung muon | -diem |
| `OVERDUE_EVIDENCE` | Qua han nop minh chung | -diem (nang) |
| `DAILY_POST` | Viet bai hang ngay | +diem |

## Q7.2: Admin co the tuy chinh diem khong?

**Tra loi:** Co, qua API `PUT /api/trust-score/config/{ruleKey}`:
- Thay doi so diem cho moi rule.
- Bat/tat rule (`isActive`).
- Sua ten va mo ta rule.
- He thong dung **cache** (`ConcurrentHashMap`) de tang performance, tu dong invalidate khi cap nhat.

## Q7.3: Chong trung Trust Score nhu the nao?

**Tra loi:** He thong co **duplicate check** truoc khi cong diem:
- `DAILY_POST`: Moi user chi duoc cong 1 lan/ngay. Kiem tra bang `existsByUserIdAndRuleKeyAndCreatedAtAfter(userId, ruleKey, startOfDay)`.
- Cac rule khac: Kiem tra theo `referenceId` (campaignId/expenditureId). Neu da cham diem cho reference nay thi skip.

## Q7.4: Trust Score co Leaderboard khong?

**Tra loi:** Co. API `GET /api/trust-score/leaderboard` tra ve danh sach user xep theo diem cao nhat. Frontend hien thi ranking, avatar, ten.

---

# 8. FLAG & TO CAO

## Q8.1: Co che to cao (Flag) hoat dong nhu the nao?

**Tra loi:**
1. User gui report (`POST /api/flags`) voi `campaignId` hoac `postId` va `reason`.
2. **Chong trung:** Moi user chi duoc flag 1 lan cho moi campaign/post. Neu da flag thi throw `IllegalStateException`.
3. He thong tao `ApprovalTask` va assign cho staff.
4. **Gui notification cho follower:** Neu flag campaign, tat ca nguoi follow chien dich nhan duoc canh bao.
5. Staff review flag, set status `RESOLVED` hoac giu `PENDING`.
6. **Gui notification cho nguoi to cao:** Thong bao ket qua xu ly.

## Q8.2: Khi chien dich bi to cao nhieu lan, he thong xu ly nhu the nao?

**Tra loi:** Hien tai he thong **khong tu dong dong** chien dich khi dat so luong flag nhat dinh. Staff can **thu cong** review va quyet dinh:
- Pause chien dich (`PUT /api/campaigns/{id}/pause`).
- Dong chien dich (`PUT /api/campaigns/{id}/close`).

**Goi y cai thien:** Co the them threshold tu dong (vi du: 10+ flag -> tu dong pause de review).

---

# 9. FEED POST & CONG DONG

## Q9.1: Feed Post la gi?

**Tra loi:** He thong co tinh nang **dien dan/blog** de:
- Fund Owner cap nhat tien do chien dich.
- Cong dong chia se y kien, kinh nghiem.

**Tinh nang:**
- CRUD bai viet voi tinh trang (draft, published, archived).
- **Like** va **Comment** voi kiem soat trung lap.
- **Revision History:** Moi lan sua bai, he thong luu **phien ban cu** de audit.
- **Pin/Lock:** Admin co the ghim bai viet hoac khoa binh luan.
- **View Count** va **User Post Seen** de theo doi luot xem.

## Q9.2: Tai sao can Feed Post trong he thong quy thien nguyen?

**Tra loi:**
- **Minh bach:** Fund Owner dang bai cap nhat tien do, anh chung minh viec su dung tien.
- **Cong dong:** Donor va nguoi theo doi co the binh luan, hoi dap truc tiep.
- **Trust:** Hoat dong thuong xuyen tren Feed tang Trust Score (`DAILY_POST`).
- **Gia tri thuc te:** Giong cach GoFundMe, Kitabisa.com co tinh nang "Updates" de chu quy cap nhat.

---

# 10. NOTIFICATION & REAL-TIME

## Q10.1: He thong thong bao hoat dong nhu the nao?

**Tra loi:** Notification Service xu ly 2 kenh:

1. **Real-time (Pusher):** Push notification tuc thoi den frontend qua WebSocket (Pusher Channels).
2. **Email (Gmail SMTP):** Gui email cho cac su kien quan trong (KYC, OTP, commitment...).

**Cac loai notification:**
- `CAMPAIGN_APPROVED` / `CAMPAIGN_REJECTED`: Ket qua duyet chien dich.
- `EXPENDITURE_APPROVED` / `EXPENDITURE_REJECTED` / `EXPENDITURE_DISBURSED`: Ket qua chi tieu.
- `EVIDENCE_APPROVED` / `EVIDENCE_REJECTED`: Ket qua duyet minh chung.
- `KYC_APPROVED` / `KYC_REJECTED`: Ket qua KYC.
- `CAMPAIGN_FLAGGED`: Chien dich bi to cao.
- `FLAG_REVIEWED`: Ket qua xu ly to cao.
- `CAMPAIGN_LOCKED_OVERDUE`: Quy bi dong do vi pham.
- `EXPENDITURE_EVIDENCE_REQUIRED`: Yeu cau nop minh chung (tu dong tu Casso).

## Q10.2: Frontend nhan notification nhu the nao?

**Tra loi:** Frontend (danbox) co component `NotificationBell` va `WalletDropdown`:
- Dang ky kenh Pusher theo `userId`.
- Khi co notification moi, hien thi badge va danh sach notification.
- Click vao notification de di den trang tuong ung.

---

# 11. CHAT SERVICE

## Q11.1: Chat Service cung cap tinh nang gi?

**Tra loi:**
- **Nhan tin truc tiep** giua user va staff/fund owner.
- **Chat theo chien dich:** Moi chien dich co phong chat rieng (`/api/chat/conversations/campaign/{campaignId}`).
- **WebSocket (SockJS + STOMP):** Real-time messaging qua WebSocket.
- **Lich hen (Appointments):** Staff co the dat lich hen voi fund owner de review chien dich truc tiep.

## Q11.2: Tai sao can Chat trong he thong quy thien nguyen?

**Tra loi:**
- Staff can lien lac voi Fund Owner de hoi them thong tin khi duyet chien dich/chi tieu.
- Donor co the hoi truc tiep Fund Owner ve tien do.
- Lich hen giup staff to chuc review truc tiep (video call, gap mat) cho cac chien dich lon.

---

# 12. MEDIA SERVICE

## Q12.1: Media Service xu ly file nhu the nao?

**Tra loi:** Media Service quan ly upload/download file:
- **Luu tru:** Supabase Storage (Object Storage, tuong tu AWS S3).
- **Ho tro:** Anh (JPEG, PNG, WebP), Video, PDF.
- **Gioi han:** Max file size 50MB (cau hinh tai Gateway).
- **API:** `POST /api/media/upload`, `GET /api/media/{id}`, `DELETE /api/media/{id}`.
- **Lien ket:** File duoc lien ket voi campaign (coverImage), KYC (idImageFront/Back, selfieImage), feed post, evidence...

---

# 13. AI INTEGRATION

## Q13.1: He thong tich hop AI nhu the nao?

**Tra loi:** TrustFundMe tich hop **AI Service** (tach rieng, chay tren port 7000) cho cac chuc nang:

| Endpoint | Chuc nang |
|----------|----------|
| `/api/generate-description` | AI tao mo ta chien dich tu thong tin co ban |
| `/api/parse-expenditure-excel` | Doc file Excel ke hoach chi tieu |
| `/api/ocr-kyc` | Doc thong tin tu anh CCCD (OCR) |
| `/api/analyze-flag` | Phan tich bao cao vi pham |
| `/api/analyze-expenditure` | Audit gia thi truong cua vat pham |
| `/api/analyze-evidence` | Phan tich minh chung chi tieu |
| `/api/generate-suggestion-labels` | Goi y nhan cho vat pham |
| `/api/generate-post` | Tao noi dung bai viet |

## Q13.2: Perplexity AI dung de lam gi?

**Tra loi:** Perplexity AI duoc dung de **audit gia thi truong**:
- Khi staff duyet ke hoach chi tieu, he thong gui danh sach vat pham (ten, gia khai bao, so luong) cho Perplexity.
- Perplexity tra ve phan tich: gia khai bao co hop ly khong, gia thi truong la bao nhieu, co bat thuong khong.
- Giup staff phat hien truong hop Fund Owner khai gia cao de cat bot tien.

## Q13.3: AI Prompt duoc luu o dau?

**Tra loi:** Trong `campaign-service/src/main/resources/prompts/`:
- `ai_bill_analysis_prompt.txt`: Phan tich hoa don.
- `ai_campaign_description_prompt.txt`: Tao mo ta chien dich.
- `ai_flag_analysis_prompt.txt`: Phan tich bao cao vi pham.
- `ai_market_analysis_prompt.txt`: Phan tich gia thi truong.
- `ai_ocr_prompt.json`: OCR CCCD.

Admin co the **chinh sua prompt AI** tu giao dien web (`/admin/promt-AI`).

---

# 14. QUY CHUNG (General Fund)

## Q14.1: General Fund la gi? Hoat dong nhu the nao?

**Tra loi:** General Fund (Quy chung) la **quy trung tam** cua he thong:
- **ID co dinh = 1**, `type = "GENERAL_FUND"`.
- **Khong hien thi** trong danh sach chien dich thuong (filter `typeNot(GENERAL_FUND)`).
- **Chi admin** co quyen thao tac.

**Nguon thu:**
- **Tip:** Tien ung ho he thong tu donor.
- **Recovery:** So du thu hoi khi dong chien dich.
- **Dieu chuyen tu cac chien dich.**

**Chi tieu:**
- **Ho tro chien dich:** Chuyen tien tu quy chung den chien dich can ho tro.
- **Chi phi van hanh:** (neu co).

**Giao dich noi bo (Internal Transaction):**
```
Admin tao giao dich -> PENDING -> (Duyet) -> APPROVED/COMPLETED -> Chuyen tien giua cac quy
```
He thong kiem tra so du truoc khi chuyen (`balance >= amount`).

## Q14.2: Tai sao can General Fund?

**Tra loi:**
- Dam bao **khong co tien bi mat:** Khi dong chien dich, so du ve General Fund.
- **Minh bach:** Moi giao dich noi bo deu duoc ghi nhan, co staff ID, evidence.
- **Ho tro lien chien dich:** General Fund co the chuyen tien den chien dich khan cap.

---

# 15. BAO MAT & VULNERABILITY

## Q15.1: He thong co bi SQL Injection khong?

**Tra loi:** **Khong**, vi:
- Su dung **Spring Data JPA** voi **Named Parameters** va **JPQL**. Spring tự dong parameterize tat ca query.
- Khong co raw SQL nao su dung string concatenation voi input cua user.
- Du lieu input duoc validate qua **Jakarta Validation** (`@Valid`, `@NotNull`, `@Size`...).

## Q15.2: He thong co bi XSS khong?

**Tra loi:**
- Backend chi tra ve **JSON**, khong render HTML -> it rui ro XSS tu backend.
- Frontend (Next.js) tu dong **escape** tat ca noi dung trong JSX.
- Cac truong nhu `description`, `reason`, `content` duoc luu va hien thi nhu text, khong render HTML.
- **Can luu y:** Neu co rich text editor (bai viet), can sanitize HTML truoc khi luu.

## Q15.3: CORS duoc cau hinh nhu the nao?

**Tra loi:** CORS cau hinh tai **Gateway level**:
```properties
spring.cloud.gateway.globalcors.cors-configurations[/**].allowed-origin-patterns=${ALLOWED_ORIGINS:http://localhost:3000}
spring.cloud.gateway.globalcors.cors-configurations[/**].allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS
spring.cloud.gateway.globalcors.cors-configurations[/**].allow-credentials=true
```
- Chi cho phep origin tu frontend (localhost:3000 hoac domain production).
- Chi cho phep cac HTTP methods can thiet.
- Moi service con co `CorsConfig` rieng de them lop bao ve.

## Q15.4: API Internal (giua cac service) co bi truy cap trai phep khong?

**Tra loi:** Day la **diem yeu** hien tai:
- Cac endpoint `/api/internal/**` duoc de **public** trong Gateway filter de cho phep service-to-service communication.
- Dieu nay co nghia la bat ky ai biet URL cung co the goi cac API nay.

**Cach khac phuc (goi y):**
1. Dung **API Key/Secret** rieng cho internal communication.
2. Cau hinh network: chi cho phep traffic internal tu cac container trong cung Docker network.
3. Su dung **mutual TLS (mTLS)** giua cac service.

## Q15.5: Password duoc luu nhu the nao?

**Tra loi:** Password duoc hash bang **BCrypt** (`PasswordEncoder`):
- BCrypt tu dong them **salt** (khong can luu salt rieng).
- Cost factor mac dinh = 10 (2^10 = 1024 rounds).
- Khong the brute-force hoac rainbow table attack.

## Q15.6: JWT Secret co an toan khong?

**Tra loi:**
- JWT Secret duoc luu trong **bien moi truong** (`JWT_SECRET`), khong hardcode trong code.
- File `.env` duoc them vao `.gitignore`, khong push len Git.
- Tuy nhien, code co **fallback value**: `TrustFundME2024SecretKeyForJWTTokenGenerationSecureRandomString64Chars`. Day chi la gia tri mac dinh cho development, **production phai set bien moi truong rieng**.
- Secret dai 64 ky tu, du manh cho HMAC-SHA.

## Q15.7: He thong co chong CSRF khong?

**Tra loi:**
- He thong dung **JWT Bearer Token** (khong dung Cookie-based session) -> **immune voi CSRF** vi browser khong tu dong gui Authorization header.
- Spring Security cau hinh `csrf().disable()` vi da dung JWT.

## Q15.8: File upload co bi khai thac khong?

**Tra loi:**
- **Gioi han kich thuoc:** Max 50MB (cau hinh tai Gateway).
- **Luu tru:** File upload len **Supabase Storage** (khong luu local), giam rui ro path traversal.
- **Can cai thien:** Nen validate file type (chi cho anh/PDF), chong upload file doc hai (.exe, .php).

---

# 16. FRONTEND (DANBOX - NEXT.JS)

## Q16.1: Frontend duoc xay dung bang cong nghe gi?

**Tra loi:** Frontend la ung dung web **Next.js** (React framework) voi:
- **Next.js App Router** (folder-based routing).
- **TypeScript** cho type safety.
- **Tailwind CSS** cho styling.
- **API Routes** (`/app/api/`) lam proxy giua browser va backend.

## Q16.2: Tai sao frontend dung API Routes lam proxy?

**Tra loi:** Frontend co cac **API routes** (`/app/api/auth/login/route.ts`, `/app/api/flags/route.ts`...) lam **proxy** goi backend:
- **An URL backend:** Browser chi thay `/api/auth/login`, khong thay URL cua backend service.
- **Xu ly session:** Luu JWT trong cookie HttpOnly qua API route (an toan hon localStorage).
- **CORS:** Khong bi CORS issue vi browser goi cung origin (Next.js server).

## Q16.3: Frontend co nhung trang chinh nao?

**Tra loi:**

| Route | Chuc nang |
|-------|----------|
| `/` | Trang chu, hien thi chien dich noi bat |
| `/campaigns` | Danh sach chien dich |
| `/campaigns-details?id=X` | Chi tiet chien dich |
| `/campaign-creation` | Tao chien dich moi |
| `/donation?campaignId=X` | Trang donate |
| `/sign-in` | Dang nhap |
| `/account/profile` | Quan ly tai khoan |
| `/account/campaigns` | Cac chien dich cua toi |
| `/account/campaigns/expenditures` | Quan ly chi tieu |
| `/staff/*` | Dashboard staff (duyet KYC, chien dich, chi tieu, flag) |
| `/admin/*` | Dashboard admin (quan ly user, cau hinh, quy chung) |
| `/post/*` | Dien dan, bai viet |

## Q16.4: Component nao quan trong nhat tren frontend?

**Tra loi:**
- `ProtectedRoute`: Bao ve route can dang nhap.
- `RequireRole`: Kiem tra role truoc khi render UI.
- `BannedAccountWrapper`: Hien thi UI han che cho user bi ban.
- `EmailVerificationBanner`: Nhac user xac thuc email.
- `EnforcementAlertBanner`: Canh bao vi pham minh chung chi tieu.
- `NotificationBell`: Hien thi notification real-time.
- `Wallet` / `WalletDropdown`: Hien thi so du va lich su giao dich.
- `CampaignCard`: Card hien thi thong tin chien dich.
- `CampaignDonateCard`: Form donate.
- `OtpInput`: Nhap OTP 6 so.

---

# 17. DEPLOYMENT & DEVOPS

## Q17.1: He thong deploy nhu the nao?

**Tra loi:**
- **Docker Compose:** File `docker-compose.yml` dinh nghia tat ca services, co the chay `docker-compose up` de start toan bo he thong.
- **Dockerfile:** Moi service co Dockerfile rieng de build image.
- **Database:** MySQL 8.0 chay trong Docker container, data luu tren volume `mysql_data`.
- **CI/CD (Frontend):** Danbox co `.github/workflows/ci.yml` va `cd.yml` cho CI/CD tu dong.

## Q17.2: Moi truong development chay nhu the nao?

**Tra loi:**
- **PowerShell scripts:** Moi service co script rieng (`run-campaign-service.ps1`, `run-identity-service.ps1`...) de chay local.
- **`run-all-services.ps1`:** Chay tat ca services cung luc.
- **H2 Database:** Moi truong dev co the dung H2 (in-memory DB) thay vi MySQL (`run-identity-service-h2.ps1`).
- **Hot Reload:** Spring Boot DevTools cho phep restart nhanh khi code thay doi.

---

# 18. DATABASE DESIGN

## Q18.1: He thong dung co so du lieu gi? Tai sao?

**Tra loi:** **MySQL 8.0** - Relational Database.

**Tai sao chon MySQL?**
- **ACID compliance:** Giao dich tai chinh can dam bao **Atomicity, Consistency, Isolation, Durability**.
- **Quan he phuc tap:** Cac entity co nhieu quan he (User -> Campaign -> Expenditure -> Items).
- **JPA/Hibernate:** Spring Data JPA ho tro MySQL tot nhat.
- **Production-ready:** MySQL la RDBMS pho bien, community lon, nhieu tai lieu.

**Tai sao khong dung MongoDB?**
- He thong quy thien nguyen can **tinh nhat quan** (consistency) cao cho giao dich tai chinh. MongoDB (NoSQL) uu tien availability va partition tolerance (AP trong CAP theorem), khong phu hop cho nghiep vu tai chinh.

## Q18.2: Moi service dung database rieng hay chung?

**Tra loi:** **Moi service co database rieng** (Database per Service pattern):
- `trustfundme_identity_db`: User, KYC, BankAccount, Module.
- `trustfundme_campaign_db`: Campaign, Expenditure, FeedPost, Flag, TrustScore.
- `trustfundme_payment_db`: Donation, DonationItem, CassoTransaction.
- `trustfundme_chat_db`: Conversation, Message.
- `trustfundme_notification_db`: Notification.
- `trustfundme_media_db`: Media.

**Tai sao?** Dam bao **loose coupling**: moi service doc lap, co the thay doi schema ma khong anh huong service khac.

## Q18.3: Cac truong tien duoc luu nhu the nao?

**Tra loi:** Tat ca truong tien dung **`BigDecimal`** (Java) va **`DECIMAL(19,4)`** (MySQL):
- Tranh loi lam tron cua `float`/`double`.
- Precision 19 cho phep luu so len den hang trieu ty.
- Scale 4 cho phep 4 chu so thap phan.

---

# 19. TESTING

## Q19.1: He thong co test khong?

**Tra loi:** Co, he thong co **Unit Test** va **Integration Test**:

**Identity Service:**
- `AuthControllerTest`: Test dang ky, dang nhap, refresh token.
- `AuthServiceImplTest`: Test logic xac thuc.
- `UserServiceImplTest`: Test CRUD user.
- `UserKYCServiceImplTest`: Test quy trinh KYC.
- `BankAccountServiceImplTest`: Test bank account CRUD.
- `EmailServiceImplTest`: Test gui email.
- Repository tests: `UserRepositoryTest`, `OtpTokenRepositoryTest`, `BankAccountRepositoryTest`.

**Campaign Service:**
- Repository tests: `ApprovalTaskRepositoryTest`, `CampaignFollowRepositoryTest`, `ExpenditureRepositoryTest`, `FeedPostLikeRepositoryTest`.

**Payment Service:**
- `DonationServiceTest`: Test tao donation, payment flow.
- `DonationRepositoryTest`: Test query donation.

## Q19.2: Test coverage nhu the nao?

**Tra loi:** He thong tap trung test vao:
- **Service layer:** Logic nghiep vu chinh (auth, KYC, donation).
- **Repository layer:** Custom queries (JPA Named Queries, Native Queries).
- **Controller layer:** Endpoint behavior (AuthControllerTest).

**Can cai thien:** Them integration test cho cross-service communication (Payment -> Campaign balance update).

---

# 20. BAD CASE & TINH HUONG XAU

## Q20.1: Fund Owner lua dao - Tao chien dich gia de lay tien. Xu ly nhu the nao?

**Tra loi:** Nhieu lop bao ve:
1. **KYC bat buoc:** Fund Owner phai xac minh CCCD/ho chieu, face verification. He thong co full thong tin ca nhan de truy cuu.
2. **Staff duyet:** Moi chien dich phai qua staff review truoc khi duoc donate.
3. **Ke hoach chi tieu chi tiet:** Fund Owner phai liet ke tung muc chi, gia, so luong. Staff kiem tra tinh hop ly.
4. **AI Audit gia:** Perplexity AI kiem tra gia khai bao voi gia thi truong.
5. **Minh chung bat buoc:** Sau khi chi tien, phai nop hoa don/anh.
6. **Enforcement:** Qua han nop minh chung -> dong quy, thu hoi tien, tru Trust Score.
7. **Flag cong dong:** Bat ky ai cung co the to cao chien dich. Follower nhan thong bao ngay.
8. **Trust Score:** Lich su uy tin cong khai, Fund Owner co diem thap se kho nhan duoc donate.

## Q20.2: Hai Fund Owner dung chung CCCD de dang ky. He thong co phat hien?

**Tra loi:** **Co.** Trong `submitKYC()`:
```java
userKYCRepository.findFirstByIdNumber(request.getIdNumber()).ifPresent(existing -> {
    if (!existing.getUser().getId().equals(userId)) {
        throw new BadRequestException("So dinh danh da duoc dang ky boi tai khoan khac");
    }
});
```
He thong kiem tra so dinh danh (CCCD) truoc khi cho nop KYC. Neu trung voi user khac -> tu choi.

## Q20.3: Donor chuyen tien nhung ghi sai noi dung. Tien co bi mat khong?

**Tra loi:** **Khong.** He thong co fallback:
- Casso Webhook nhan duoc **tat ca giao dich** vao tai khoan lien ket voi chien dich.
- Neu noi dung khong match voi `TF {donationId}`, he thong tao **Anonymous Donation** tu dong.
- Tien van duoc cong vao balance cua chien dich (dua tren tai khoan nhan).

## Q20.4: He thong payment bi sap giua luc xu ly giao dich. Tien co bi mat?

**Tra loi:**
- **Casso Transaction luu truoc:** He thong luu `CassoTransaction` vao DB truoc khi xu ly tiep. Neu bi crash, du lieu van con.
- **Dedup bang tid:** Khi restart, neu webhook gui lai cung transaction, he thong skip vi `existsByTid(tid) = true`.
- **Balance sync flag:** `isBalanceSynchronized` dam bao khong cong tien 2 lan.
- **Transaction trong DB:** Moi giao dich deu luu vet, co the doi soat thu cong.

## Q20.5: 2 nguoi donate cung luc vao cung 1 vat pham (ITEMIZED). So luong co bi tran?

**Tra loi:**
- Truoc khi tao donation, frontend goi `checkExpenditureItemLimit()` de kiem tra `quantityLeft`.
- Khi tao donation, he thong **tru ngay** `quantityLeft` (immediate deduction).
- Tuy nhien, co **race condition** giua luc check va luc tru: 2 request check cung luc ca 2 thay du hang -> ca 2 tao donation -> vuot so luong.

**Cach giam thieu:**
- He thong dung `@Transactional` de dam bao atomicity.
- Co the cai thien bang **pessimistic locking** (`SELECT ... FOR UPDATE`) tren `quantityLeft`.

## Q20.6: Casso Webhook khong hoat dong (offline/network issue). Donation bi treo PENDING?

**Tra loi:** Co co che backup:
- **PaymentCleanupTask:** Cron job tu dong chuyen PENDING -> FAILED sau X phut.
- **Manual sync:** Frontend co the goi `POST /api/payments/donations/{id}/sync-balance` de dong bo thu cong.
- **PayOS Verify:** Goi `GET /api/payments/donations/{id}/verify` de kiem tra trang thai tren PayOS va dong bo.

## Q20.7: Staff thong dong voi Fund Owner de duyet sai. Xu ly nhu the nao?

**Tra loi:**
- **Approval Task:** He thong ghi nhan `staffId` cua nguoi duyet, co the truy vet.
- **Random Assignment:** Task duoc gan **ngau nhien** cho staff, giam kha nang thong dong voi 1 staff cu the.
- **Reassignment:** Admin co the reassign task cho staff khac.
- **Audit Trail:** Moi hanh vi duyet deu duoc ghi nhan (approvedByStaff, approvedAt).

**Can cai thien:** Them quy tac 2 nguoi duyet (4 eyes principle) cho chien dich lon.

## Q20.8: DDoS attack vao API. He thong co chiu duoc khong?

**Tra loi:** Hien tai **chua co rate limiting** tai Gateway. Cac bien phap can them:
- **Rate Limiting:** Dung Spring Cloud Gateway filter hoac Redis-based rate limiter.
- **WAF (Web Application Firewall):** Khi deploy len cloud, dung Cloudflare/AWS WAF.
- **Auto Scaling:** Docker Swarm/K8s co the tu dong scale them instance.

## Q20.9: Data leak - Database bi truy cap trai phep. Rui ro gi?

**Tra loi:** Rui ro:
- **Password:** Da hash BCrypt -> **an toan**, khong the giai ma.
- **JWT Secret:** Neu lo thi attacker co the gia mao token -> phai thay secret va revoke tat ca token.
- **CCCD, anh KYC:** Du lieu nhay cam, chua encrypt -> **rui ro cao**.
- **Bank Account:** So tai khoan, ten chu tai khoan -> **rui ro trung binh**.

**Can cai thien:** Encrypt du lieu nhay cam (CCCD, bank info) truoc khi luu DB.

## Q20.10: Fund Owner rut tien nhung khong mua hang, khong nop minh chung. Xu ly nhu the nao?

**Tra loi:** Day la **tinh huong nghiem trong nhat**. He thong xu ly:
1. **Evidence Deadline:** Khi giai ngan, he thong dat deadline nop minh chung (mac dinh 48h).
2. **Casso tu dong detect:** Khi tai khoan chi tien, Casso bao webhook -> he thong tu dong tao yeu cau nop minh chung.
3. **Enforcement Scheduler:** Chay moi phut kiem tra evidence qua han.
4. **Xu phat:**
   - **DONG chien dich** (`closeCampaign`).
   - **THU HOI so du** ve General Fund.
   - **TRU diem uy tin** (`OVERDUE_EVIDENCE`).
   - **GUI thong bao** canh bao cho Fund Owner.
5. **Cong dong:** Donor va follower co the to cao chien dich.
6. **Phap ly:** He thong da co CCCD, dia chi cua Fund Owner qua KYC. Co the phoi hop voi co quan chuc nang neu can.

## Q20.11: Neu admin bi hack, thay doi tat ca du lieu thi sao?

**Tra loi:**
- Admin co quyen cao nhat, day la rui ro lon.
- **Bien phap giam thieu:**
  - Bat buoc **2FA** cho tai khoan admin (tuong lai).
  - **Audit Log:** Ghi nhan moi hanh vi cua admin (hien tai co trong DB qua `createdByStaffId`, `approvedByStaff`).
  - **Backup:** Backup database dinh ky.
  - **Phan quyen chi tiet:** Tach quyen admin thanh nhieu cap (super admin, admin, staff).

## Q20.12: Nguoi donate yeu cau hoan tien. Xu ly nhu the nao?

**Tra loi:** Hien tai he thong **chua ho tro hoan tien cho donor** (refund to donor). Ly do:
- Tien da chuyen truc tiep vao tai khoan ngan hang cua chien dich, he thong khong giu tien.
- Viec hoan tien can thuong luong giua donor va Fund Owner.
- He thong chi ho tro **refund tu Fund Owner ve quy chung** (khi chi it hon du kien).

**Goi y:** Them chinh sach refund ro rang va tinh nang yeu cau hoan tien qua he thong.

## Q20.13: Neu Supabase (luu tru file) bi sap, anh KYC/chien dich bi mat?

**Tra loi:**
- Supabase ho tro **backup** va **replication** o tang infrastructure.
- Tuy nhien, he thong chua co **local backup** cua file.
- **Can cai thien:** Luu backup anh KYC tren cloud thu 2 (AWS S3 backup) hoac tren he thong noi bo.

---

# 21. TAI SAO CHON CONG NGHE NAY

## Q21.1: Tai sao chon Java Spring Boot?

**Tra loi:**
- **Enterprise-grade:** Spring Boot la framework pho bien nhat cho Java microservices.
- **Ecosystem phong phu:** Spring Cloud (Gateway, Eureka), Spring Data JPA, Spring Security - tat ca tich hop san.
- **Type-safe:** Java la strongly-typed language, phat hien loi som.
- **Performance:** JVM da duoc toi uu qua nhieu nam, xu ly concurrent tot voi thread pool.
- **Community:** Tai lieu, cau hoi tren StackOverflow, cong dong lon.
- **Phu hop voi moi truong hoc thuat:** Da hoc Java/Spring trong truong, tan dung kien thuc san co.

## Q21.2: Tai sao chon Next.js cho frontend?

**Tra loi:**
- **SSR (Server-Side Rendering):** SEO tot cho trang chien dich cong khai.
- **API Routes:** Lam proxy backend, xu ly cookie an toan.
- **React ecosystem:** Component-based, tai su dung code.
- **Performance:** Automatic code splitting, image optimization.
- **TypeScript:** Type safety, IntelliSense tot.
- **Vercel deploy:** Deploy frontend mien phi, nhanh chong.

## Q21.3: Tai sao chon MySQL thay vi PostgreSQL?

**Tra loi:**
- MySQL va PostgreSQL deu la RDBMS tot. Chon MySQL vi:
  - **Phong phien hon** o Viet Nam (hosting, tai lieu tieng Viet).
  - **XAMPP/Laragon:** De setup local cho development.
  - **Docker image nhe:** MySQL Docker image nho hon PostgreSQL.
  - **JPA tuong thich:** Spring Data JPA ho tro MySQL tot.
- Neu lam lai, PostgreSQL cung la lua chon tot (ho tro JSON, full-text search tot hon).

## Q21.4: Tai sao chon Eureka thay vi Consul/Kubernetes Service Discovery?

**Tra loi:**
- **Tich hop san** voi Spring Cloud: chi can them dependency, annotation la xong.
- **Hoc va dung nhanh:** Eureka don gian hon Consul (khong can setup them agent).
- **Phu hop quy mo nho:** Do an sinh vien voi 10 services, Eureka du dap ung.
- Neu quy mo lon hon (100+ services), nen chuyen sang **Kubernetes Service Discovery** hoac **Consul**.

## Q21.5: Tai sao chon Pusher thay vi Firebase Cloud Messaging?

**Tra loi:**
- **WebSocket don gian:** Pusher Channels ho tro real-time push notification qua WebSocket voi SDK de dung.
- **Khong can mobile app:** FCM tot cho mobile push, nhung frontend la web -> Pusher phu hop hon.
- **Free tier:** Pusher co free tier du cho do an (200 concurrent connections, 200,000 messages/day).

---

# 22. SO SANH VOI CAC HE THONG KHAC

## Q22.1: TrustFundMe khac gi GoFundMe?

**Tra loi:**

| Tieu chi | GoFundMe | TrustFundMe |
|----------|----------|-------------|
| **KYC** | Khong bat buoc | Bat buoc CCCD + face verify |
| **Minh bach chi tieu** | Khong co | Ke hoach chi tiet, AI audit, minh chung bat buoc |
| **Giam sat ngan hang** | Khong | Tu dong qua Casso webhook |
| **Trust Score** | Khong | Co he thong diem uy tin |
| **Flag** | Co bao cao | Co bao cao + thong bao follower + AI phan tich |
| **Loai chien dich** | Chi tien mat | ITEMIZED (vat pham) + AUTHORIZED (tien) |
| **Enforcement** | Thu cong | Tu dong dong quy khi vi pham |

## Q22.2: TrustFundMe khac gi GiveNow/Kitabisa?

**Tra loi:**
- **GiveNow/Kitabisa** la nen tang trung gian: tien gui vao nen tang, nen tang giai ngan.
- **TrustFundMe** la nen tang **truc tiep**: tien vao thang tai khoan ngan hang cua chien dich, he thong giam sat qua Casso.
- **Loi the TrustFundMe:** Khong giu tien, giam rui ro phap ly. Fund Owner nhan tien truc tiep, nhanh hon.
- **Bat loi:** Kho kiem soat hon vi khong giu tien. Giai quyet bang KYC + Evidence + Enforcement.

---

# 23. SCALABILITY & PERFORMANCE

## Q23.1: He thong co the scale nhu the nao?

**Tra loi:**
- **Horizontal Scaling:** Moi service co the chay nhieu instance. Eureka + Gateway load balance tu dong.
- **Docker Compose -> Docker Swarm/K8s:** De dang chuyen sang container orchestration.
- **Database Scaling:** MySQL Replication (read replica) cho cac query doc nhieu (Campaign list, Feed Post).
- **Cache:** Campaign Service co `CacheConfig` cho cache ket qua thong dung. Co the them Redis.

## Q23.2: He thong co nhung bottleneck nao?

**Tra loi:**
1. **N+1 Query:** `toCampaignResponse()` goi `identityServiceClient.getUserInfo()` va `mediaServiceClient.getMediaUrl()` cho moi campaign. Khi load danh sach 100 campaign -> 200 HTTP call.
   - **Cach fix:** Batch API (lay nhieu user cung luc) hoac cache.
2. **Synchronous REST Call:** Service-to-service communication dong bo. Neu Identity Service cham, Campaign Service cung bi cham.
   - **Cach fix:** Circuit Breaker (Resilience4j), Async message queue.
3. **Filter trong code Java:** `getByStatus()` va `getByCategoryId()` load **tat ca campaigns** roi filter trong Java thay vi query DB co dieu kien.
   - **Cach fix:** Dung `findByStatusAndTypeNot()` trong Repository.

## Q23.3: He thong chiu duoc bao nhieu nguoi dung dong thoi?

**Tra loi:** Voi cau hinh hien tai (1 instance moi service):
- **Gateway:** Spring WebFlux (non-blocking) -> xu ly hang ngan request/s.
- **Service:** Spring MVC default thread pool = 200 threads -> ~200 request dong thoi / service.
- **Database:** MySQL connection pool mac dinh = 10 -> bottleneck o day.
- **Uoc tinh:** ~100-500 nguoi dung dong thoi (phu thuoc do phuc tap cua query).

Voi scaling (nhieu instance + connection pool lon hon + cache): co the phuc vu hang ngan nguoi dung.

---

# PHU LUC: CAU HOI THUONG GAP KHI BAO VE

## Cau hoi tong quat:
1. "Em mo ta tong quan he thong duoc khong?" -> Xem Q1.1
2. "Tai sao chon kien truc microservices?" -> Xem Q1.1
3. "He thong co bao nhieu service? Chuc nang moi service?" -> Xem Q1.1
4. "Cac service giao tiep voi nhau nhu the nao?" -> Xem Q1.4
5. "Em dung Design Pattern nao?" -> Xem Q1.5

## Cau hoi ve bao mat:
6. "He thong xac thuc nhu the nao?" -> Xem Q2.1
7. "Neu token bi lo thi sao?" -> Xem Q2.4
8. "He thong co bi SQL Injection khong?" -> Xem Q15.1
9. "Password luu nhu the nao?" -> Xem Q15.5

## Cau hoi ve nghiep vu:
10. "Mo ta quy trinh tao chien dich" -> Xem Q3.1
11. "Lam sao dam bao Fund Owner khong tham o?" -> Xem Q20.1, Q6.2
12. "Thanh toan hoat dong nhu the nao?" -> Xem Q4.1
13. "KYC la gi? Tai sao can?" -> Xem Q5.1, Q5.3
14. "Trust Score hoat dong nhu the nao?" -> Xem Q7.1

## Cau hoi ve bad case:
15. "2 nguoi donate cung luc thi sao?" -> Xem Q20.5
16. "Fund Owner khong nop minh chung thi sao?" -> Xem Q20.10
17. "Webhook khong hoat dong thi sao?" -> Xem Q20.6
18. "Donor chuyen tien sai noi dung thi sao?" -> Xem Q20.3
19. "He thong bi sap giua giao dich thi sao?" -> Xem Q20.4
20. "Staff thong dong voi Fund Owner thi sao?" -> Xem Q20.7

## Cau hoi ve cong nghe:
21. "Tai sao chon Java Spring Boot?" -> Xem Q21.1
22. "Tai sao chon MySQL?" -> Xem Q21.3
23. "Tai sao chon Next.js?" -> Xem Q21.2
24. "He thong co the scale khong?" -> Xem Q23.1

## Cau hoi ve AI:
25. "AI duoc dung de lam gi trong he thong?" -> Xem Q13.1
26. "Perplexity AI audit gia nhu the nao?" -> Xem Q13.2

## Cau hoi ve so sanh:
27. "He thong cua em khac GoFundMe nhu the nao?" -> Xem Q22.1
28. "Uu diem noi bat nhat cua he thong la gi?" -> Muc dich minh bach: KYC bat buoc + Ke hoach chi tiet + AI audit + Minh chung bat buoc + Enforcement tu dong + Trust Score + Flag cong dong. Day la he thong quy thien nguyen **dau tien** ket hop tat ca cac co che nay.

---

# LUU Y QUAN TRONG KHI BAO VE

1. **Khong hoang khi bi hoi cau kho.** Neu khong biet, noi: "Em chua trinh bay trong scope do an, nhung huong giai quyet la..." roi goi y cach lam.
2. **Luon lien he code thuc te.** Khi tra loi, de cap ten file, ten class, ten method cu the.
3. **Nhan diem yeu.** Khi bi hoi diem yeu, tra loi trung thuc roi goi y cach cai thien.
4. **Nhan manh diem noi bat:**
   - Enforcement tu dong (dong quy khi vi pham).
   - AI audit gia thi truong.
   - Face verification trong KYC.
   - Casso webhook tu dong giam sat ngan hang.
   - Trust Score cong dong.
5. **Chuan bi demo live:** Mo san he thong, demo:
   - Tao chien dich -> Staff duyet.
   - Tao donation -> Quet QR -> Xem balance cap nhat.
   - Tao ke hoach chi tieu -> Staff duyet -> Giai ngan -> Nop minh chung.
   - Flag chien dich -> Staff xu ly.
   - Xem Trust Score, Leaderboard.
   - Admin: quan ly user, cau hinh, quy chung.

---

*Tai lieu nay duoc tao tu dong dua tren phan tich toan bo source code cua TrustFundMe-BE va danbox (web frontend).*
*Tong so: 10 microservices, 200+ Java files, 150+ TypeScript/React files.*
*Cap nhat: 2026-05-01*
