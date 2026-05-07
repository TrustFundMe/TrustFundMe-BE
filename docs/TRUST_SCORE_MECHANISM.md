# TrustFundMe — CƠ CHẾ TRUST SCORE (Điểm Tín Nhiệm)

> **Câu hỏi:** Trust Score tính như thế nào? Có đang hardcode không?
> **Trả lời ngắn:** Trust Score **KHÔNG hardcode**. Điểm được lưu trong bảng `trust_score_config` trong database, Admin có thể thay đổi điểm số, bật/tắt rule, sửa tên và mô tả qua API `PUT /api/trust-score/config/{ruleKey}` — **không cần sửa code hay restart server.**

---

## 1. KIẾN TRÚC TỔNG QUAN

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        TRUST SCORE SYSTEM                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────────┐     ┌──────────────────┐     ┌─────────────────┐ │
│  │ trust_score_config│     │  trust_score_log  │     │ users.trust_    │ │
│  │ (campaign-service)│     │ (campaign-service) │     │ score (identity)│ │
│  ├──────────────────┤     ├──────────────────┤     ├─────────────────┤ │
│  │ rule_key         │     │ user_id           │     │ user_id         │ │
│  │ rule_name        │     │ rule_key          │     │ trust_score     │ │
│  │ points    ←──────│────→│ points_change     │────→│ (tổng tích lũy) │ │
│  │ description      │     │ reference_id      │     │                 │ │
│  │ is_active        │     │ reference_type    │     │                 │ │
│  └──────────────────┘     │ description       │     └─────────────────┘ │
│        ↕ Admin API        │ created_at        │       ↕ Identity Service│
│  PUT /api/trust-score/    └──────────────────┘       GET /api/users/   │
│  config/{ruleKey}              ↕ Log API              {id}/trust-score │
│                          GET /api/trust-score/logs                      │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. BẢNG QUY TẮC CHẤM ĐIỂM (Database-driven, KHÔNG hardcode)

Dữ liệu seed từ [`init-all-databases.sql`](deploy/init-scripts/init-all-databases.sql:247):

| # | Rule Key | Tên tiếng Việt | Điểm mặc định | Mô tả | Trạng thái |
|---|----------|---------------|---------------|--------|------------|
| 1 | `CAMPAIGN_APPROVED` | Campaign được duyệt | **+50** | Cộng điểm khi chiến dịch được staff duyệt thành công | ✅ Active |
| 2 | `CAMPAIGN_REJECTED` | Campaign bị từ chối | **-20** | Trừ điểm khi chiến dịch bị staff từ chối | ✅ Active |
| 3 | `ON_TIME_SUBMIT` | Nộp đúng hạn | **+20** | Cộng điểm khi chi tiêu được duyệt đúng hoặc trước hạn nộp | ✅ Active |
| 4 | `LATE_SUBMIT` | Nộp muộn | **-10** | Trừ điểm khi chi tiêu nộp muộn so với hạn chót | ✅ Active |
| 5 | `DAILY_POST` | Đăng bài hàng ngày | **+5** | Cộng điểm khi đăng ít nhất 1 bài viết/ngày (tối đa 1 lần/ngày) | ✅ Active |

### Bảng DB schema:

```sql
CREATE TABLE trust_score_config (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_key    VARCHAR(100) NOT NULL UNIQUE,   -- Key lookup, không đổi
    rule_name   VARCHAR(255) NOT NULL,          -- Tên hiển thị (Admin sửa được)
    points      INT NOT NULL,                    -- Số điểm (Admin sửa được, có thể âm)
    description TEXT,                            -- Mô tả (Admin sửa được)
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,   -- Bật/tắt rule (Admin sửa được)
    created_at  DATETIME NOT NULL DEFAULT NOW(),
    updated_at  DATETIME
);
```

---

## 3. CÓ HARDCODE KHÔNG? — PHÂN TÍCH CHI TIẾT

### ❌ ĐIỂM SỐ: KHÔNG HARDCODE

Điểm được đọc từ DB tại runtime trong [`TrustScoreServiceImpl.addScore()`](campaign-service/src/main/java/com/trustfund/service/impl/TrustScoreServiceImpl.java:82):

```java
// Đọc config từ DB — KHÔNG hardcode số điểm
TrustScoreConfig config = configRepository.findByRuleKey(ruleKey).orElse(null);
if (config == null || !config.getIsActive()) {
    return; // Rule không tồn tại hoặc bị tắt → skip
}

int points = config.getPoints(); // ← Điểm lấy từ DB
```

Admin thay đổi điểm qua API mà **không cần restart**:
```
PUT /api/trust-score/config/CAMPAIGN_APPROVED
Body: { "points": 100 }  ← Đổi từ 50 thành 100
```

### ⚠️ RULE KEY: CÓ HARDCODE (nhưng là thiết kế hợp lý)

Các `ruleKey` string **CÓ hardcode** tại nơi gọi `addScore()`:

| Nơi gọi | Rule Key hardcode | File |
|----------|-------------------|------|
| Duyệt chiến dịch | `"CAMPAIGN_APPROVED"` / `"CAMPAIGN_REJECTED"` | [`CampaignServiceImpl.java:261`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:261) |
| Enforcement quá hạn | `"LATE_SUBMIT"` | [`ExpenditureEnforcementScheduler.java:73`](campaign-service/src/main/java/com/trustfund/scheduler/ExpenditureEnforcementScheduler.java:73) |
| Đăng bài Feed | `"DAILY_POST"` | Trong FeedPostServiceImpl |
| Nộp minh chứng đúng hạn | `"ON_TIME_SUBMIT"` | Trong ExpenditureServiceImpl |

**Tại sao hardcode rule key là hợp lý:**
- Rule key là **identifier** (như enum), không phải giá trị nghiệp vụ.
- Giống cách Spring Security hardcode role name (`"ADMIN"`, `"STAFF"`) — chỉ là key lookup.
- Nếu muốn thêm rule mới → cần thêm code trigger + thêm row trong DB. Đây là thiết kế **có chủ đích**.

### ✅ CƠ CHẾ CACHE THÔNG MINH

[`TrustScoreServiceImpl`](campaign-service/src/main/java/com/trustfund/service/impl/TrustScoreServiceImpl.java:40) có `ConcurrentHashMap` cache:

```java
private final Map<String, Integer> configCache = new ConcurrentHashMap<>();
```

- Cache giúp không phải query DB mỗi lần cộng điểm.
- Khi Admin update config → `configCache.remove(ruleKey)` invalidate cache.
- Thread-safe với `ConcurrentHashMap`.

---

## 4. FLOW CHẤM ĐIỂM — TỪ TRIGGER ĐẾN LƯU

```
[Sự kiện xảy ra]  (VD: Staff duyệt chiến dịch)
        ↓
[Service gọi trustScoreService.addScore(userId, ruleKey, referenceId, referenceType, description)]
        ↓
[1. Lookup config] ← configRepository.findByRuleKey(ruleKey) → lấy points từ DB
        ↓
[2. Check active]  ← config.getIsActive() == true? Nếu false → skip
        ↓
[3. Dedup check]   ← Đã chấm điểm cho reference này chưa?
  │  • DAILY_POST: existsByUserIdAndRuleKeyAndCreatedAtAfter(userId, ruleKey, startOfDay)
  │  • Khác: findByUserIdAndRuleKeyAndReference(userId, ruleKey, referenceId)
  │  → Nếu đã tồn tại → skip (không cộng/trừ lần 2)
        ↓
[4. Write log]     ← TrustScoreLog { userId, ruleKey, pointsChange, referenceId, referenceType, description }
  │                   → Lưu vào trust_score_log (campaign-service DB)
        ↓
[5. Sync Identity] ← REST PUT /api/users/{userId}/trust-score?points={delta}
  │                   → Identity Service cộng/trừ vào users.trust_score
  │                   → Nếu Identity Service down → log error, KHÔNG rollback log ở bước 4
        ↓
[Done]
```

---

## 5. BẢNG CHI TIẾT CÁC SỰ KIỆN TRIGGER

| Sự kiện | Ai trigger | Rule Key | Điểm | Nơi trong code | Reference |
|---------|-----------|----------|------|-----------------|-----------|
| Staff **duyệt** chiến dịch | `CampaignServiceImpl.reviewCampaign()` | `CAMPAIGN_APPROVED` | +50 | [`CampaignServiceImpl.java:261`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:261) | campaignId |
| Staff **từ chối** chiến dịch | `CampaignServiceImpl.reviewCampaign()` | `CAMPAIGN_REJECTED` | -20 | [`CampaignServiceImpl.java:261`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:261) | campaignId |
| Fund Owner **nộp minh chứng đúng hạn** | `ExpenditureServiceImpl` | `ON_TIME_SUBMIT` | +20 | ExpenditureServiceImpl | expenditureId |
| Fund Owner **nộp minh chứng muộn** / quá hạn | `ExpenditureEnforcementScheduler` | `LATE_SUBMIT` | -10 | [`ExpenditureEnforcementScheduler.java:73`](campaign-service/src/main/java/com/trustfund/scheduler/ExpenditureEnforcementScheduler.java:73) | evidenceId |
| Fund Owner **đăng bài viết** (1 lần/ngày) | `FeedPostServiceImpl` | `DAILY_POST` | +5 | FeedPostServiceImpl | postId |

---

## 6. CƠ CHẾ CHỐNG TRÙNG (DEDUP)

### DAILY_POST — Chỉ 1 lần/ngày:

```java
if ("DAILY_POST".equals(ruleKey)) {
    LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
    boolean alreadyPosted = logRepository.existsByUserIdAndRuleKeyAndCreatedAtAfter(
        userId, ruleKey, startOfDay);
    if (alreadyPosted) return; // Skip — đã cộng điểm hôm nay rồi
}
```

### Các rule khác — Không cộng 2 lần cho cùng reference:

```java
if (referenceId != null) {
    List<TrustScoreLog> existing = logRepository.findByUserIdAndRuleKeyAndReference(
        userId, ruleKey, referenceId);
    if (!existing.isEmpty()) return; // Skip — đã chấm cho campaign/expenditure này rồi
}
```

**Ví dụ:** Nếu staff duyệt campaign #5 → cộng 50 điểm. Dù bug gọi `addScore()` 2 lần cho campaign #5 → lần 2 sẽ bị skip.

---

## 7. NƠI LƯU TRỮ TRUST SCORE

Trust Score được lưu ở **2 nơi** (eventual consistency):

| Nơi | Service | Mục đích | Dữ liệu |
|-----|---------|----------|----------|
| `trust_score_log` | Campaign Service | **Audit trail** — ghi chi tiết từng lần cộng/trừ | userId, ruleKey, pointsChange, referenceId, description, createdAt |
| `users.trust_score` | Identity Service | **Tổng tích lũy** — hiển thị trên profile, leaderboard | Số nguyên (INT), tổng cộng dồn |

**Sync mechanism:** Sau khi ghi log, campaign-service gọi REST `PUT` tới identity-service để cộng/trừ `users.trust_score`. Nếu identity-service down → log đã ghi nhưng score chưa sync. Cần reconciliation job.

---

## 8. API QUẢN LÝ TRUST SCORE

| Method | Endpoint | Mô tả | Quyền |
|--------|----------|-------|-------|
| `GET` | `/api/trust-score/config` | Xem tất cả rule config | STAFF, ADMIN |
| `PUT` | `/api/trust-score/config/{ruleKey}` | **Sửa điểm, tên, mô tả, bật/tắt** | ADMIN |
| `GET` | `/api/trust-score/logs` | Xem lịch sử chấm điểm (filter by userId, ruleKey, date) | STAFF, ADMIN |
| `GET` | `/api/trust-score/user/{userId}` | Xem tổng điểm 1 user | Authenticated |
| `GET` | `/api/trust-score/leaderboard` | Bảng xếp hạng | Public |

### Ví dụ: Admin đổi điểm cho rule CAMPAIGN_APPROVED

```
PUT /api/trust-score/config/CAMPAIGN_APPROVED
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "points": 100,          // Đổi từ 50 → 100
  "ruleName": "Duyệt campaign thành công",
  "description": "Thưởng 100 điểm khi chiến dịch được phê duyệt",
  "isActive": true
}
```

**Response:**
```json
{
  "id": 1,
  "ruleKey": "CAMPAIGN_APPROVED",
  "ruleName": "Duyệt campaign thành công",
  "points": 100,
  "description": "Thưởng 100 điểm khi chiến dịch được phê duyệt",
  "isActive": true,
  "createdAt": "2026-01-01T00:00:00",
  "updatedAt": "2026-05-07T10:30:00"
}
```

---

## 9. TÓM TẮT: HARDCODE HAY KHÔNG?

| Thành phần | Hardcode? | Giải thích |
|-----------|-----------|-----------|
| **Số điểm (points)** | ❌ KHÔNG | Đọc từ bảng `trust_score_config` trong DB. Admin sửa qua API. |
| **Tên rule (rule_name)** | ❌ KHÔNG | Lưu trong DB, Admin sửa qua API. |
| **Mô tả (description)** | ❌ KHÔNG | Lưu trong DB, Admin sửa qua API. |
| **Bật/tắt rule (is_active)** | ❌ KHÔNG | Lưu trong DB, Admin sửa qua API. |
| **Rule Key (CAMPAIGN_APPROVED...)** | ⚠️ CÓ | Dùng làm identifier trong code. Cần sửa code nếu thêm rule mới. Đây là thiết kế hợp lý (giống enum). |
| **Giá trị seed mặc định** | ⚠️ CÓ | Trong `init-all-databases.sql`. Chỉ dùng lần đầu setup, sau đó Admin thay đổi qua API. |

**Kết luận:** Hệ thống Trust Score là **database-driven, configurable at runtime** — Admin thay đổi điểm số mà KHÔNG cần sửa code hay restart server. Chỉ có `ruleKey` (identifier) là hardcode trong code, tương tự cách Spring Security hardcode role name.

---

*Tài liệu tạo dựa trên phân tích code: [`TrustScoreConfig.java`](campaign-service/src/main/java/com/trustfund/model/TrustScoreConfig.java), [`TrustScoreServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/TrustScoreServiceImpl.java), [`init-all-databases.sql`](deploy/init-scripts/init-all-databases.sql:247).*
*Cập nhật: 2026-05-07*
