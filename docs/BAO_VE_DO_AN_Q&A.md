# TrustFundMe - TÀI LIỆU BẢO VỆ ĐỒ ÁN
## Toàn bộ câu hỏi & trả lời chi tiết cho hệ thống Quỹ Thiện Nguyện TrustFundMe

> Tài liệu này bao gồm tất cả câu hỏi có thể bị hỏi khi bảo vệ đồ án, kèm trả lời chi tiết dựa trên code thực tế.
> Mỗi phần được sắp xếp theo chủ đề: Kiến trúc, Nghiệp vụ, Bảo mật, Database, Payment, KYC, AI, và các tình huống bad-case.

---

# MỤC LỤC

1. [KIẾN TRÚC HỆ THỐNG (Architecture)](#1-kiến-trúc-hệ-thống)
2. [AUTHENTICATION & AUTHORIZATION](#2-authentication--authorization)
3. [CAMPAIGN - NGHIỆP VỤ QUỸ THIỆN NGUYỆN](#3-campaign---nghiệp-vụ-quỹ-thiện-nguyện)
4. [THANH TOÁN & DÒNG TIỀN (Payment Flow)](#4-thanh-toán--dòng-tiền)
5. [KYC - XÁC MINH DANH TÍNH](#5-kyc---xác-minh-danh-tính)
6. [CHI TIÊU & GIẢI NGÂN (Expenditure & Disbursement)](#6-chi-tiêu--giải-ngân)
7. [TRUST SCORE - ĐIỂM UY TÍN](#7-trust-score---điểm-uy-tín)
8. [FLAG & TỐ CÁO](#8-flag--tố-cáo)
9. [FEED POST & CỘNG ĐỒNG](#9-feed-post--cộng-đồng)
10. [NOTIFICATION & REAL-TIME](#10-notification--real-time)
11. [CHAT SERVICE](#11-chat-service)
12. [MEDIA SERVICE](#12-media-service)
13. [AI INTEGRATION](#13-ai-integration)
14. [QUỸ CHUNG (General Fund)](#14-quỹ-chung-general-fund)
15. [BẢO MẬT & VULNERABILITY](#15-bảo-mật--vulnerability)
16. [FRONTEND (DANBOX - NEXT.JS)](#16-frontend-danbox---nextjs)
17. [DEPLOYMENT & DEVOPS](#17-deployment--devops)
18. [DATABASE DESIGN](#18-database-design)
19. [TESTING](#19-testing)
20. [BAD CASE & TÌNH HUỐNG XẤU](#20-bad-case--tình-huống-xấu)
21. [TẠI SAO CHỌN CÔNG NGHỆ NÀY](#21-tại-sao-chọn-công-nghệ-này)
22. [SO SÁNH VỚI CÁC HỆ THỐNG KHÁC](#22-so-sánh-với-các-hệ-thống-khác)
23. [SCALABILITY & PERFORMANCE](#23-scalability--performance)

---

# 1. KIẾN TRÚC HỆ THỐNG

## Q1.1: Hệ thống của bạn được thiết kế theo kiến trúc gì? Tại sao chọn kiến trúc đó?

**Trả lời:** Hệ thống TrustFundMe được xây dựng theo kiến trúc **Microservices** với **10 service riêng biệt**:

| Service | Port | Chức năng |
|---------|------|-----------|
| **API Gateway** | 8080 | Định tuyến, xác thực JWT, CORS, load balancing |
| **Discovery Server** | 8761 | Service registry (Eureka) |
| **Identity Service** | 8081 | Đăng ký, đăng nhập, KYC, quản lý user, bank account |
| **Campaign Service** | 8082 | Chiến dịch, chi tiêu, trust score, feed post, flag |
| **Media Service** | 8083 | Upload/quản lý file ảnh/video (Supabase Storage) |
| **Chat Service** | 8086 | Nhắn tin, lịch hẹn (WebSocket) |
| **Payment Service** | 8087 | Thanh toán, donate, Casso webhook |
| **Notification Service** | 8088 | Thông báo real-time (Pusher), email |
| **Feed Service** | (trong campaign-service) | Bài viết, bình luận, like |
| **Flag Service** | (trong campaign-service) | Báo cáo vi phạm |

**Tại sao chọn Microservices thay vì Monolith?**
- **Tách biệt nghiệp vụ:** Mỗi service phụ trách 1 domain riêng (Single Responsibility). Payment xử lý tiền tách biệt với Campaign xử lý chiến dịch.
- **Độc lập deploy:** Có thể deploy riêng từng service mà không ảnh hưởng các service khác.
- **Scalability:** Có thể scale riêng service chịu tải cao (Payment, Campaign) mà không cần scale toàn bộ hệ thống.
- **Cách ly lỗi (Fault Isolation):** Nếu Payment bị lỗi, Campaign vẫn hoạt động bình thường.
- **Phù hợp với nhóm:** Các thành viên có thể phát triển song song trên các service khác nhau.

## Q1.2: API Gateway hoạt động như thế nào? Tại sao cần Gateway?

**Trả lời:** API Gateway sử dụng **Spring Cloud Gateway** (Reactive/WebFlux), là điểm vào duy nhất của hệ thống:

1. **Routing:** Định tuyến request đến đúng service dựa trên URL path. Ví dụ `/api/campaigns/**` -> campaign-service, `/api/payments/**` -> payment-service.
2. **JWT Authentication:** Global filter kiểm tra JWT token trước khi cho request đi qua. Token hợp lệ thì inject header `X-User-Id`, `X-User-Email`, `X-User-Role` vào request.
3. **Load Balancing:** Sử dụng prefix `lb://` kết hợp với Eureka để tự động load balance giữa các instance của cùng 1 service.
4. **CORS:** Cấu hình global CORS cho phép frontend (localhost:3000) gọi API.
5. **Retry:** Tự động retry 3 lần khi service trả về 503 (Service Unavailable).
6. **Public endpoints:** Một số endpoint không cần token (GET campaigns, tạo donation, webhook PayOS...).

**Tại sao cần Gateway mà không gọi trực tiếp từng service?**
- Frontend chỉ cần biết 1 URL (port 8080), không cần biết từng service chạy port nào.
- Tập trung logic xác thực tại 1 điểm, tránh việc mỗi service phải tự verify JWT.
- Dễ dàng thêm rate limiting, logging, monitoring tại gateway.

## Q1.3: Service Discovery (Eureka) hoạt động ra sao? Tại sao không dùng IP cố định?

**Trả lời:** Hệ thống sử dụng **Netflix Eureka** làm Service Registry:
- Mỗi service khi start sẽ tự **đăng ký** với Eureka (tên service, IP, port).
- Gateway và các service khi cần gọi service khác sẽ **tìm kiếm** (lookup) trên Eureka thay vì hardcode IP.
- Eureka heartbeat mỗi 5s (`lease-renewal-interval-in-seconds=5`), nếu service không gửi heartbeat trong 15s thì bị xóa khỏi registry.
- Gateway fetch registry mỗi 5s (`registry-fetch-interval-seconds=5`) để cập nhật nhanh.

**Tại sao không dùng IP cố định?**
- Khi deploy lên cloud (Docker, K8s), IP của container thay đổi liên tục.
- Khi scale thêm instance, IP mới không ai biết trước.
- Eureka cho phép tự động phát hiện service mới và xóa service chết.

## Q1.4: Các service giao tiếp với nhau như thế nào?

**Trả lời:** Các service giao tiếp qua **REST API (HTTP)** sử dụng `RestTemplate`:
- Campaign Service gọi Identity Service để lấy thông tin user, kiểm tra KYC.
- Payment Service gọi Campaign Service để cập nhật balance.
- Campaign Service gọi Notification Service để gửi thông báo.
- Payment Service gọi Identity Service để lấy thông tin bank account.

Mỗi service có các **Client class** (ví dụ `IdentityServiceClient`, `PaymentServiceClient`, `NotificationServiceClient`) để wrap các HTTP call. Các client này xử lý exception và fallback khi service không khả dụng.

**Hạn chế và hướng cải thiện:**
- Hiện tại dùng **synchronous REST call** (đồng bộ). Nếu 1 service chậm, service gọi cũng bị chậm (coupling).
- Có thể cải thiện bằng **Message Queue** (RabbitMQ/Kafka) cho các event không cần response ngay (send notification, update trust score).
- Hiện tại chưa có **Circuit Breaker** (Resilience4j), có thể thêm để tránh cascading failure.

## Q1.5: Hệ thống có sử dụng Design Pattern nào không?

**Trả lời:** Có, hệ thống áp dụng **20 Design Pattern** thực tế (có evidence trong code):

### A. Microservices Patterns (Kiến trúc phân tán)

1. **Database per Service Pattern:** Mỗi service có database riêng biệt — `trustfundme_identity_db`, `trustfundme_campaign_db`, `trustfundme_payment_db`, `trustfundme_chat_db`, `trustfundme_notification_db`, `trustfundme_media_db`. Đảm bảo loose coupling, mỗi service độc lập schema. *(Evidence: `docker-compose.yml` — 6 JDBC URL khác nhau)*

2. **API Gateway Pattern:** Spring Cloud Gateway là điểm vào duy nhất, định tuyến 21 routes đến các service qua `lb://service-name`. *(Evidence: `api-gateway/src/main/resources/application.properties` — 21 route definitions)*

3. **Service Registry / Service Discovery Pattern:** Netflix Eureka — 1 server (`@EnableEurekaServer`) + 6 client (`@EnableDiscoveryClient`). Service tự đăng ký khi start, Gateway tự tìm service qua Eureka thay vì hardcode IP. *(Evidence: `discovery-server/DiscoveryServerApplication.java`, 6 service application class)*

4. **Anti-Corruption Layer (ACL):** 10 Client class wrap các HTTP call giữa service, cách ly domain khỏi giao thức bên ngoài, xử lý error/fallback. Đặc biệt `PerplexityClient.java` wrap Perplexity AI API, chuyển đổi response JSON thành domain object. *(Evidence: `campaign-service/client/IdentityServiceClient.java`, `PerplexityClient.java`,...)*

### B. GoF Design Patterns (Gang of Four)

5. **Singleton Pattern:** Tất cả Spring Bean (`@Service`, `@Component`, `@Repository`, `@RestController`) mặc định là Singleton scope — chỉ 1 instance trong IoC container. Tổng cộng **150+ Singleton beans**. *(Evidence: 31 `@Service`, 30 `@Component`, 36 `@Repository`, 37 `@RestController`)*

6. **Observer Pattern (Pub/Sub):** Webhook Forwarder publish event lên Pusher channel, Payment Service subscribe và lắng nghe:
   - **Publisher:** `webhook-forwarder/api/webhook.js` → `pusher.trigger('payos-webhook', 'payment', payload)`
   - **Subscriber:** `PusherWebhookListener.java` → `channel.bind("payment", new SubscriptionEventListener())`
   - Tương tự cho Casso webhook qua `PusherCassoListener.java`.
   *(Evidence: `payment-service/config/PusherWebhookListener.java`, `PusherCassoListener.java`)*

7. **Builder Pattern:** 131 class dùng `@Builder` (Lombok) — tất cả entity, DTO, response class. Hỗ trợ `@Builder.Default` cho giá trị mặc định (ví dụ: `Campaign.balance = BigDecimal.ZERO`). *(Evidence: `Campaign.java`, `Donation.java`, `ApiResponse.java`,...)*

8. **Template Method Pattern:** 
   - 6 class extends `OncePerRequestFilter` — Spring định nghĩa skeleton `doFilter()`, mỗi service override `doFilterInternal()` để xử lý JWT.
   - 2 class implements `CommandLineRunner` — Spring gọi `run()` khi app khởi động, `GeneralFundSeeder` và `SystemConfigSeeder` override để seed dữ liệu.
   *(Evidence: `JwtAuthenticationFilter.java` trong 6 service, `GeneralFundSeeder.java`)*

9. **Chain of Responsibility Pattern:**
   - **Gateway:** `JwtAuthenticationFilter implements GlobalFilter` → xử lý JWT rồi gọi `chain.filter(exchange)` chuyển tiếp.
   - **Mỗi service:** `SecurityFilterChain` → chuỗi filter xử lý tuần tự (CORS → JWT → Authorization).
   *(Evidence: `JwtAuthenticationFilter.java` line 30: `return chain.filter(exchange)`)*

10. **Adapter Pattern:** 10 Client class đóng vai trò Adapter — chuyển đổi HTTP response từ service khác thành object phù hợp với domain hiện tại. Ví dụ: `IdentityServiceClient.getUserInfo()` gọi REST và trả về `UserInfoResponse`. *(Evidence: `campaign-service/client/IdentityServiceClient.java` — 10 methods gọi 10 endpoint khác nhau)*

11. **Facade Pattern:** Các Client class cũng đóng vai trò Facade — gom nhiều endpoint của 1 service bên ngoài vào 1 class duy nhất. Ví dụ: `IdentityServiceClient` gom 10 method: `validateUserExists()`, `getUserInfo()`, `getVerificationStatus()`, `upgradeUserRole()`, `updateTrustScore()`, `getLeaderboard()`,... *(Evidence: `IdentityServiceClient.java`)*

12. **Proxy Pattern:** `webhook-forwarder/` là serverless proxy trên Vercel — nhận webhook từ PayOS/Casso rồi chuyển tiếp qua Pusher đến Payment Service. Không xử lý logic, chỉ forward. *(Evidence: `webhook-forwarder/api/webhook.js`, `webhook-forwarder/api/casso.js`)*

### C. Enterprise / Application Patterns

13. **MVC Pattern (Model-View-Controller):** Spring MVC cho tất cả service (trừ Gateway dùng WebFlux):
    - **Model:** 131 entity class (`@Entity`) với JPA annotations.
    - **Controller:** 37 class `@RestController` xử lý HTTP request.
    - **Service:** 31 class `@Service` xử lý business logic.
    - (Không có View vì là REST API — trả về JSON).
    *(Evidence: toàn bộ cấu trúc package `model/`, `controller/`, `service/`)*

14. **Service Layer Pattern:** Tách Controller → Service Interface → Service Implementation → Repository. Ví dụ: `CampaignService` (interface) → `CampaignServiceImpl` (implementation). Tổng cộng **27 interface + 25 implementation**. *(Evidence: `campaign-service/service/CampaignService.java` → `service/impl/CampaignServiceImpl.java`)*

15. **Repository Pattern:** 36 Repository interface extends `JpaRepository` (Spring Data JPA). Spring tự động generate implementation tại runtime. *(Evidence: `CampaignRepository.java`, `UserRepository.java`, `DonationRepository.java`,...)*

16. **DTO Pattern (Data Transfer Object):** 56 Request DTO + 53 Response DTO tách biệt với entity. Ví dụ: `CreateCampaignRequest` (client gửi lên) → `CampaignResponse` (server trả về). Có `ApiResponse<T>` là generic wrapper cho tất cả response. *(Evidence: package `model/request/`, `model/response/`)*

17. **Dependency Injection:** 2 hình thức:
    - **Constructor Injection:** `@RequiredArgsConstructor` (Lombok) trong **94 file** — cách được khuyến khích.
    - **Bean Method Injection:** 22 class `@Configuration` với 25+ `@Bean` methods tạo `RestTemplate`, `SecurityFilterChain`, `PasswordEncoder`,...
    *(Evidence: hầu hết tất cả service/client/config class)*

18. **Soft Delete Pattern:** Campaign dùng `status = "DELETED"` thay vì xóa thực sự khỏi database. Media Service cũng filter `statusNot("DELETED")`. Giữ lại dữ liệu để audit/truy vết. *(Evidence: `Campaign.java` line 69: `STATUS_DELETED = "DELETED"`, `CampaignServiceImpl.markAsDeleted()`)*

19. **Data Seeder Pattern:** 2 class tự động seed dữ liệu mặc định khi ứng dụng khởi động:
    - `GeneralFundSeeder` (`@Order(1)`): Tạo Quỹ Chung (ID=1), cấu hình `EXPENDITURE_EVIDENCE_DEADLINE_HOURS`.
    - `SystemConfigSeeder` (`@Order(2)`): Load AI prompts từ file `.txt` vào `system_config` table.
    - Dùng `INSERT IGNORE` / `findByConfigKey().isEmpty()` để đảm bảo **idempotent** (chạy nhiều lần không duplicate).
    *(Evidence: `campaign-service/config/GeneralFundSeeder.java`, `SystemConfigSeeder.java`)*

20. **Scheduler Pattern:** 2 Scheduled Task chạy định kỳ:
    - `PaymentCleanupTask` (`@Scheduled(cron = "0 * * * * *")`): Mỗi phút đánh dấu donation PENDING quá 10 phút thành FAILED, rollback quantity.
    - `ExpenditureEnforcementScheduler` (`@Scheduled(fixedDelay = 60000)`): Mỗi phút kiểm tra evidence quá hạn, đóng quỹ vi phạm, trừ Trust Score.
    *(Evidence: `payment-service/PaymentCleanupTask.java`, `campaign-service/scheduler/ExpenditureEnforcementScheduler.java`)*

**Bonus — Mapper Pattern:** `ModuleGroupMapper.java`, `ModuleMapper.java` trong identity-service chuyển đổi entity sang response DTO.

## Q1.6: Nếu Identity Service bị chết/crash, các service khác sẽ hoạt động ra sao?

**Trả lời:** Đây là câu hỏi rất hay. Identity Service là service **được gọi nhiều nhất** (lấy thông tin user, kiểm tra KYC, bank account...). Khi nó chết:

**Ảnh hưởng cụ thể:**
- **Campaign Service:** Vẫn hoạt động nhưng **dữ liệu bị thiếu**. Các Client class dùng pattern `try-catch` và trả về `null` khi không gọi được:
  ```java
  // IdentityServiceClient.java
  try {
      return restTemplate.getForObject(url, Map.class);
  } catch (Exception e) {
      log.error("Failed to fetch user info: {}", e.getMessage());
      return null; // Trả về null, campaign vẫn hiển thị nhưng thiếu tên chủ quỹ
  }
  ```
  - `toCampaignResponse()` sẽ trả về campaign với `ownerName = null`, `coverImageUrl = null`.
  - `reviewCampaign()` sẽ **không thể duyệt** vì không kiểm tra được KYC → throw exception.
  - `createCampaign()` sẽ **fail** vì `validateUserExists()` throw `ResponseStatusException(400)`.

- **Payment Service:** Không lấy được thông tin bank account → **không tạo được QR code** cho donation. Nhưng Casso webhook vẫn nhận giao dịch bình thường (không phụ thuộc Identity Service ở bước nhận webhook).

- **Notification Service:** Vẫn hoạt động độc lập (chỉ nhận request gửi notification).

**Cách xử lý hiện tại (Graceful Degradation):**
- Hệ thống áp dụng **fail-soft**: các Client class bắt exception và trả về giá trị mặc định (null/empty) thay vì crash toàn bộ.
- Các tính năng đọc (xem danh sách chiến dịch) vẫn hoạt động nhưng thiếu thông tin bổ sung.
- Các tính năng ghi (tạo chiến dịch, duyệt KYC) sẽ fail rõ ràng với error message.

**Hướng cải thiện:**
1. **Circuit Breaker (Resilience4j):** Khi Identity Service fail liên tục, tự động "mở mạch" và trả về fallback data thay vì gọi liên tục.
2. **Cache user info:** Lưu thông tin user vào Redis/Caffeine cache, khi Identity Service chết vẫn có dữ liệu cũ để hiển thị.
3. **Message Queue:** Các event không cần response ngay (notification, trust score) gửi qua queue, retry tự động khi service phục hồi.

## Q1.7: Nếu Payment Service bị chết giữa lúc đang xử lý giao dịch, tiền có bị mất không?

**Trả lời:** Đây là tình huống **nghiêm trọng nhất** trong hệ thống tài chính. Phân tích chi tiết:

**Kịch bản 1: Payment Service chết TRƯỚC khi lưu CassoTransaction**
- Casso webhook gửi request nhưng Payment Service không phản hồi.
- Casso sẽ **retry webhook** (cơ chế retry của Casso).
- Khi Payment Service phục hồi, nhận lại webhook → xử lý bình thường.
- **Kết luận:** Tiền KHÔNG mất, chỉ bị trễ xử lý.

**Kịch bản 2: Payment Service chết SAU khi lưu CassoTransaction nhưng TRƯỚC khi cập nhật balance**
- `CassoTransaction` đã lưu vào DB (an toàn).
- Nhưng `updateBalanceOnlyInCampaignService()` chưa được gọi → campaign balance chưa tăng.
- **Đây là trạng thái inconsistent:** Tiền đã vào tài khoản ngân hàng, giao dịch đã ghi nhận, nhưng số dư trên hệ thống chưa cập nhật.

**Cách xử lý hiện tại:**
- Dữ liệu `CassoTransaction` còn trong DB → có thể **đối soát thủ công**.
- Frontend có nút "Sync Balance" để staff đồng bộ lại.
- `isBalanceSynchronized` flag trên `Donation` giúp biết donation nào chưa sync.

**Hướng cải thiện:**
1. **Outbox Pattern:** Lưu event "cần cập nhật balance" vào bảng `outbox` trong cùng transaction với `CassoTransaction`. Một scheduler đọc outbox và retry cho đến khi thành công.
2. **Saga Pattern:** Chia quy trình thanh toán thành các bước có compensation (rollback) nếu bước sau fail.
3. **Idempotency Key:** Đảm bảo mỗi lần gọi `updateBalance` với cùng transaction ID chỉ cộng tiền 1 lần.

## Q1.8: Nếu MySQL database bị chết, toàn bộ hệ thống sẽ ra sao?

**Trả lời:** MySQL là **Single Point of Failure lớn nhất** của hệ thống:

**Hiện trạng:**
- Tất cả 7 service (trừ Discovery Server) đều kết nối đến **cùng 1 MySQL instance** (dù mỗi service dùng database riêng).
- Docker Compose chỉ chạy **1 container MySQL**, không có replication.
- Không có cơ chế **failover** hay **backup** tự động trong config.

**Khi MySQL chết:**
- **TẤT CẢ service ngừng hoạt động** vì không thể đọc/ghi dữ liệu.
- Gateway vẫn chạy nhưng mọi request đều trả về lỗi 500.
- Eureka vẫn hoạt động (dùng in-memory registry) nhưng không còn ý nghĩa.
- **Mất dữ liệu** nếu MySQL data volume bị hỏng.

**Cách xử lý hiện tại:**
- Docker volume `mysql_data` giữ dữ liệu khi restart container.
- `restart: always` trong Docker Compose tự động restart MySQL nếu bị crash.
- Có thể backup thủ công bằng `mysqldump`.

**Hướng cải thiện cho production:**
1. **MySQL Replication (Master-Slave):** 1 master ghi, nhiều slave đọc. Slave có thể lên thay master nếu master chết.
2. **MySQL Cluster/InnoDB Cluster:** Multi-master replication với automatic failover.
3. **Automated Backup:** Cron job backup database hàng ngày lên cloud storage (S3, GCS).
4. **Connection Pool Tuning:** Tăng `HikariCP.maximumPoolSize` từ mặc định 10 lên 30-50 cho production.
5. **Health Check trong Docker:** Thêm `healthcheck` cho MySQL container, các service chỉ start khi MySQL sẵn sàng (`depends_on.condition: service_healthy`).

## Q1.9: Nếu API Gateway bị chết, client có thể kết nối được không? Có giải pháp nào không?

**Trả lời:** **Không.** Gateway là **điểm vào duy nhất** (Single Entry Point) của hệ thống:

**Khi Gateway chết:**
- Frontend không thể gọi bất kỳ API nào.
- Toàn bộ hệ thống **mất kết nối với client**.
- Casso webhook cũng không gửi được → giao dịch thanh toán bị trễ (nhưng Casso sẽ retry).

**Đây là Single Point of Failure theo thiết kế** — đánh đổi (trade-off) của Gateway Pattern:
- **Lợi ích:** Tập trung xác thực, routing, CORS → đơn giản hóa các service phía sau.
- **Rủi ro:** Gateway chết = toàn bộ hệ thống không thể truy cập.

**Giải pháp cho production:**
1. **Chạy nhiều instance Gateway** (2-3 instance) + Load Balancer phía trước (Nginx, HAProxy, AWS ALB).
2. **Kubernetes:** K8s tự động restart pod Gateway nếu bị crash (liveness probe) và chạy nhiều replica.
3. **Docker Compose hiện tại:** Có `restart: always` → Gateway tự restart nếu crash, nhưng downtime vài giây.
4. **Health Check:** Frontend có thể detect Gateway down và hiển thị "Hệ thống đang bảo trì" thay vì lỗi trắng.

## Q1.10: Nếu Eureka (Discovery Server) bị chết, các service có giao tiếp được với nhau không?

**Trả lời:** **Có, trong thời gian ngắn.** Phân tích chi tiết:

**Cơ chế cache:**
- Mỗi service và Gateway đều cache **bản sao registry** từ Eureka.
- Khi Eureka chết, service dùng **cached registry** để resolve địa chỉ các service khác.
- Tuy nhiên, Gateway có config `spring.cloud.loadbalancer.cache.enabled=false` → **tắt loadbalancer cache** → Gateway sẽ **không thể resolve service** khi Eureka chết.

**Tác động:**
- **Các service có RestTemplate hardcode URL** (payment-service dùng `app.campaign-service.url`) → **vẫn hoạt động** vì không phụ thuộc Eureka.
- **Gateway** → sẽ **fail** sau khi cached registry hết hạn vì loadbalancer cache bị tắt.
- **Không thể đăng ký service mới** hay phát hiện service chết.

**Hướng cải thiện:**
1. **Bật loadbalancer cache:** `spring.cloud.loadbalancer.cache.enabled=true` ở Gateway.
2. **Eureka Cluster:** Chạy 2-3 Eureka instance (peer-to-peer replication). Khi 1 chết, các instance còn lại vẫn hoạt động.
3. **Fallback DNS/IP:** Cấu hình service có thể fallback về IP cố định khi Eureka không khả dụng.

## Q1.11: Distributed Transaction — Khi donor donate thành công nhưng cập nhật balance chiến dịch thất bại, xử lý thế nào?

**Trả lời:** Đây là **vấn đề kinh điển** của microservices — Distributed Transaction.

**Hiện trạng:** Hệ thống **KHÔNG có Distributed Transaction**, **KHÔNG có Saga Pattern**, **KHÔNG có compensation logic**.

**Flow nguy hiểm cụ thể:**
```
1. Casso webhook → Payment Service lưu CassoTransaction ✅
2. Payment Service cập nhật Donation status = "PAID" ✅
3. Payment Service gọi Campaign Service: updateBalance() ❌ FAIL
```

Nếu bước 3 fail: **tiền đã vào tài khoản ngân hàng, donation đã là PAID, nhưng campaign.balance KHÔNG tăng**.

**Cách xử lý hiện tại:**
- `@Transactional` chỉ hoạt động **trong 1 service** (local transaction), không bảo vệ được cross-service.
- Code `updateBalanceOnlyInCampaignService()` sẽ **throw exception** nếu fail, nhưng `CassoTransaction` đã được lưu ở bước trước (khác transaction).
- `isBalanceSynchronized` flag giúp nhận biết donation nào chưa đồng bộ balance.
- Staff có thể **đối soát thủ công** dựa trên `CassoTransaction` trong DB.

**Vấn đề thêm với ITEMIZED campaign:**
- `processQuantityUpdate()` chạy **loop từng item**, gọi REST riêng cho mỗi item:
  ```java
  for (DonationItem item : donation.getItems()) {
      // Gọi Campaign Service trừ quantityLeft cho từng item
      processQuantityUpdate(item.getExpenditureItemId(), item.getQuantity());
  }
  ```
- Nếu item thứ 3/5 fail → 2 item đầu đã bị trừ quantity, item 3,4,5 chưa → **partial update không có rollback**.

**Hướng cải thiện:**
1. **Outbox Pattern:** Lưu event vào bảng `outbox` trong cùng local transaction. Scheduler poll outbox và gửi đến service đích. Retry cho đến khi thành công.
2. **Saga Pattern (Choreography):** Mỗi service publish event khi hoàn thành, service tiếp theo lắng nghe và xử lý. Nếu fail → publish compensation event.
3. **Two-Phase Commit (2PC):** Ít được khuyến khích trong microservices vì chậm và phức tạp.
4. **Reconciliation Job:** Cron job chạy định kỳ, so sánh `CassoTransaction` với `Donation` và `Campaign.balance`, tự động sửa inconsistency.

## Q1.12: Hệ thống có Circuit Breaker không? Khi 1 service chậm, có gây ảnh hưởng dây chuyền (cascading failure) không?

**Trả lời:** **Không có Circuit Breaker.** Hệ thống hiện tại **hoàn toàn không dùng Resilience4j** hay bất kỳ thư viện circuit breaker nào.

**Cascading failure có thể xảy ra:**
```
Identity Service chậm (10s/request)
→ Campaign Service gọi Identity Service, bị block 10s mỗi request
→ Thread pool Campaign Service (200 threads) cạn kiệt
→ Campaign Service không phản hồi được request mới
→ Gateway timeout → Frontend hiển thị lỗi cho TẤT CẢ chức năng
```

**Vấn đề timeout:**
- Campaign Service tạo `RestTemplate` **KHÔNG có timeout config** → mặc định là **vô hạn** (infinite timeout của JDK).
- Nghĩa là: nếu Identity Service bị treo (hang), Campaign Service sẽ **chờ mãi mãi**, chiếm thread cho đến khi hết thread pool.
- Payment Service có timeout tốt hơn: `connect=5s, read=5s`.

**Cách xử lý hiện tại:**
- Gateway có retry 3 lần: `spring.cloud.gateway.default-filters[0]=Retry=3` — nhưng điều này có thể **làm tệ hơn** vì retry 3 lần vào service đang chậm = 3x tải.
- Các Client class dùng `catch(Exception)` và trả về null — giảm thiểu crash nhưng **không ngăn thread bị block**.

**Hướng cải thiện:**
1. **Resilience4j Circuit Breaker:**
   ```java
   @CircuitBreaker(name = "identityService", fallbackMethod = "fallbackGetUser")
   public UserInfo getUser(Long userId) { ... }
   
   public UserInfo fallbackGetUser(Long userId, Exception e) {
       return UserInfo.builder().fullName("Đang cập nhật...").build();
   }
   ```
2. **Timeout config cho tất cả RestTemplate:** Connect timeout 3s, Read timeout 5s.
3. **Bulkhead Pattern:** Giới hạn số thread cho mỗi downstream service, không để 1 service chậm chiếm hết thread pool.
4. **Async call với CompletableFuture:** Các call không cần response ngay (notification, trust score) xử lý bất đồng bộ.

## Q1.13: Hệ thống hiện tại có thể scale lên bao nhiêu người dùng? Bottleneck nằm ở đâu?

**Trả lời:** Với cấu hình hiện tại (1 instance mỗi service, chưa tối ưu), ước tính **100-300 người dùng đồng thời**.

**Phân tích bottleneck chi tiết:**

| Tầng | Config hiện tại | Bottleneck | Giải pháp |
|------|----------------|------------|-----------|
| **Gateway** | WebFlux (non-blocking) | Ít bottleneck, xử lý hàng ngàn req/s | Chạy 2-3 instance |
| **Service** | Tomcat 200 threads (default) | 200 request đồng thời / service | Tăng thread pool hoặc dùng WebFlux |
| **Database** | HikariCP 10 connections (default) | **BOTTLENECK LỚN NHẤT** — chỉ 10 query đồng thời | Tăng lên 30-50 connections |
| **Inter-service** | Sync REST, không timeout | 1 service chậm → block tất cả | Circuit Breaker + timeout |
| **N+1 Query** | Gọi REST per-item trong loop | 100 campaigns → 200 HTTP calls | Batch API + cache |
| **Cache** | Caffeine in-memory (1 JVM) | Mất khi restart, không chia sẻ giữa instances | Redis distributed cache |

**Kế hoạch scale theo giai đoạn:**

**Giai đoạn 1 (500 users):** Tăng connection pool (30-50), thêm timeout (3-5s), thêm Redis cache cho user info + campaign list.

**Giai đoạn 2 (2,000 users):** Chạy 2-3 instance mỗi service, MySQL read replica, Message Queue cho notification/email/trust-score.

**Giai đoạn 3 (10,000+ users):** Chuyển sang Kubernetes, auto-scaling, distributed tracing (Jaeger), centralized logging (ELK), MySQL cluster.

## Q1.14: Hệ thống có cơ chế monitoring/giám sát không? Làm sao biết service nào đang gặp vấn đề?

**Trả lời:** Hiện tại monitoring ở mức **cơ bản:**

**Có:**
- **Spring Boot Actuator** được tích hợp trong **tất cả 8 services**:
  ```properties
  management.endpoints.web.exposure.include=health,info
  management.endpoint.health.show-details=always
  ```
- Endpoint `/actuator/health` trả về trạng thái service (UP/DOWN), bao gồm:
  - Database connection status (MySQL)
  - Disk space
  - Eureka registration status
- Gateway expose thêm endpoint `/actuator/gateway` để xem routing table.
- **Logging:** Mỗi service dùng SLF4J/Logback, log ra console và file.

**Chưa có:**
- **Không có Prometheus/Grafana** → không có dashboard metrics (CPU, memory, request rate, error rate).
- **Không có Distributed Tracing** (Zipkin/Jaeger) → không thể trace 1 request đi qua nhiều service.
- **Không có Centralized Logging** (ELK Stack/Loki) → phải vào từng container đọc log riêng.
- **Không có Alerting** → không tự động cảnh báo khi service down hay error rate tăng.
- Docker Compose **không có health check** cho các service (chỉ dùng `depends_on` đơn giản).

**Hướng cải thiện:**
1. **Prometheus + Grafana:** Thêm `micrometer-registry-prometheus` dependency, expose `/actuator/prometheus` endpoint.
2. **Jaeger/Zipkin:** Thêm `micrometer-tracing-bridge-brave` để trace request across services.
3. **ELK Stack:** Filebeat thu log → Logstash xử lý → Elasticsearch lưu → Kibana hiển thị.
4. **Docker health check:**
   ```yaml
   healthcheck:
     test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
     interval: 30s
     timeout: 10s
     retries: 3
   ```

## Q1.15: Tại sao không dùng Message Queue (RabbitMQ/Kafka) mà dùng hoàn toàn REST đồng bộ?

**Trả lời:** Hệ thống hiện tại **100% synchronous REST** — không có Message Queue.

**Lý do chọn REST đồng bộ:**
- **Đơn giản:** REST call dễ implement, dễ debug, dễ hiểu. Phù hợp với scope đồ án sinh viên.
- **Không cần infrastructure thêm:** Không phải setup RabbitMQ/Kafka server, giảm độ phức tạp deployment.
- **Đủ cho quy mô nhỏ:** Với lượng user dưới 500, synchronous REST hoàn toàn đáp ứng được.

**Hạn chế và tác động:**
- **Notification gửi đồng bộ:** Khi duyệt chiến dịch, hệ thống gọi REST tới Notification Service → nếu Notification chậm, Campaign Service cũng bị chậm. Code đã xử lý bằng cách `catch(Exception)` và bỏ qua nếu fail:
  ```java
  // NotificationServiceClient - tất cả 3 service đều có pattern này
  try {
      restTemplate.postForObject(url, request, Void.class);
  } catch (Exception e) {
      log.error("Failed to send notification: {}", e.getMessage());
      // Không throw → không ảnh hưởng flow chính
  }
  ```
- **Trust Score cập nhật đồng bộ:** Nếu Identity Service chậm khi sync trust score, Campaign Service bị ảnh hưởng.
- **Không có retry queue:** Nếu notification fail, nó **mất luôn** — không có dead letter queue để retry.

**Nếu dùng Message Queue (hướng cải thiện):**
```
Campaign Service → [RabbitMQ Queue] → Notification Service
                                    → Trust Score Update
                                    → Email Service
```
- **Decouple:** Campaign Service publish event và return ngay, không cần chờ.
- **Retry tự động:** Message ở trong queue, nếu consumer fail thì retry.
- **Dead Letter Queue:** Message fail quá nhiều lần → chuyển sang DLQ để xử lý thủ công.
- **Back-pressure:** Queue đầy thì producer chậm lại, không overload consumer.

## Q1.16: Rate Limiting — Nếu ai đó spam hàng triệu request vào API, hệ thống có chịu được không?

**Trả lời:** **Không.** Hệ thống hiện tại **không có rate limiting** ở bất kỳ tầng nào.

**Các endpoint nguy hiểm nhất khi bị spam:**
- `POST /api/auth/register` (public) → tạo hàng triệu tài khoản giả.
- `POST /api/auth/send-otp` (public) → spam email OTP (tốn tiền SMTP).
- `POST /api/payments/create` (public) → tạo hàng triệu donation giả.
- `POST /api/payments/webhook` (public) → giả lập webhook.
- `GET /api/campaigns/**` (public) → overload database.

**Hướng cải thiện:**
1. **Gateway Rate Limiter (Spring Cloud Gateway + Redis):**
   ```yaml
   filters:
     - name: RequestRateLimiter
       args:
         redis-rate-limiter.replenishRate: 10    # 10 req/s
         redis-rate-limiter.burstCapacity: 20    # burst tối đa 20
         key-resolver: "#{@ipKeyResolver}"       # rate limit theo IP
   ```
2. **Rate limit theo user:** Mỗi user tối đa X request/phút (dùng userId từ JWT).
3. **CAPTCHA:** Thêm reCAPTCHA cho form đăng ký, gửi OTP.
4. **WAF (Production):** Cloudflare/AWS WAF chặn DDoS ở tầng network.

## Q1.17: Graceful Shutdown — Khi deploy bản mới, request đang xử lý giữa chừng có bị mất không?

**Trả lời:** **Có thể bị mất.** Hệ thống hiện tại **không cấu hình graceful shutdown**.

**Vấn đề:**
- Không có `server.shutdown=graceful` trong bất kỳ service nào.
- Không có `spring.lifecycle.timeout-per-shutdown-phase` để chờ request đang chạy hoàn thành.
- Docker Compose dùng `restart: always` nhưng **không có `stop_grace_period`**.

**Khi service bị stop/restart:**
- Các request đang xử lý sẽ **bị cắt ngang**.
- Transaction đang chạy có thể **bị rollback** (tốt) hoặc **bị corrupt** (xấu, nếu chỉ commit 1 phần).
- Đặc biệt nguy hiểm cho Payment Service: nếu đang xử lý webhook giữa chừng → mất giao dịch.

**Hướng cải thiện:**
```properties
# Thêm vào application.properties của mỗi service
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```
- Khi nhận signal shutdown, service sẽ **từ chối request mới** nhưng **chờ request đang chạy hoàn thành** (tối đa 30s).
- Docker Compose thêm `stop_grace_period: 40s` cho mỗi service.

## Q1.18: Hệ thống có đảm bảo data consistency giữa các service không? Nếu trust score trong Campaign Service và Identity Service bị lệch nhau thì sao?

**Trả lời:** Hệ thống dùng mô hình **best-effort consistency** — KHÔNG đảm bảo strong consistency giữa các service.

**Các điểm có thể bị lệch (inconsistent):**

| Dữ liệu | Service A | Service B | Khi nào lệch |
|----------|-----------|-----------|--------------|
| Trust Score | `TrustScoreLog` (Campaign) | `User.trustScore` (Identity) | `IdentityServiceClient.updateTrustScore()` fail |
| Campaign Balance | `Donation` (Payment) | `Campaign.balance` (Campaign) | `updateBalance()` fail |
| Item Quantity | `DonationItem.quantity` (Payment) | `ExpenditureItem.quantityLeft` (Campaign) | `processQuantityUpdate()` fail |
| User Role | Upgrade FUND_OWNER (Campaign) | `User.role` (Identity) | `upgradeUserRole()` fail |

**Cách xử lý hiện tại:**
- Tất cả các cross-service update đều dùng `try-catch` → fail thì chỉ **log error**, không retry.
- Không có **reconciliation job** (job đối soát) để phát hiện và sửa inconsistency.
- Staff có thể phát hiện bất thường qua dashboard (so sánh số liệu giữa các service).

**Hướng cải thiện:**
1. **Event Sourcing:** Lưu tất cả event (DONATION_CREATED, BALANCE_UPDATED, TRUST_SCORE_CHANGED) vào event store. Có thể replay event để rebuild state.
2. **Reconciliation Scheduler:** Cron job chạy hàng đêm, so sánh dữ liệu giữa các service, phát hiện và tự động sửa inconsistency.
3. **Saga with compensation:** Nếu bước 2 fail → gọi compensation (rollback bước 1).

## Q1.19: Endpoint `/api/campaigns/*/update-balance` cho phép bất kỳ ai gọi được — đây có phải lỗ hổng bảo mật không?

**Trả lời:** **Đúng, đây là lỗ hổng bảo mật nghiêm trọng.** Phân tích:

**Vấn đề trong SecurityConfig của Campaign Service:**
```java
// SecurityConfig.java
.requestMatchers(HttpMethod.PUT, "/api/campaigns/*/update-balance").permitAll()
```
- Endpoint này cho phép **bất kỳ ai** (không cần JWT) gửi request cập nhật balance của campaign.
- Attacker có thể gửi: `PUT /api/campaigns/5/update-balance?amount=999999999` → cộng tiền giả.

**Lý do tồn tại:** Endpoint này được thiết kế cho **inter-service communication** — Payment Service gọi Campaign Service để cập nhật balance. Vì Payment Service không gửi JWT user token, endpoint phải để public.

**Cách khắc phục:**
1. **Internal API Key:** Thêm header `X-Internal-Api-Key` cho inter-service call, verify ở Campaign Service.
2. **Network-level isolation:** Chỉ cho phép traffic từ Docker internal network đến endpoint này (không expose ra ngoài Gateway).
3. **Gateway chặn:** Thêm rule ở Gateway: chặn tất cả request từ client đến `/api/campaigns/*/update-balance` — chỉ cho phép từ internal service.
4. **Mutual TLS (mTLS):** Service gọi nhau phải có certificate riêng để xác thực.

**Các endpoint internal khác cũng có vấn đề tương tự:**
- `/api/internal/**` → tất cả đều public ở Gateway.
- `/api/emails/**` → có thể spam email.
- `POST /api/users/*/trust-score` → thay đổi trust score bất kỳ user nào.

---

# 2. AUTHENTICATION & AUTHORIZATION

## Q2.1: Hệ thống xác thực người dùng như thế nào?

**Trả lời:** Hệ thống sử dụng **JWT (JSON Web Token)** với HMAC-SHA:

1. **Đăng ký (`/api/auth/register`):** User gửi email + password + fullName. Password được hash bằng `BCryptPasswordEncoder`. Hệ thống trả về accessToken + refreshToken.
2. **Đăng nhập (`/api/auth/login`):** User gửi email + password. Hệ thống verify password với hash trong DB, nếu đúng thì generate JWT.
3. **Google OAuth2 (`/api/auth/google`):** User gửi Google ID Token. Hệ thống dùng `GoogleIdTokenVerifier` để verify với Google, nếu user chưa có thì tự động tạo tài khoản.
4. **JWT Token:** Chứa `sub` (userId), `email`, `role`. Token được ký bằng HMAC-SHA với `JWT_SECRET` (64+ ký tự).
5. **Refresh Token:** Khi access token hết hạn, frontend gửi refresh token để lấy cặp token mới mà không cần đăng nhập lại.

## Q2.2: Phân quyền (Authorization) hoạt động như thế nào?

**Trả lời:** Hệ thống có **5 role**:

| Role | Quyền |
|------|-------|
| `USER` | Xem chiến dịch, donate, viết bài, bình luận, flag |
| `FUND_OWNER` | Tất cả của USER + tạo chiến dịch, quản lý chi tiêu |
| `FUND_DONOR` | (Dự phòng, hiện tại tương đương USER) |
| `STAFF` | Duyệt chiến dịch, KYC, chi tiêu, flag, quản lý feed post |
| `ADMIN` | Toàn quyền, quản lý user, ban, cấu hình hệ thống |

**Cơ chế phân quyền:**
1. **Gateway level:** JWT filter inject `X-User-Role` header vào mỗi request.
2. **Service level:** Dùng `@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")` trên các endpoint cần bảo vệ. Ví dụ: chỉ STAFF/ADMIN mới được duyệt chiến dịch (`PUT /api/campaigns/{id}/review`).
3. **Spring Security:** Mỗi service có `SecurityConfig` riêng, cấu hình endpoint nào public, endpoint nào cần role cụ thể.
4. **Frontend:** Component `<RequireRole>` kiểm tra role trước khi render UI admin/staff.

## Q2.3: Làm sao để chống giả mạo token?

**Trả lời:**
- JWT được ký bằng **HMAC-SHA** với secret key dài 64 ký tự, được lưu trong biến môi trường (`JWT_SECRET`), không hardcode trong code.
- Mỗi request, Gateway verify chữ ký của token. Nếu token bị sửa đổi (payload hoặc signature), `Jwts.parser().verifyWith(key)` sẽ throw exception và trả về 401.
- Token có thời gian hết hạn (`expiration`), sau đó token không còn hợp lệ.
- **Refresh token** cũng được ký cùng secret, có thời gian sống dài hơn access token.

## Q2.4: Nếu ai đó đánh cắp JWT token thì sao?

**Trả lời:** Đây là rủi ro chung của JWT-based authentication. Biện pháp giảm thiểu:
- Token có **thời gian hết hạn ngắn** (configurable qua `jwt.expiration`).
- **Refresh Token Rotation:** Mỗi lần refresh thì cặp cũ bị voidify, cặp mới được tạo.
- **HTTPS (SSL):** Trong production, tất cả traffic qua HTTPS để chống man-in-the-middle.
- **HttpOnly Cookie:** Frontend có thể lưu token trong HttpOnly cookie để chống XSS (tương lai cải thiện).
- **Logout:** Frontend xóa token khỏi localStorage/cookie.

**Hạn chế hiện tại:** Chưa có blacklist/revoke token mechanism trên server. Nếu cần cải thiện, có thể dùng Redis để lưu blacklist các token đã bị revoke.

## Q2.5: OTP và Reset Password hoạt động ra sao?

**Trả lời:**
1. User gửi email, hệ thống tạo **OTP 6 số** bằng `SecureRandom` (100000-999999).
2. OTP được lưu vào bảng `otp_tokens` với `expiresAt` (mặc định 10 phút).
3. Hệ thống gửi OTP qua **email** (Gmail SMTP).
4. User gửi OTP để verify, hệ thống check: OTP đúng + chưa hết hạn + chưa used.
5. Nếu verify thành công, hệ thống tạo **Password Reset Token** (JWT với type="password_reset", hết hạn 30 phút).
6. User gửi token + newPassword để đổi mật khẩu.

**Anti-abuse:** OTP chỉ dùng 1 lần (`markAsUsed`). Nếu ai đó brute-force OTP, hệ thống vẫn bảo vệ vì chỉ có 900000 giá trị và hết hạn sau 10 phút.

## Q2.6: Tại sao cho phép user bị ban/deactivated vẫn đăng nhập được?

**Trả lời:** Đây là **thiết kế có ý**. Code comment ghi rõ:
```
// Allow login even if account is deactivated - the frontend will handle showing the restricted view
```
- Mục đích: Khi user bị ban, họ vẫn có thể đăng nhập để **xem lý do bị ban** (`banReason`) và liên hệ admin.
- Frontend sẽ kiểm tra `isActive` và hiển thị **restricted view** thay vì dashboard bình thường (component `BannedAccountWrapper`).
- Điều này nhân văn hơn việc hoàn toàn khóa tài khoản mà không giải thích.

---

# 3. CAMPAIGN - NGHIỆP VỤ QUỸ THIỆN NGUYỆN

## Q3.1: Quy trình tạo và duyệt chiến dịch như thế nào?

**Trả lời:** Quy trình gồm **5 bước**:

```
USER tạo chiến dịch -> PENDING_APPROVAL -> Staff duyệt -> APPROVED (hoặc REJECTED)
                                                        -> Chiến dịch hoạt động
```

**Chi tiết:**
1. User điền thông tin (title, description, category, coverImage, startDate, endDate).
2. Hệ thống validate: category tồn tại, fundOwnerId tồn tại trong Identity Service.
3. Chiến dịch được tạo với `status = "PENDING_APPROVAL"`.
4. Hệ thống tự động **nâng cấp role** user lên `FUND_OWNER` (gọi `identityServiceClient.upgradeUserRole()`).
5. Hệ thống tạo **ApprovalTask** và **random assign cho 1 staff** để duyệt.
6. Staff xem task trên dashboard, review chiến dịch:
   - **Nếu APPROVED:** Kiểm tra user đã KYC chưa (`kycVerified`). Nếu chưa KYC thì không cho approve. Gửi notification "Chiến dịch đã được duyệt".
   - **Nếu REJECTED:** Bắt buộc nhập `rejectionReason`. Gửi notification "Chiến dịch bị từ chối".
7. **Trust Score:** Duyệt thành công +điểm, bị từ chối -điểm.

## Q3.2: Hệ thống có bao nhiêu loại chiến dịch? Khác nhau như thế nào?

**Trả lời:** Có **3 loại chiến dịch**:

| Loại | Giải thích | Flow chi tiêu |
|------|-----------|--------------|
| **ITEMIZED** | Quyên góp vật phẩm cụ thể (gạo, sữa, sách...). Donor chọn vật phẩm và số lượng. | Balance = tổng số tiền donate. Fund Owner yêu cầu rút tiền để mua hàng. |
| **AUTHORIZED** | Quyên góp tiền trực tiếp cho mục đích cụ thể. | Staff duyệt chi tiêu -> Tự động chuyển sang WITHDRAWAL_REQUESTED -> Staff giải ngân. |
| **GENERAL_FUND** | Quỹ chung của hệ thống, không hiển thị trong danh sách. Chỉ admin thao tác. | Admin điều chuyển tiền từ quỹ chung đến các chiến dịch. |

**Sự khác biệt quan trọng:**
- **ITEMIZED:** Donor thấy từng vật phẩm (tên, giá, số lượng), donate theo vật phẩm. `quantityLeft` giảm khi có người donate.
- **AUTHORIZED:** Donor donate tiền trực tiếp, không chọn vật phẩm. Fund Owner lên kế hoạch chi tiêu, staff duyệt rồi giải ngân.

## Q3.3: Campaign có những trạng thái (status) nào?

**Trả lời:**

| Status | Mô tả |
|--------|-------|
| `PENDING_APPROVAL` | Vừa tạo, chờ staff duyệt |
| `APPROVED` | Đã được duyệt, đang hoạt động, nhận donate |
| `REJECTED` | Bị staff từ chối |
| `PAUSED` | Bị tạm dừng bởi staff/admin |
| `CLOSED` | Đã đóng, số dư được thu hồi về Quỹ chung |
| `DISABLED` | Bị vô hiệu hóa do vi phạm (overdue evidence) |
| `DELETED` | Xóa mềm (soft delete) |

## Q3.4: Khi đóng chiến dịch (CLOSED), số dư xử lý như thế nào?

**Trả lời:** Khi staff/admin đóng chiến dịch (`closeCampaign`):
1. Kiểm tra `balance > 0`.
2. Nếu còn dư, tạo giao dịch **RECOVERY** từ campaign về General Fund (ID=1).
3. Set `balance = 0`.
4. Set `status = "CLOSED"`.

Điều này đảm bảo **không có tiền bị mất** khi đóng chiến dịch. Toàn bộ số dư được chuyển về quỹ chung để tái sử dụng.

## Q3.5: Fund Owner có thể tự ý thay đổi thông tin chiến dịch không?

**Trả lời:** **Có giới hạn:**
- Chỉ được sửa khi `status != "DISABLED"` và `status != "APPROVED"`.
- Nghĩa là: chỉ sửa được khi đang `PENDING_APPROVAL` hoặc `REJECTED` (để sửa và nộp lại).
- Khi đã `APPROVED` (đang hoạt động), **không** được sửa để đảm bảo thông tin minh bạch với donor.

## Q3.6: CampaignFollower là gì? Có tác dụng gì?

**Trả lời:** Người dùng có thể **follow** một chiến dịch:
- Khi chiến dịch bị flag (tố cáo), hệ thống gửi **notification đến tất cả follower** để cảnh báo.
- Follower có thể theo dõi tiến độ, bài viết của chiến dịch.
- Thống kê `followerCount` cũng là chỉ số đo lường mức độ quan tâm của cộng đồng.

---

# 4. THANH TOÁN & DÒNG TIỀN

## Q4.1: Hệ thống thanh toán hoạt động như thế nào?

**Trả lời:** TrustFundMe tích hợp **VietQR** kết hợp **Casso Webhook** để xử lý thanh toán:

```
Donor tạo donation -> Hệ thống tạo QR Code (VietQR) -> Donor quét QR chuyển khoản
    -> Ngân hàng gửi thông báo về Casso -> Casso gửi webhook đến hệ thống
    -> Hệ thống match transaction với donation -> Cập nhật balance chiến dịch
```

**Chi tiết từng bước:**
1. **Tạo donation:** Donor chọn chiến dịch, nhập số tiền (và tip tùy chọn). Hệ thống tạo bản ghi `Donation` với `status = "PENDING"`.
2. **Tạo QR Code:** Hệ thống gọi Identity Service lấy thông tin bank account của chiến dịch, tạo URL VietQR với format: `TF {donationId}` trong nội dung chuyển khoản.
3. **Donor thanh toán:** Quét QR hoặc chuyển khoản thủ công với nội dung `TF {donationId}`.
4. **Casso Webhook:** Khi ngân hàng ghi nhận giao dịch, Casso gửi webhook đến `/api/payments/webhook`. Hệ thống:
   - Lưu `CassoTransaction` (chống trùng `tid`).
   - Tìm `Donation` bằng regex `TF {id}` trong description.
   - Nếu match: cập nhật `status = "PAID"`, đồng bộ balance.
   - Nếu không match: tạo `Anonymous Donation` (người chuyển trực tiếp vào tài khoản quỹ).
5. **Cập nhật balance:** Gọi Campaign Service API `PUT /api/campaigns/{id}/update-balance`.

## Q4.2: Tại sao chọn VietQR + Casso thay vì tích hợp PayOS trực tiếp?

**Trả lời:**
- **VietQR** là chuẩn quốc gia, hỗ trợ **mọi ngân hàng** tại Việt Nam, miễn phí tạo QR.
- **Casso** là dịch vụ **theo dõi giao dịch ngân hàng tự động**, miễn phí cho lượng nhỏ.
- **PayOS** được tích hợp để **tạo payment link** nhưng thực tế hệ thống chuyển sang dùng **VietQR + Casso** vì:
  - PayOS tính phí giao dịch, Casso + VietQR miễn phí cho quỹ thiện nguyện.
  - VietQR hỗ trợ trực tiếp chuyển khoản vào tài khoản ngân hàng của **chính chủ quỹ** thay vì qua trung gian.
  - Phù hợp với mục đích minh bạch: tiền vào thẳng tài khoản được liên kết với chiến dịch.

## Q4.3: Làm sao đảm bảo không bị trùng (duplicate payment)?

**Trả lời:** Hệ thống có nhiều lớp chống trùng:
1. **Casso Transaction ID (tid):** Trước khi xử lý, kiểm tra `cassoTransactionRepository.existsByTid(tid)`. Nếu đã xử lý thì skip.
2. **Database Unique Constraint:** Field `tid` trong `CassoTransaction` có unique constraint. Nếu 2 thread ghi cùng lúc, `DataIntegrityViolationException` sẽ xảy ra và thread sau bị skip.
3. **Balance Sync Flag:** Mỗi `Donation` có flag `isBalanceSynchronized`. Chỉ cập nhật balance 1 lần, các lần gọi sau sẽ bị skip (`Balance already synchronized`).

## Q4.4: Donor có thể donate mà không cần đăng nhập không?

**Trả lời:** **Có.** Hệ thống hỗ trợ **Guest Donation**:
- Endpoint `POST /api/payments/create` được để là **public** trong Gateway filter.
- `donorId` có thể null. Khi đó `isAnonymous = true` (mặc định).
- Guest chỉ cần nhập số tiền, quét QR, chuyển khoản. Không cần tài khoản.

## Q4.5: Tip (tiền ủng hộ hệ thống) xử lý như thế nào?

**Trả lời:** Donation có 3 trường tiền:
- `donationAmount`: Số tiền ủng hộ cho chiến dịch.
- `tipAmount`: Số tiền ủng hộ cho hệ thống (tùy chọn, mặc định 0).
- `totalAmount = donationAmount + tipAmount`.

Chỉ có `donationAmount` được cộng vào balance của chiến dịch. `tipAmount` giúp hệ thống duy trì hoạt động mà không thu phí bắt buộc.

## Q4.6: Với chiến dịch ITEMIZED, khi donor chọn vật phẩm, số lượng được xử lý như thế nào?

**Trả lời:**
1. Trước khi tạo donation, frontend gọi `GET /api/payments/check-item-limit?expenditureItemId=X&quantity=Y` để kiểm tra `quantityLeft`.
2. Khi tạo donation, hệ thống **trừ ngay `quantityLeft`** (immediate deduction) để chống tràn (2 người donate cùng lúc).
3. Nếu donation bị FAILED/CANCELLED, hệ thống **hoàn lại số lượng** (rollback).
4. Logic:
   - `processQuantityUpdate()`: Gọi Campaign Service `PUT /api/expenditures/items/{id}/update-quantity?amount=N` để trừ `quantityLeft`.
   - `processQuantityRollback()`: Gửi `amount = -N` để cộng lại.

## Q4.7: Casso Webhook bị replay attack thì sao?

**Trả lời:**
- **Dedup bằng tid:** Mỗi transaction có `tid` duy nhất từ Casso. Hệ thống kiểm tra `existsByTid(tid)` trước khi xử lý.
- **Webhook Key Verification:** Hệ thống verify `Secure-Token` header với `webhookKey` đã lưu trong Identity Service. Nếu không khớp thì reject.
- **HMAC Signature:** Hỗ trợ verify Casso V2 signature format `t=timestamp,v1=hmac_sha256`.

**Lưu ý:** Code hiện tại có bypass verification cho testing (`REMOVED BYPASS FOR TESTING`). Trong production phải bật lại verification.

## Q4.8: Cleanup Donation: nếu donor tạo donation mà không thanh toán thì sao?

**Trả lời:** Có `PaymentCleanupTask` (Scheduled Task) chạy định kỳ:
- Tìm các `Donation` có `status = "PENDING"` và đã tạo quá X phút.
- Tự động chuyển sang `FAILED`.
- Rollback `quantityLeft` (nếu là ITEMIZED donation).

---

# 5. KYC - XÁC MINH DANH TÍNH

## Q5.1: Quy trình KYC như thế nào?

**Trả lời:** KYC (Know Your Customer) gồm **4 bước**:

```
User nộp hồ sơ -> PENDING -> Staff duyệt -> APPROVED (hoặc REJECTED)
```

1. **Nộp hồ sơ** (`POST /api/kyc/submit`): User gửi:
   - Loại giấy tờ (`idType`): CCCD, hộ chiếu...
   - Số định danh (`idNumber`): Số CCCD.
   - Ngày cấp, ngày hết hạn, nơi cấp.
   - **Ảnh mặt trước** và **ảnh mặt sau** CCCD.
   - **Ảnh selfie** (chụp trực tiếp).
   - **Face Descriptor** (128-dim vector từ face-api.js).
   - **Liveness Metadata** (dữ liệu chứng minh người thật, không phải ảnh chụp từ màn hình).
   - **Face Mesh Sample** (điểm mốc 3D khuôn mặt).
2. **AI OCR:** Frontend sử dụng AI để đọc thông tin từ ảnh CCCD (tên, ngày sinh, địa chỉ) và tự động điền vào form.
3. **Face Verification:** Frontend dùng face-api.js để:
   - So sánh khuôn mặt trong selfie với ảnh trên CCCD.
   - Thực hiện liveness detection (quay đầu trái/phải, nhắm mắt).
4. **Staff duyệt:** Staff xem hồ sơ KYC, so sánh thông tin, duyệt hoặc từ chối.

## Q5.2: Hệ thống chống giả mạo KYC như thế nào?

**Trả lời:** Nhiều tầng bảo vệ:

1. **Chống trùng CCCD:** `userKYCRepository.findFirstByIdNumber(idNumber)` kiểm tra CCCD đã được đăng ký bởi người khác chưa. Nếu có -> từ chối.
2. **Face Descriptor (128-dim vector):** Lưu face embedding để có thể so sánh 2 khuôn mặt.
3. **Liveness Detection:** `livenessMetadata` ghi nhận các bước verify (quay đầu, nhắm mắt, thời gian) chứng minh người thật ngồi trước camera.
4. **Face Mesh Sample:** 3D face landmarks giúp xác nhận độ sâu khuôn mặt (chống ảnh 2D).
5. **Staff Manual Review:** Dù AI phân tích, vẫn cần con người duyệt để đảm bảo.

## Q5.3: Tại sao KYC là bắt buộc trước khi tạo chiến dịch?

**Trả lời:**
- Trong `reviewCampaign()`: Trước khi duyệt APPROVED, hệ thống kiểm tra `kycVerified`:
  ```
  if (!verificationStatus.isKycVerified()) {
      throw "Cannot approve campaign. Owner's KYC is not verified.";
  }
  ```
- Đảm bảo **mọi chủ quỹ đều đã được xác minh danh tính**, chống lừa đảo, giả mạo.
- Đây là yêu cầu pháp lý với tổ chức tiếp nhận quỹ thiện nguyện tại Việt Nam.

## Q5.4: Nếu KYC bị từ chối, user làm gì?

**Trả lời:** User có thể **nộp lại** (`PUT /api/kyc/resubmit`):
- Chỉ nộp lại được khi `status != APPROVED` (đã duyệt thì không sửa được).
- Kiểm tra CCCD mới (nếu đổi số CCCD) có trùng không.
- Reset `status = PENDING`, xóa `rejectionReason`.

## Q5.5: Dữ liệu KYC có bị mã hóa không?

**Trả lời:** Hệ thống có `EncryptionUtils.java` trong Identity Service. Tuy nhiên:
- Hiện tại dữ liệu KYC (số CCCD, ảnh) **chưa được encrypt** tất cả trường.
- Ảnh KYC lưu trên **Supabase Storage** (cloud).
- Để tăng bảo mật, nên encrypt các trường nhạy cảm như `idNumber` trước khi lưu DB.

---

# 6. CHI TIÊU & GIẢI NGÂN (Expenditure & Disbursement)

## Q6.1: Quy trình chi tiêu và giải ngân hoạt động như thế nào?

**Trả lời:** Đây là **cơ chế minh bạch cốt lõi** của TrustFundMe:

### Với chiến dịch ITEMIZED:
```
Fund Owner tạo Expenditure (kế hoạch chi) -> PENDING_REVIEW -> Staff duyệt -> APPROVED
-> Fund Owner yêu cầu rút tiền -> WITHDRAWAL_REQUESTED -> Staff giải ngân (DISBURSED)
-> Fund Owner mua hàng và nộp minh chứng (evidence)
```

### Với chiến dịch AUTHORIZED:
```
Fund Owner tạo Expenditure -> PENDING_REVIEW -> Staff duyệt -> Tự động WITHDRAWAL_REQUESTED
-> Staff giải ngân (DISBURSED) -> Fund Owner nộp minh chứng
```

**Chi tiết:**
1. **Tạo Expenditure:** Fund Owner lên kế hoạch chi tiêu với các hạng mục (categories) và vật phẩm (items), mỗi item có `expectedPrice`, `expectedQuantity`.
2. **Staff duyệt:** Kiểm tra kế hoạch hợp lý, số tiền phù hợp.
3. **Yêu cầu rút tiền:** Fund Owner gửi yêu cầu rút tiền kèm thời hạn nộp minh chứng.
4. **Giải ngân:** Staff xác nhận chuyển tiền, upload bằng chứng chuyển khoản (`proofUrl`).
5. **Nộp minh chứng:** Fund Owner nộp hóa đơn, ảnh hàng hóa để chứng minh đã chi đúng mục đích.

## Q6.2: Làm sao đảm bảo Fund Owner không chi tiền sai mục đích?

**Trả lời:** Nhiều lớp kiểm soát:

1. **Kế hoạch chi tiêu chi tiết (Expenditure Items):** Mỗi mục chi phải ghi rõ tên hàng, giá dự kiến, số lượng, đơn vị, địa điểm mua. Staff kiểm tra trước khi duyệt.
2. **AI Audit (Perplexity):** Hệ thống tích hợp **Perplexity AI** để kiểm tra giá thị trường của các mặt hàng (`auditExpenditure`). Nếu giá khai cao hơn thị trường, AI sẽ cảnh báo.
3. **Minh chứng chi tiêu (Evidence):** Sau khi chi tiền, Fund Owner **bắt buộc** nộp hóa đơn/ảnh chứng minh. Hệ thống tạo `ExpenditureEvidence` với deadline.
4. **Thời hạn nộp minh chứng:** Configurable qua `EXPENDITURE_EVIDENCE_DEADLINE_HOURS` (mặc định 48h). Quá hạn thì bị phạt.
5. **Enforcement Scheduler:** Cron job chạy mỗi phút, kiểm tra evidence quá hạn:
   - **Đóng chiến dịch** (`closeCampaign`).
   - **Trừ điểm uy tín** (`OVERDUE_EVIDENCE`).
   - **Gửi thông báo** cảnh báo vi phạm.
6. **Ghi nhận tự động từ ngân hàng:** Khi Casso phát hiện giao dịch **chi** (amount < 0) từ tài khoản chiến dịch, hệ thống tự động tạo **yêu cầu nộp minh chứng** (`createEvidenceRequirement`).

## Q6.3: "Variance" là gì? Ý nghĩa?

**Trả lời:** `variance = totalReceivedAmount - totalAmount` (thực chi)
- **Variance > 0:** Còn dư tiền (chi ít hơn dự kiến).
- **Variance < 0:** Chi vượt mức (chi nhiều hơn dự kiến).
- **Variance = 0:** Chi đúng kế hoạch.

Hệ thống theo dõi variance để phát hiện bất thường. Nếu Fund Owner chi nhiều hơn kế hoạch, hệ thống cảnh báo.

## Q6.4: Refund (hoàn tiền) xử lý như thế nào?

**Trả lời:** Khi chi tiêu ít hơn số tiền nhận được:
1. Fund Owner gửi yêu cầu hoàn tiền (`createRefund`) với số tiền và bằng chứng chuyển khoản.
2. Hệ thống tạo `ExpenditureTransaction` với `type = "REFUND"`, `status = "COMPLETED"`.
3. **Cộng số tiền hoàn lại vào balance** của chiến dịch (`campaignService.updateBalance(campaign.getId(), amount)`).
4. Tiền hoàn được ghi nhận để đảm bảo minh bạch.

## Q6.5: ExpenditureCategory (Hạng mục) để làm gì?

**Trả lời:** Mỗi expenditure có thể chia thành nhiều **hạng mục** (categories), mỗi hạng mục có nhiều **items**:
```
Expenditure
  ├── Hạng mục "Lương thực"
  │     ├── Gạo 5kg - 150,000 x 100
  │     └── Mì tôm - 5,000 x 200
  ├── Hạng mục "Đồ dùng học tập"
  │     ├── Vở - 10,000 x 50
  │     └── Bút - 5,000 x 50
```
Mỗi hạng mục có `expectedAmount`, `actualAmount` để theo dõi chi tiết.

---

# 7. TRUST SCORE - ĐIỂM UY TÍN

## Q7.1: Trust Score là gì? Hoạt động như thế nào?

**Trả lời:** Trust Score là **hệ thống chấm điểm uy tín** cho Fund Owner, giúp cộng đồng đánh giá mức độ đáng tin cậy.

**Cơ chế:**
- Mỗi hành vi tốt/xấu được **cộng/trừ điểm** theo cấu hình (`TrustScoreConfig`).
- Điểm được lưu trong `User.trustScore` (Identity Service) và log chi tiết trong `TrustScoreLog` (Campaign Service).

**Các quy tắc chấm điểm:**

| Rule Key | Mô tả | Điểm |
|----------|-------|------|
| `CAMPAIGN_APPROVED` | Chiến dịch được duyệt | +điểm |
| `CAMPAIGN_REJECTED` | Chiến dịch bị từ chối | -điểm |
| `ON_TIME_SUBMIT` | Nộp minh chứng đúng hạn | +điểm |
| `LATE_SUBMIT` | Nộp minh chứng muộn | -điểm |
| `OVERDUE_EVIDENCE` | Quá hạn nộp minh chứng | -điểm (nặng) |
| `DAILY_POST` | Viết bài hàng ngày | +điểm |

## Q7.2: Admin có thể tùy chỉnh điểm không?

**Trả lời:** Có, qua API `PUT /api/trust-score/config/{ruleKey}`:
- Thay đổi số điểm cho mỗi rule.
- Bật/tắt rule (`isActive`).
- Sửa tên và mô tả rule.
- Hệ thống dùng **cache** (`ConcurrentHashMap`) để tăng performance, tự động invalidate khi cập nhật.

## Q7.3: Chống trùng Trust Score như thế nào?

**Trả lời:** Hệ thống có **duplicate check** trước khi cộng điểm:
- `DAILY_POST`: Mỗi user chỉ được cộng 1 lần/ngày. Kiểm tra bằng `existsByUserIdAndRuleKeyAndCreatedAtAfter(userId, ruleKey, startOfDay)`.
- Các rule khác: Kiểm tra theo `referenceId` (campaignId/expenditureId). Nếu đã chấm điểm cho reference này thì skip.

## Q7.4: Trust Score có Leaderboard không?

**Trả lời:** Có. API `GET /api/trust-score/leaderboard` trả về danh sách user xếp theo điểm cao nhất. Frontend hiển thị ranking, avatar, tên.

---

# 8. FLAG & TỐ CÁO

## Q8.1: Cơ chế tố cáo (Flag) hoạt động như thế nào?

**Trả lời:**
1. User gửi report (`POST /api/flags`) với `campaignId` hoặc `postId` và `reason`.
2. **Chống trùng:** Mỗi user chỉ được flag 1 lần cho mỗi campaign/post. Nếu đã flag thì throw `IllegalStateException`.
3. Hệ thống tạo `ApprovalTask` và assign cho staff.
4. **Gửi notification cho follower:** Nếu flag campaign, tất cả người follow chiến dịch nhận được cảnh báo.
5. Staff review flag, set status `RESOLVED` hoặc giữ `PENDING`.
6. **Gửi notification cho người tố cáo:** Thông báo kết quả xử lý.

## Q8.2: Khi chiến dịch bị tố cáo nhiều lần, hệ thống xử lý như thế nào?

**Trả lời:** Hiện tại hệ thống **không tự động đóng** chiến dịch khi đạt số lượng flag nhất định. Staff cần **thủ công** review và quyết định:
- Pause chiến dịch (`PUT /api/campaigns/{id}/pause`).
- Đóng chiến dịch (`PUT /api/campaigns/{id}/close`).

**Gợi ý cải thiện:** Có thể thêm threshold tự động (ví dụ: 10+ flag -> tự động pause để review).

---

# 9. FEED POST & CỘNG ĐỒNG

## Q9.1: Feed Post là gì?

**Trả lời:** Hệ thống có tính năng **diễn đàn/blog** để:
- Fund Owner cập nhật tiến độ chiến dịch.
- Cộng đồng chia sẻ ý kiến, kinh nghiệm.

**Tính năng:**
- CRUD bài viết với trạng thái (draft, published, archived).
- **Like** và **Comment** với kiểm soát trùng lặp.
- **Revision History:** Mỗi lần sửa bài, hệ thống lưu **phiên bản cũ** để audit.
- **Pin/Lock:** Admin có thể ghim bài viết hoặc khóa bình luận.
- **View Count** và **User Post Seen** để theo dõi lượt xem.

## Q9.2: Tại sao cần Feed Post trong hệ thống quỹ thiện nguyện?

**Trả lời:**
- **Minh bạch:** Fund Owner đăng bài cập nhật tiến độ, ảnh chứng minh việc sử dụng tiền.
- **Cộng đồng:** Donor và người theo dõi có thể bình luận, hỏi đáp trực tiếp.
- **Trust:** Hoạt động thường xuyên trên Feed tăng Trust Score (`DAILY_POST`).
- **Giá trị thực tế:** Giống cách GoFundMe, Kitabisa.com có tính năng "Updates" để chủ quỹ cập nhật.

---

# 10. NOTIFICATION & REAL-TIME

## Q10.1: Hệ thống thông báo hoạt động như thế nào?

**Trả lời:** Notification Service xử lý 2 kênh:

1. **Real-time (Pusher):** Push notification tức thời đến frontend qua WebSocket (Pusher Channels).
2. **Email (Gmail SMTP):** Gửi email cho các sự kiện quan trọng (KYC, OTP, commitment...).

**Các loại notification:**
- `CAMPAIGN_APPROVED` / `CAMPAIGN_REJECTED`: Kết quả duyệt chiến dịch.
- `EXPENDITURE_APPROVED` / `EXPENDITURE_REJECTED` / `EXPENDITURE_DISBURSED`: Kết quả chi tiêu.
- `EVIDENCE_APPROVED` / `EVIDENCE_REJECTED`: Kết quả duyệt minh chứng.
- `KYC_APPROVED` / `KYC_REJECTED`: Kết quả KYC.
- `CAMPAIGN_FLAGGED`: Chiến dịch bị tố cáo.
- `FLAG_REVIEWED`: Kết quả xử lý tố cáo.
- `CAMPAIGN_LOCKED_OVERDUE`: Quỹ bị đóng do vi phạm.
- `EXPENDITURE_EVIDENCE_REQUIRED`: Yêu cầu nộp minh chứng (tự động từ Casso).

## Q10.2: Frontend nhận notification như thế nào?

**Trả lời:** Frontend (danbox) có component `NotificationBell` và `WalletDropdown`:
- Đăng ký kênh Pusher theo `userId`.
- Khi có notification mới, hiển thị badge và danh sách notification.
- Click vào notification để đi đến trang tương ứng.

---

# 11. CHAT SERVICE

## Q11.1: Chat Service cung cấp tính năng gì?

**Trả lời:**
- **Nhắn tin trực tiếp** giữa user và staff/fund owner.
- **Chat theo chiến dịch:** Mỗi chiến dịch có phòng chat riêng (`/api/chat/conversations/campaign/{campaignId}`).
- **WebSocket (SockJS + STOMP):** Real-time messaging qua WebSocket.
- **Lịch hẹn (Appointments):** Staff có thể đặt lịch hẹn với fund owner để review chiến dịch trực tiếp.

## Q11.2: Tại sao cần Chat trong hệ thống quỹ thiện nguyện?

**Trả lời:**
- Staff cần liên lạc với Fund Owner để hỏi thêm thông tin khi duyệt chiến dịch/chi tiêu.
- Donor có thể hỏi trực tiếp Fund Owner về tiến độ.
- Lịch hẹn giúp staff tổ chức review trực tiếp (video call, gặp mặt) cho các chiến dịch lớn.

---

# 12. MEDIA SERVICE

## Q12.1: Media Service xử lý file như thế nào?

**Trả lời:** Media Service quản lý upload/download file:
- **Lưu trữ:** Supabase Storage (Object Storage, tương tự AWS S3).
- **Hỗ trợ:** Ảnh (JPEG, PNG, WebP), Video, PDF.
- **Giới hạn:** Max file size 50MB (cấu hình tại Gateway).
- **API:** `POST /api/media/upload`, `GET /api/media/{id}`, `DELETE /api/media/{id}`.
- **Liên kết:** File được liên kết với campaign (coverImage), KYC (idImageFront/Back, selfieImage), feed post, evidence...

---

# 13. AI INTEGRATION

## Q13.1: Hệ thống tích hợp AI như thế nào?

**Trả lời:** TrustFundMe tích hợp **AI Service** (tách riêng, chạy trên port 7000) cho các chức năng:

| Endpoint | Chức năng |
|----------|----------|
| `/api/generate-description` | AI tạo mô tả chiến dịch từ thông tin cơ bản |
| `/api/parse-expenditure-excel` | Đọc file Excel kế hoạch chi tiêu |
| `/api/ocr-kyc` | Đọc thông tin từ ảnh CCCD (OCR) |
| `/api/analyze-flag` | Phân tích báo cáo vi phạm |
| `/api/analyze-expenditure` | Audit giá thị trường của vật phẩm |
| `/api/analyze-evidence` | Phân tích minh chứng chi tiêu |
| `/api/generate-suggestion-labels` | Gợi ý nhãn cho vật phẩm |
| `/api/generate-post` | Tạo nội dung bài viết |

## Q13.2: Perplexity AI dùng để làm gì?

**Trả lời:** Perplexity AI được dùng để **audit giá thị trường**:
- Khi staff duyệt kế hoạch chi tiêu, hệ thống gửi danh sách vật phẩm (tên, giá khai báo, số lượng) cho Perplexity.
- Perplexity trả về phân tích: giá khai báo có hợp lý không, giá thị trường là bao nhiêu, có bất thường không.
- Giúp staff phát hiện trường hợp Fund Owner khai giá cao để cắt bớt tiền.

## Q13.3: AI Prompt được lưu ở đâu?

**Trả lời:** Trong `campaign-service/src/main/resources/prompts/`:
- `ai_bill_analysis_prompt.txt`: Phân tích hóa đơn.
- `ai_campaign_description_prompt.txt`: Tạo mô tả chiến dịch.
- `ai_flag_analysis_prompt.txt`: Phân tích báo cáo vi phạm.
- `ai_market_analysis_prompt.txt`: Phân tích giá thị trường.
- `ai_ocr_prompt.json`: OCR CCCD.

Admin có thể **chỉnh sửa prompt AI** từ giao diện web (`/admin/promt-AI`).

---

# 14. QUỸ CHUNG (General Fund)

## Q14.1: General Fund là gì? Hoạt động như thế nào?

**Trả lời:** General Fund (Quỹ chung) là **quỹ trung tâm** của hệ thống:
- **ID cố định = 1**, `type = "GENERAL_FUND"`.
- **Không hiển thị** trong danh sách chiến dịch thường (filter `typeNot(GENERAL_FUND)`).
- **Chỉ admin** có quyền thao tác.

**Nguồn thu:**
- **Tip:** Tiền ủng hộ hệ thống từ donor.
- **Recovery:** Số dư thu hồi khi đóng chiến dịch.
- **Điều chuyển từ các chiến dịch.**

**Chi tiêu:**
- **Hỗ trợ chiến dịch:** Chuyển tiền từ quỹ chung đến chiến dịch cần hỗ trợ.
- **Chi phí vận hành:** (nếu có).

**Giao dịch nội bộ (Internal Transaction):**
```
Admin tạo giao dịch -> PENDING -> (Duyệt) -> APPROVED/COMPLETED -> Chuyển tiền giữa các quỹ
```
Hệ thống kiểm tra số dư trước khi chuyển (`balance >= amount`).

## Q14.2: Tại sao cần General Fund?

**Trả lời:**
- Đảm bảo **không có tiền bị mất:** Khi đóng chiến dịch, số dư về General Fund.
- **Minh bạch:** Mọi giao dịch nội bộ đều được ghi nhận, có staff ID, evidence.
- **Hỗ trợ liên chiến dịch:** General Fund có thể chuyển tiền đến chiến dịch khẩn cấp.

---

# 15. BẢO MẬT & VULNERABILITY

## Q15.1: Hệ thống có bị SQL Injection không?

**Trả lời:** **Không**, vì:
- Sử dụng **Spring Data JPA** với **Named Parameters** và **JPQL**. Spring tự động parameterize tất cả query.
- Không có raw SQL nào sử dụng string concatenation với input của user.
- Dữ liệu input được validate qua **Jakarta Validation** (`@Valid`, `@NotNull`, `@Size`...).

## Q15.2: Hệ thống có bị XSS không?

**Trả lời:**
- Backend chỉ trả về **JSON**, không render HTML -> ít rủi ro XSS từ backend.
- Frontend (Next.js) tự động **escape** tất cả nội dung trong JSX.
- Các trường như `description`, `reason`, `content` được lưu và hiển thị như text, không render HTML.
- **Cần lưu ý:** Nếu có rich text editor (bài viết), cần sanitize HTML trước khi lưu.

## Q15.3: CORS được cấu hình như thế nào?

**Trả lời:** CORS cấu hình tại **Gateway level**:
```properties
spring.cloud.gateway.globalcors.cors-configurations[/**].allowed-origin-patterns=${ALLOWED_ORIGINS:http://localhost:3000}
spring.cloud.gateway.globalcors.cors-configurations[/**].allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS
spring.cloud.gateway.globalcors.cors-configurations[/**].allow-credentials=true
```
- Chỉ cho phép origin từ frontend (localhost:3000 hoặc domain production).
- Chỉ cho phép các HTTP methods cần thiết.
- Mỗi service còn có `CorsConfig` riêng để thêm lớp bảo vệ.

## Q15.4: API Internal (giữa các service) có bị truy cập trái phép không?

**Trả lời:** Đây là **điểm yếu** hiện tại:
- Các endpoint `/api/internal/**` được để **public** trong Gateway filter để cho phép service-to-service communication.
- Điều này có nghĩa là bất kỳ ai biết URL cũng có thể gọi các API này.

**Cách khắc phục (gợi ý):**
1. Dùng **API Key/Secret** riêng cho internal communication.
2. Cấu hình network: chỉ cho phép traffic internal từ các container trong cùng Docker network.
3. Sử dụng **mutual TLS (mTLS)** giữa các service.

## Q15.5: Password được lưu như thế nào?

**Trả lời:** Password được hash bằng **BCrypt** (`PasswordEncoder`):
- BCrypt tự động thêm **salt** (không cần lưu salt riêng).
- Cost factor mặc định = 10 (2^10 = 1024 rounds).
- Không thể brute-force hoặc rainbow table attack.

## Q15.6: JWT Secret có an toàn không?

**Trả lời:**
- JWT Secret được lưu trong **biến môi trường** (`JWT_SECRET`), không hardcode trong code.
- File `.env` được thêm vào `.gitignore`, không push lên Git.
- Tuy nhiên, code có **fallback value**: `TrustFundME2024SecretKeyForJWTTokenGenerationSecureRandomString64Chars`. Đây chỉ là giá trị mặc định cho development, **production phải set biến môi trường riêng**.
- Secret dài 64 ký tự, đủ mạnh cho HMAC-SHA.

## Q15.7: Hệ thống có chống CSRF không?

**Trả lời:**
- Hệ thống dùng **JWT Bearer Token** (không dùng Cookie-based session) -> **immune với CSRF** vì browser không tự động gửi Authorization header.
- Spring Security cấu hình `csrf().disable()` vì đã dùng JWT.

## Q15.8: File upload có bị khai thác không?

**Trả lời:**
- **Giới hạn kích thước:** Max 50MB (cấu hình tại Gateway).
- **Lưu trữ:** File upload lên **Supabase Storage** (không lưu local), giảm rủi ro path traversal.
- **Cần cải thiện:** Nên validate file type (chỉ cho ảnh/PDF), chống upload file độc hại (.exe, .php).

---

# 16. FRONTEND (DANBOX - NEXT.JS)

## Q16.1: Frontend được xây dựng bằng công nghệ gì?

**Trả lời:** Frontend là ứng dụng web **Next.js** (React framework) với:
- **Next.js App Router** (folder-based routing).
- **TypeScript** cho type safety.
- **Tailwind CSS** cho styling.
- **API Routes** (`/app/api/`) làm proxy giữa browser và backend.

## Q16.2: Tại sao frontend dùng API Routes làm proxy?

**Trả lời:** Frontend có các **API routes** (`/app/api/auth/login/route.ts`, `/app/api/flags/route.ts`...) làm **proxy** gọi backend:
- **Ẩn URL backend:** Browser chỉ thấy `/api/auth/login`, không thấy URL của backend service.
- **Xử lý session:** Lưu JWT trong cookie HttpOnly qua API route (an toàn hơn localStorage).
- **CORS:** Không bị CORS issue vì browser gọi cùng origin (Next.js server).

## Q16.3: Frontend có những trang chính nào?

**Trả lời:**

| Route | Chức năng |
|-------|----------|
| `/` | Trang chủ, hiển thị chiến dịch nổi bật |
| `/campaigns` | Danh sách chiến dịch |
| `/campaigns-details?id=X` | Chi tiết chiến dịch |
| `/campaign-creation` | Tạo chiến dịch mới |
| `/donation?campaignId=X` | Trang donate |
| `/sign-in` | Đăng nhập |
| `/account/profile` | Quản lý tài khoản |
| `/account/campaigns` | Các chiến dịch của tôi |
| `/account/campaigns/expenditures` | Quản lý chi tiêu |
| `/staff/*` | Dashboard staff (duyệt KYC, chiến dịch, chi tiêu, flag) |
| `/admin/*` | Dashboard admin (quản lý user, cấu hình, quỹ chung) |
| `/post/*` | Diễn đàn, bài viết |

## Q16.4: Component nào quan trọng nhất trên frontend?

**Trả lời:**
- `ProtectedRoute`: Bảo vệ route cần đăng nhập.
- `RequireRole`: Kiểm tra role trước khi render UI.
- `BannedAccountWrapper`: Hiển thị UI hạn chế cho user bị ban.
- `EmailVerificationBanner`: Nhắc user xác thực email.
- `EnforcementAlertBanner`: Cảnh báo vi phạm minh chứng chi tiêu.
- `NotificationBell`: Hiển thị notification real-time.
- `Wallet` / `WalletDropdown`: Hiển thị số dư và lịch sử giao dịch.
- `CampaignCard`: Card hiển thị thông tin chiến dịch.
- `CampaignDonateCard`: Form donate.
- `OtpInput`: Nhập OTP 6 số.

---

# 17. DEPLOYMENT & DEVOPS

## Q17.1: Hệ thống deploy như thế nào?

**Trả lời:**
- **Docker Compose:** File `docker-compose.yml` định nghĩa tất cả services, có thể chạy `docker-compose up` để start toàn bộ hệ thống.
- **Dockerfile:** Mỗi service có Dockerfile riêng để build image.
- **Database:** MySQL 8.0 chạy trong Docker container, data lưu trên volume `mysql_data`.
- **CI/CD (Frontend):** Danbox có `.github/workflows/ci.yml` và `cd.yml` cho CI/CD tự động.

## Q17.2: Môi trường development chạy như thế nào?

**Trả lời:**
- **PowerShell scripts:** Mỗi service có script riêng (`run-campaign-service.ps1`, `run-identity-service.ps1`...) để chạy local.
- **`run-all-services.ps1`:** Chạy tất cả services cùng lúc.
- **H2 Database:** Môi trường dev có thể dùng H2 (in-memory DB) thay vì MySQL (`run-identity-service-h2.ps1`).
- **Hot Reload:** Spring Boot DevTools cho phép restart nhanh khi code thay đổi.

---

# 18. DATABASE DESIGN

## Q18.1: Hệ thống dùng cơ sở dữ liệu gì? Tại sao?

**Trả lời:** **MySQL 8.0** - Relational Database.

**Tại sao chọn MySQL?**
- **ACID compliance:** Giao dịch tài chính cần đảm bảo **Atomicity, Consistency, Isolation, Durability**.
- **Quan hệ phức tạp:** Các entity có nhiều quan hệ (User -> Campaign -> Expenditure -> Items).
- **JPA/Hibernate:** Spring Data JPA hỗ trợ MySQL tốt nhất.
- **Production-ready:** MySQL là RDBMS phổ biến, community lớn, nhiều tài liệu.

**Tại sao không dùng MongoDB?**
- Hệ thống quỹ thiện nguyện cần **tính nhất quán** (consistency) cao cho giao dịch tài chính. MongoDB (NoSQL) ưu tiên availability và partition tolerance (AP trong CAP theorem), không phù hợp cho nghiệp vụ tài chính.

## Q18.2: Mỗi service dùng database riêng hay chung?

**Trả lời:** **Mỗi service có database riêng** (Database per Service pattern):
- `trustfundme_identity_db`: User, KYC, BankAccount, Module.
- `trustfundme_campaign_db`: Campaign, Expenditure, FeedPost, Flag, TrustScore.
- `trustfundme_payment_db`: Donation, DonationItem, CassoTransaction.
- `trustfundme_chat_db`: Conversation, Message.
- `trustfundme_notification_db`: Notification.
- `trustfundme_media_db`: Media.

**Tại sao?** Đảm bảo **loose coupling**: mỗi service độc lập, có thể thay đổi schema mà không ảnh hưởng service khác.

## Q18.3: Các trường tiền được lưu như thế nào?

**Trả lời:** Tất cả trường tiền dùng **`BigDecimal`** (Java) và **`DECIMAL(19,4)`** (MySQL):
- Tránh lỗi làm tròn của `float`/`double`.
- Precision 19 cho phép lưu số lên đến hàng triệu tỷ.
- Scale 4 cho phép 4 chữ số thập phân.

---

# 19. TESTING

## Q19.1: Hệ thống có test không?

**Trả lời:** Có, hệ thống có **Unit Test** và **Integration Test**:

**Identity Service:**
- `AuthControllerTest`: Test đăng ký, đăng nhập, refresh token.
- `AuthServiceImplTest`: Test logic xác thực.
- `UserServiceImplTest`: Test CRUD user.
- `UserKYCServiceImplTest`: Test quy trình KYC.
- `BankAccountServiceImplTest`: Test bank account CRUD.
- `EmailServiceImplTest`: Test gửi email.
- Repository tests: `UserRepositoryTest`, `OtpTokenRepositoryTest`, `BankAccountRepositoryTest`.

**Campaign Service:**
- Repository tests: `ApprovalTaskRepositoryTest`, `CampaignFollowRepositoryTest`, `ExpenditureRepositoryTest`, `FeedPostLikeRepositoryTest`.

**Payment Service:**
- `DonationServiceTest`: Test tạo donation, payment flow.
- `DonationRepositoryTest`: Test query donation.

## Q19.2: Test coverage như thế nào?

**Trả lời:** Hệ thống tập trung test vào:
- **Service layer:** Logic nghiệp vụ chính (auth, KYC, donation).
- **Repository layer:** Custom queries (JPA Named Queries, Native Queries).
- **Controller layer:** Endpoint behavior (AuthControllerTest).

**Cần cải thiện:** Thêm integration test cho cross-service communication (Payment -> Campaign balance update).

---

# 20. BAD CASE & TÌNH HUỐNG XẤU

## Q20.1: Fund Owner lừa đảo - Tạo chiến dịch giả để lấy tiền. Xử lý như thế nào?

**Trả lời:** Nhiều lớp bảo vệ:
1. **KYC bắt buộc:** Fund Owner phải xác minh CCCD/hộ chiếu, face verification. Hệ thống có full thông tin cá nhân để truy cứu.
2. **Staff duyệt:** Mỗi chiến dịch phải qua staff review trước khi được donate.
3. **Kế hoạch chi tiêu chi tiết:** Fund Owner phải liệt kê từng mục chi, giá, số lượng. Staff kiểm tra tính hợp lý.
4. **AI Audit giá:** Perplexity AI kiểm tra giá khai báo với giá thị trường.
5. **Minh chứng bắt buộc:** Sau khi chi tiền, phải nộp hóa đơn/ảnh.
6. **Enforcement:** Quá hạn nộp minh chứng -> đóng quỹ, thu hồi tiền, trừ Trust Score.
7. **Flag cộng đồng:** Bất kỳ ai cũng có thể tố cáo chiến dịch. Follower nhận thông báo ngay.
8. **Trust Score:** Lịch sử uy tín công khai, Fund Owner có điểm thấp sẽ khó nhận được donate.

## Q20.2: Hai Fund Owner dùng chung CCCD để đăng ký. Hệ thống có phát hiện?

**Trả lời:** **Có.** Trong `submitKYC()`:
```java
userKYCRepository.findFirstByIdNumber(request.getIdNumber()).ifPresent(existing -> {
    if (!existing.getUser().getId().equals(userId)) {
        throw new BadRequestException("Số định danh đã được đăng ký bởi tài khoản khác");
    }
});
```
Hệ thống kiểm tra số định danh (CCCD) trước khi cho nộp KYC. Nếu trùng với user khác -> từ chối.

## Q20.3: Donor chuyển tiền nhưng ghi sai nội dung. Tiền có bị mất không?

**Trả lời:** **Không.** Hệ thống có fallback:
- Casso Webhook nhận được **tất cả giao dịch** vào tài khoản liên kết với chiến dịch.
- Nếu nội dung không match với `TF {donationId}`, hệ thống tạo **Anonymous Donation** tự động.
- Tiền vẫn được cộng vào balance của chiến dịch (dựa trên tài khoản nhận).

## Q20.4: Hệ thống payment bị sập giữa lúc xử lý giao dịch. Tiền có bị mất?

**Trả lời:**
- **Casso Transaction lưu trước:** Hệ thống lưu `CassoTransaction` vào DB trước khi xử lý tiếp. Nếu bị crash, dữ liệu vẫn còn.
- **Dedup bằng tid:** Khi restart, nếu webhook gửi lại cùng transaction, hệ thống skip vì `existsByTid(tid) = true`.
- **Balance sync flag:** `isBalanceSynchronized` đảm bảo không cộng tiền 2 lần.
- **Transaction trong DB:** Mọi giao dịch đều lưu vết, có thể đối soát thủ công.

## Q20.5: 2 người donate cùng lúc vào cùng 1 vật phẩm (ITEMIZED). Số lượng có bị tràn?

**Trả lời:**
- Trước khi tạo donation, frontend gọi `checkExpenditureItemLimit()` để kiểm tra `quantityLeft`.
- Khi tạo donation, hệ thống **trừ ngay** `quantityLeft` (immediate deduction).
- Tuy nhiên, có **race condition** giữa lúc check và lúc trừ: 2 request check cùng lúc cả 2 thấy đủ hàng -> cả 2 tạo donation -> vượt số lượng.

**Cách giảm thiểu:**
- Hệ thống dùng `@Transactional` để đảm bảo atomicity.
- Có thể cải thiện bằng **pessimistic locking** (`SELECT ... FOR UPDATE`) trên `quantityLeft`.

## Q20.6: Casso Webhook không hoạt động (offline/network issue). Donation bị treo PENDING?

**Trả lời:** Có cơ chế backup:
- **PaymentCleanupTask:** Cron job tự động chuyển PENDING -> FAILED sau X phút.
- **Manual sync:** Frontend có thể gọi `POST /api/payments/donations/{id}/sync-balance` để đồng bộ thủ công.
- **PayOS Verify:** Gọi `GET /api/payments/donations/{id}/verify` để kiểm tra trạng thái trên PayOS và đồng bộ.

## Q20.7: Staff thông đồng với Fund Owner để duyệt sai. Xử lý như thế nào?

**Trả lời:**
- **Approval Task:** Hệ thống ghi nhận `staffId` của người duyệt, có thể truy vết.
- **Random Assignment:** Task được gán **ngẫu nhiên** cho staff, giảm khả năng thông đồng với 1 staff cụ thể.
- **Reassignment:** Admin có thể reassign task cho staff khác.
- **Audit Trail:** Mọi hành vi duyệt đều được ghi nhận (approvedByStaff, approvedAt).

**Cần cải thiện:** Thêm quy tắc 2 người duyệt (4 eyes principle) cho chiến dịch lớn.

## Q20.8: DDoS attack vào API. Hệ thống có chịu được không?

**Trả lời:** Hiện tại **chưa có rate limiting** tại Gateway. Các biện pháp cần thêm:
- **Rate Limiting:** Dùng Spring Cloud Gateway filter hoặc Redis-based rate limiter.
- **WAF (Web Application Firewall):** Khi deploy lên cloud, dùng Cloudflare/AWS WAF.
- **Auto Scaling:** Docker Swarm/K8s có thể tự động scale thêm instance.

## Q20.9: Data leak - Database bị truy cập trái phép. Rủi ro gì?

**Trả lời:** Rủi ro:
- **Password:** Đã hash BCrypt -> **an toàn**, không thể giải mã.
- **JWT Secret:** Nếu lộ thì attacker có thể giả mạo token -> phải thay secret và revoke tất cả token.
- **CCCD, ảnh KYC:** Dữ liệu nhạy cảm, chưa encrypt -> **rủi ro cao**.
- **Bank Account:** Số tài khoản, tên chủ tài khoản -> **rủi ro trung bình**.

**Cần cải thiện:** Encrypt dữ liệu nhạy cảm (CCCD, bank info) trước khi lưu DB.

## Q20.10: Fund Owner rút tiền nhưng không mua hàng, không nộp minh chứng. Xử lý như thế nào?

**Trả lời:** Đây là **tình huống nghiêm trọng nhất**. Hệ thống xử lý:
1. **Evidence Deadline:** Khi giải ngân, hệ thống đặt deadline nộp minh chứng (mặc định 48h).
2. **Casso tự động detect:** Khi tài khoản chi tiền, Casso báo webhook -> hệ thống tự động tạo yêu cầu nộp minh chứng.
3. **Enforcement Scheduler:** Chạy mỗi phút kiểm tra evidence quá hạn.
4. **Xử phạt:**
   - **ĐÓNG chiến dịch** (`closeCampaign`).
   - **THU HỒI số dư** về General Fund.
   - **TRỪ điểm uy tín** (`OVERDUE_EVIDENCE`).
   - **GỬI thông báo** cảnh báo cho Fund Owner.
5. **Cộng đồng:** Donor và follower có thể tố cáo chiến dịch.
6. **Pháp lý:** Hệ thống đã có CCCD, địa chỉ của Fund Owner qua KYC. Có thể phối hợp với cơ quan chức năng nếu cần.

## Q20.11: Nếu admin bị hack, thay đổi tất cả dữ liệu thì sao?

**Trả lời:**
- Admin có quyền cao nhất, đây là rủi ro lớn.
- **Biện pháp giảm thiểu:**
  - Bắt buộc **2FA** cho tài khoản admin (tương lai).
  - **Audit Log:** Ghi nhận mọi hành vi của admin (hiện tại có trong DB qua `createdByStaffId`, `approvedByStaff`).
  - **Backup:** Backup database định kỳ.
  - **Phân quyền chi tiết:** Tách quyền admin thành nhiều cấp (super admin, admin, staff).

## Q20.12: Người donate yêu cầu hoàn tiền. Xử lý như thế nào?

**Trả lời:** Hiện tại hệ thống **chưa hỗ trợ hoàn tiền cho donor** (refund to donor). Lý do:
- Tiền đã chuyển trực tiếp vào tài khoản ngân hàng của chiến dịch, hệ thống không giữ tiền.
- Việc hoàn tiền cần thương lượng giữa donor và Fund Owner.
- Hệ thống chỉ hỗ trợ **refund từ Fund Owner về quỹ chung** (khi chi ít hơn dự kiến).

**Gợi ý:** Thêm chính sách refund rõ ràng và tính năng yêu cầu hoàn tiền qua hệ thống.

## Q20.13: Nếu Supabase (lưu trữ file) bị sập, ảnh KYC/chiến dịch bị mất?

**Trả lời:**
- Supabase hỗ trợ **backup** và **replication** ở tầng infrastructure.
- Tuy nhiên, hệ thống chưa có **local backup** của file.
- **Cần cải thiện:** Lưu backup ảnh KYC trên cloud thứ 2 (AWS S3 backup) hoặc trên hệ thống nội bộ.

---

# 21. TẠI SAO CHỌN CÔNG NGHỆ NÀY

## Q21.1: Tại sao chọn Java Spring Boot?

**Trả lời:**
- **Enterprise-grade:** Spring Boot là framework phổ biến nhất cho Java microservices.
- **Ecosystem phong phú:** Spring Cloud (Gateway, Eureka), Spring Data JPA, Spring Security - tất cả tích hợp sẵn.
- **Type-safe:** Java là strongly-typed language, phát hiện lỗi sớm.
- **Performance:** JVM đã được tối ưu qua nhiều năm, xử lý concurrent tốt với thread pool.
- **Community:** Tài liệu, câu hỏi trên StackOverflow, cộng đồng lớn.
- **Phù hợp với môi trường học thuật:** Đã học Java/Spring trong trường, tận dụng kiến thức sẵn có.

## Q21.2: Tại sao chọn Next.js cho frontend?

**Trả lời:**
- **SSR (Server-Side Rendering):** SEO tốt cho trang chiến dịch công khai.
- **API Routes:** Làm proxy backend, xử lý cookie an toàn.
- **React ecosystem:** Component-based, tái sử dụng code.
- **Performance:** Automatic code splitting, image optimization.
- **TypeScript:** Type safety, IntelliSense tốt.
- **Vercel deploy:** Deploy frontend miễn phí, nhanh chóng.

## Q21.3: Tại sao chọn MySQL thay vì PostgreSQL?

**Trả lời:**
- MySQL và PostgreSQL đều là RDBMS tốt. Chọn MySQL vì:
  - **Phổ biến hơn** ở Việt Nam (hosting, tài liệu tiếng Việt).
  - **XAMPP/Laragon:** Dễ setup local cho development.
  - **Docker image nhẹ:** MySQL Docker image nhỏ hơn PostgreSQL.
  - **JPA tương thích:** Spring Data JPA hỗ trợ MySQL tốt.
- Nếu làm lại, PostgreSQL cũng là lựa chọn tốt (hỗ trợ JSON, full-text search tốt hơn).

## Q21.4: Tại sao chọn Eureka thay vì Consul/Kubernetes Service Discovery?

**Trả lời:**
- **Tích hợp sẵn** với Spring Cloud: chỉ cần thêm dependency, annotation là xong.
- **Học và dùng nhanh:** Eureka đơn giản hơn Consul (không cần setup thêm agent).
- **Phù hợp quy mô nhỏ:** Đồ án sinh viên với 10 services, Eureka đủ đáp ứng.
- Nếu quy mô lớn hơn (100+ services), nên chuyển sang **Kubernetes Service Discovery** hoặc **Consul**.

## Q21.5: Tại sao chọn Pusher thay vì Firebase Cloud Messaging?

**Trả lời:**
- **WebSocket đơn giản:** Pusher Channels hỗ trợ real-time push notification qua WebSocket với SDK dễ dùng.
- **Không cần mobile app:** FCM tốt cho mobile push, nhưng frontend là web -> Pusher phù hợp hơn.
- **Free tier:** Pusher có free tier đủ cho đồ án (200 concurrent connections, 200,000 messages/day).

---

# 22. SO SÁNH VỚI CÁC HỆ THỐNG KHÁC

## Q22.1: TrustFundMe khác gì GoFundMe?

**Trả lời:**

| Tiêu chí | GoFundMe | TrustFundMe |
|----------|----------|-------------|
| **KYC** | Không bắt buộc | Bắt buộc CCCD + face verify |
| **Minh bạch chi tiêu** | Không có | Kế hoạch chi tiết, AI audit, minh chứng bắt buộc |
| **Giám sát ngân hàng** | Không | Tự động qua Casso webhook |
| **Trust Score** | Không | Có hệ thống điểm uy tín |
| **Flag** | Có báo cáo | Có báo cáo + thông báo follower + AI phân tích |
| **Loại chiến dịch** | Chỉ tiền mặt | ITEMIZED (vật phẩm) + AUTHORIZED (tiền) |
| **Enforcement** | Thủ công | Tự động đóng quỹ khi vi phạm |

## Q22.2: TrustFundMe khác gì GiveNow/Kitabisa?

**Trả lời:**
- **GiveNow/Kitabisa** là nền tảng trung gian: tiền gửi vào nền tảng, nền tảng giải ngân.
- **TrustFundMe** là nền tảng **trực tiếp**: tiền vào thẳng tài khoản ngân hàng của chiến dịch, hệ thống giám sát qua Casso.
- **Lợi thế TrustFundMe:** Không giữ tiền, giảm rủi ro pháp lý. Fund Owner nhận tiền trực tiếp, nhanh hơn.
- **Bất lợi:** Khó kiểm soát hơn vì không giữ tiền. Giải quyết bằng KYC + Evidence + Enforcement.

---

# 23. SCALABILITY & PERFORMANCE

## Q23.1: Hệ thống có thể scale như thế nào?

**Trả lời:**
- **Horizontal Scaling:** Mỗi service có thể chạy nhiều instance. Eureka + Gateway load balance tự động.
- **Docker Compose -> Docker Swarm/K8s:** Dễ dàng chuyển sang container orchestration.
- **Database Scaling:** MySQL Replication (read replica) cho các query đọc nhiều (Campaign list, Feed Post).
- **Cache:** Campaign Service có `CacheConfig` cho cache kết quả thông dụng. Có thể thêm Redis.

## Q23.2: Hệ thống có những bottleneck nào?

**Trả lời:**
1. **N+1 Query:** `toCampaignResponse()` gọi `identityServiceClient.getUserInfo()` và `mediaServiceClient.getMediaUrl()` cho mỗi campaign. Khi load danh sách 100 campaign -> 200 HTTP call.
   - **Cách fix:** Batch API (lấy nhiều user cùng lúc) hoặc cache.
2. **Synchronous REST Call:** Service-to-service communication đồng bộ. Nếu Identity Service chậm, Campaign Service cũng bị chậm.
   - **Cách fix:** Circuit Breaker (Resilience4j), Async message queue.
3. **Filter trong code Java:** `getByStatus()` và `getByCategoryId()` load **tất cả campaigns** rồi filter trong Java thay vì query DB có điều kiện.
   - **Cách fix:** Dùng `findByStatusAndTypeNot()` trong Repository.

## Q23.3: Hệ thống chịu được bao nhiêu người dùng đồng thời?

**Trả lời:** Với cấu hình hiện tại (1 instance mỗi service):
- **Gateway:** Spring WebFlux (non-blocking) -> xử lý hàng ngàn request/s.
- **Service:** Spring MVC default thread pool = 200 threads -> ~200 request đồng thời / service.
- **Database:** MySQL connection pool mặc định = 10 -> bottleneck ở đây.
- **Ước tính:** ~100-500 người dùng đồng thời (phụ thuộc độ phức tạp của query).

Với scaling (nhiều instance + connection pool lớn hơn + cache): có thể phục vụ hàng ngàn người dùng.

---

# PHỤ LỤC: CÂU HỎI THƯỜNG GẶP KHI BẢO VỆ

## Câu hỏi tổng quát:
1. "Em mô tả tổng quan hệ thống được không?" -> Xem Q1.1
2. "Tại sao chọn kiến trúc microservices?" -> Xem Q1.1
3. "Hệ thống có bao nhiêu service? Chức năng mỗi service?" -> Xem Q1.1
4. "Các service giao tiếp với nhau như thế nào?" -> Xem Q1.4
5. "Em dùng Design Pattern nào?" -> Xem Q1.5

## Câu hỏi về bảo mật:
6. "Hệ thống xác thực như thế nào?" -> Xem Q2.1
7. "Nếu token bị lộ thì sao?" -> Xem Q2.4
8. "Hệ thống có bị SQL Injection không?" -> Xem Q15.1
9. "Password lưu như thế nào?" -> Xem Q15.5

## Câu hỏi về nghiệp vụ:
10. "Mô tả quy trình tạo chiến dịch" -> Xem Q3.1
11. "Làm sao đảm bảo Fund Owner không tham ô?" -> Xem Q20.1, Q6.2
12. "Thanh toán hoạt động như thế nào?" -> Xem Q4.1
13. "KYC là gì? Tại sao cần?" -> Xem Q5.1, Q5.3
14. "Trust Score hoạt động như thế nào?" -> Xem Q7.1

## Câu hỏi về bad case:
15. "2 người donate cùng lúc thì sao?" -> Xem Q20.5
16. "Fund Owner không nộp minh chứng thì sao?" -> Xem Q20.10
17. "Webhook không hoạt động thì sao?" -> Xem Q20.6
18. "Donor chuyển tiền sai nội dung thì sao?" -> Xem Q20.3
19. "Hệ thống bị sập giữa giao dịch thì sao?" -> Xem Q20.4
20. "Staff thông đồng với Fund Owner thì sao?" -> Xem Q20.7

## Câu hỏi về công nghệ:
21. "Tại sao chọn Java Spring Boot?" -> Xem Q21.1
22. "Tại sao chọn MySQL?" -> Xem Q21.3
23. "Tại sao chọn Next.js?" -> Xem Q21.2
24. "Hệ thống có thể scale không?" -> Xem Q23.1

## Câu hỏi về AI:
25. "AI được dùng để làm gì trong hệ thống?" -> Xem Q13.1
26. "Perplexity AI audit giá như thế nào?" -> Xem Q13.2

## Câu hỏi về so sánh:
27. "Hệ thống của em khác GoFundMe như thế nào?" -> Xem Q22.1
28. "Ưu điểm nổi bật nhất của hệ thống là gì?" -> Mục đích minh bạch: KYC bắt buộc + Kế hoạch chi tiết + AI audit + Minh chứng bắt buộc + Enforcement tự động + Trust Score + Flag cộng đồng. Đây là hệ thống quỹ thiện nguyện **đầu tiên** kết hợp tất cả các cơ chế này.

---

# LƯU Ý QUAN TRỌNG KHI BẢO VỆ

1. **Không hoảng khi bị hỏi câu khó.** Nếu không biết, nói: "Em chưa trình bày trong scope đồ án, nhưng hướng giải quyết là..." rồi gợi ý cách làm.
2. **Luôn liên hệ code thực tế.** Khi trả lời, đề cập tên file, tên class, tên method cụ thể.
3. **Nhận điểm yếu.** Khi bị hỏi điểm yếu, trả lời trung thực rồi gợi ý cách cải thiện.
4. **Nhấn mạnh điểm nổi bật:**
   - Enforcement tự động (đóng quỹ khi vi phạm).
   - AI audit giá thị trường.
   - Face verification trong KYC.
   - Casso webhook tự động giám sát ngân hàng.
   - Trust Score cộng đồng.
5. **Chuẩn bị demo live:** Mở sẵn hệ thống, demo:
   - Tạo chiến dịch -> Staff duyệt.
   - Tạo donation -> Quét QR -> Xem balance cập nhật.
   - Tạo kế hoạch chi tiêu -> Staff duyệt -> Giải ngân -> Nộp minh chứng.
   - Flag chiến dịch -> Staff xử lý.
   - Xem Trust Score, Leaderboard.
   - Admin: quản lý user, cấu hình, quỹ chung.

---

*Tài liệu này được tạo tự động dựa trên phân tích toàn bộ source code của TrustFundMe-BE và danbox (web frontend).*
*Tổng số: 10 microservices, 200+ Java files, 150+ TypeScript/React files.*
*Cập nhật: 2026-05-02*
*Phiên bản 2.0: Bổ sung 14 câu hỏi khó về fault tolerance, scaling, distributed transaction, security vulnerability*
