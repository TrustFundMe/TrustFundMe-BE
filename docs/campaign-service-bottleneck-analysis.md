# Campaign Service — Bottleneck Analysis Report

> **Ngày phân tích:** 2026-05-04  
> **Service:** `campaign-service` (Spring Boot, JPA/Hibernate, MySQL)  
> **Phạm vi:** Entity, Repository, Service, Controller, DB config

---

## Tóm tắt

Sau khi phân tích toàn bộ codebase, tôi xác định **6 nhóm bottleneck chính** xếp theo mức độ nghiêm trọng từ cao đến thấp:

| # | Loại | Mức độ | Tổng issues |
|---|------|--------|-------------|
| 1 | External Service Calls trong vòng lặp (N+1 HTTP) | 🔴 CRITICAL | 5 |
| 2 | Missing Database Indexes | 🔴 CRITICAL | 12+ |
| 3 | Lazy Loading → N+1 SQL Queries | 🟠 HIGH | 4 |
| 4 | `findAll()` không pagination + In-memory Filtering | 🟠 HIGH | 5 |
| 5 | Thiếu DTO Projection (lấy full entity không cần thiết) | 🟡 MEDIUM | 3 |
| 6 | Miscellaneous Performance Issues | 🟡 MEDIUM | 3 |

---

## 1. 🔴 CRITICAL — External Service Calls trong vòng lặp (N+1 HTTP)

Đây là bottleneck **nghiêm trọng nhất**. Mỗi HTTP call tới external service mất 10-200ms+ (network latency). Khi gọi trong vòng lặp, thời gian tăng tuyến tính theo số lượng records.

### Issue 1.1: `CampaignServiceImpl.toCampaignResponse()` — 3-5 HTTP calls PER campaign

**File:** [`CampaignServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:284)

**Vấn đề:** Method `toCampaignResponse()` (line 284-351) gọi **3-5 external HTTP calls** cho MỖI campaign:
1. Line 288: `identityServiceClient.getVerificationStatus(fundOwnerId)` — HTTP call #1
2. Line 299: `identityServiceClient.getUserInfo(fundOwnerId)` — HTTP call #2
3. Line 314: `mediaServiceClient.getMediaUrl(coverImage)` — HTTP call #3
4. Line 320: `mediaServiceClient.getFirstImageByCampaignId(campaignId)` — HTTP call #4 (fallback)
5. Line 335: `mediaServiceClient.getMediaUrl(category.getIcon())` — HTTP call #5

**Tác động:** Khi gọi `getAll()` (line 38-46) trả về N campaigns → **N × 3-5 HTTP calls**.
- 20 campaigns = 60-100 HTTP calls → ~2-20 giây delay
- 100 campaigns = 300-500 HTTP calls → ~10-100 giây delay

**Các endpoint bị ảnh hưởng:**
- `GET /api/campaigns` — [`CampaignController.java:34`](campaign-service/src/main/java/com/trustfund/controller/CampaignController.java:34)
- `GET /api/campaigns/status/{status}` — [`CampaignController.java:86`](campaign-service/src/main/java/com/trustfund/controller/CampaignController.java:86)
- `GET /api/campaigns/category/{categoryId}` — [`CampaignController.java:91`](campaign-service/src/main/java/com/trustfund/controller/CampaignController.java:91)
- `GET /api/campaigns/fund-owner/{fundOwnerId}` — [`CampaignController.java:46`](campaign-service/src/main/java/com/trustfund/controller/CampaignController.java:46)

**Fix suggestion:**
```java
// 1. Batch fetch user info trước vòng lặp
Map<Long, UserInfoResponse> userInfoCache = new HashMap<>();
Set<Long> ownerIds = campaigns.stream()
    .map(Campaign::getFundOwnerId)
    .filter(Objects::nonNull)
    .collect(Collectors.toSet());

// Tạo batch API trong identity-service: POST /api/internal/users/batch
Map<Long, UserInfoResponse> batchUserInfo = identityServiceClient.getBatchUserInfo(ownerIds);

// 2. Batch fetch verification status
Map<Long, UserVerificationStatusResponse> batchVerification = 
    identityServiceClient.getBatchVerificationStatus(ownerIds);

// 3. Batch fetch media URLs
Set<Long> mediaIds = campaigns.stream()
    .map(Campaign::getCoverImage)
    .filter(Objects::nonNull)
    .collect(Collectors.toSet());
Map<Long, String> batchMediaUrls = mediaServiceClient.getBatchMediaUrls(mediaIds);

// 4. Dùng cache trong toCampaignResponse
private CampaignResponse toCampaignResponse(Campaign c, 
    Map<Long, UserInfoResponse> userInfoCache, 
    Map<Long, UserVerificationStatusResponse> verificationCache,
    Map<Long, String> mediaUrlCache) { ... }
```

---

### Issue 1.2: `FeedPostServiceImpl.toResponse()` — 3-4 HTTP calls PER post

**File:** [`FeedPostServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/implementServices/FeedPostServiceImpl.java:499)

**Vấn đề:** Method `toResponse()` (line 499-586) gọi:
1. Line 505-506: `expenditureRepository.findById()` — DB query per post (nếu targetType=EXPENDITURE)
2. Line 524: `feedPostLikeRepository.existsByPostIdAndUserId()` — DB query per post
3. Line 530: `userInfoClient.getUserInfo()` — HTTP call per post (có cache 5 phút nhưng cold-start vẫn chậm)
4. Line 539: `mediaServiceClient.getMediaByPostId()` — HTTP call per post
5. Line 583: `feedPostRevisionRepository.existsByPostId()` — DB query per post

**Tác động:** Khi lấy page 10 posts → 30-50 calls (mix HTTP + DB).

**Các endpoint bị ảnh hưởng:**
- `GET /api/feed-posts` — [`FeedPostController.java:67`](campaign-service/src/main/java/com/trustfund/controller/FeedPostController.java:67)
- `GET /api/feed-posts/my` — [`FeedPostController.java:94`](campaign-service/src/main/java/com/trustfund/controller/FeedPostController.java:94)
- `GET /api/feed-posts/admin` — [`FeedPostController.java:241`](campaign-service/src/main/java/com/trustfund/controller/FeedPostController.java:241)
- `GET /api/feed-posts/by-target` — [`FeedPostController.java:160`](campaign-service/src/main/java/com/trustfund/controller/FeedPostController.java:160)

**Fix suggestion:**
```java
// Trong getActiveFeedPosts, batch load trước khi map:
Page<FeedPost> posts = feedPostRepository.findVisibleActivePosts(currentUserId, pageable);

// Batch load: post IDs, author IDs
List<Long> postIds = posts.map(FeedPost::getId).getContent();
Set<Long> authorIds = posts.getContent().stream()
    .map(FeedPost::getAuthorId).collect(Collectors.toSet());

// Pre-fetch batch
Map<Long, UserInfoClient.UserInfo> authorCache = userInfoClient.getBatchUserInfo(authorIds);
Map<Long, List<Map<String,Object>>> mediaCache = mediaServiceClient.getBatchMediaByPostIds(postIds);
Set<Long> likedPostIds = feedPostLikeRepository.findPostIdsLikedByUser(currentUserId, postIds);
Set<Long> hasRevisionsPostIds = feedPostRevisionRepository.findPostIdsWithRevisions(postIds);

// Pass caches to toResponse
```

---

### Issue 1.3: `FeedPostCommentServiceImpl.toResponse()` — 3-5 HTTP calls PER comment

**File:** [`FeedPostCommentServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/implementServices/FeedPostCommentServiceImpl.java:183)

**Vấn đề:** Method `toResponse()` (line 183-289) gọi:
1. Line 187: `identityServiceClient.getUserInfo(comment.getUserId())` — HTTP call #1
2. Line 199: `feedPostCommentLikeRepository.existsByCommentIdAndUserId()` — DB query
3. Line 209: `feedPostRepository.findById(comment.getPostId())` — DB query per comment
4. Line 217: `identityServiceClient.getUserInfo(post.getAuthorId())` — HTTP call #2
5. Line 253-263: Nếu có parentComment: `identityServiceClient.getUserInfo(parent.getUserId())` — HTTP call #3

**Tác động nghiêm trọng khi `getCommentsByPostId()`** (line 81-98):
- Mỗi root comment → `toResponse()` (3-5 calls)
- Mỗi root comment → load replies → mỗi reply → `toResponse()` (3-5 calls)
- 10 root comments × 3 replies each = 40 × 4 calls = ~160 external calls!

**Fix suggestion:**
```java
// Batch load all user info, like status, post info trước khi map
```

---

### Issue 1.4: `FlagServiceImpl.sendFlagNotification()` — HTTP call PER follower

**File:** [`FlagServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/FlagServiceImpl.java:157)

**Vấn đề:** Line 157-181 gửi notification cho MỖI follower trong vòng `for`:
```java
for (CampaignFollow follow : followers) {
    notificationServiceClient.sendNotification(request); // HTTP call per follower
}
```

**Fix suggestion:**
```java
// Tạo batch notification API: POST /api/notifications/batch
notificationServiceClient.sendBatchNotification(List<NotificationRequest> requests);
```

---

### Issue 1.5: `CampaignTransactionServiceImpl.getCampaignTransactionHistory()` — HTTP call PER donation

**File:** [`CampaignTransactionServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignTransactionServiceImpl.java:59)

**Vấn đề:** Line 59-87: Trong vòng `forEach` xử lý donations, mỗi donation gọi:
```java
response.getBody().forEach(d -> {
    UserInfoResponse donor = identityServiceClient.getUserById(donorId); // HTTP call per donation!
});
```

**Fix suggestion:**
```java
// Batch lấy tất cả donor info trước khi loop
Set<Long> donorIds = response.getBody().stream()
    .filter(d -> !Boolean.TRUE.equals(d.get("anonymous")))
    .map(d -> Long.valueOf(d.get("donorId").toString()))
    .collect(Collectors.toSet());
Map<Long, UserInfoResponse> donorCache = identityServiceClient.getBatchUserInfo(donorIds);
```

---

## 2. 🔴 CRITICAL — Missing Database Indexes

**Không có `@Index` annotation nào** trên toàn bộ entity classes. Với `spring.jpa.hibernate.ddl-auto=update`, Hibernate chỉ tạo PK và FK indexes.

### Các index cần thêm ngay:

| Entity | Column(s) | Lý do | Priority |
|--------|-----------|-------|----------|
| `Campaign` | `fund_owner_id` | Query: `findByFundOwnerIdAndTypeNot`, `countByFundOwnerId` | 🔴 |
| `Campaign` | `status` | Query: `findByStatusAndTypeNot`, filter by status | 🔴 |
| `Campaign` | `category_id` | Query: `findByCategoryIdAndTypeNot` | 🔴 |
| `Campaign` | `type` | Query: `findByTypeNot` — mọi list query đều filter type | 🔴 |
| `FeedPost` | `author_id, status` | Query: `findMyPosts`, `findPublicPostsByAuthorId` | 🔴 |
| `FeedPost` | `status, is_locked, visibility` | Query: `findVisibleActivePosts` | 🔴 |
| `FeedPost` | `target_id, target_type` | Query: `findByTargetIdAndTargetType` | 🟠 |
| `FeedPost` | `category_id, status` | Query: `findByCategoryIdAndStatus` | 🟠 |
| `Expenditure` | `campaign_id` | Query: `findByCampaignId`, `findByCampaignIdOrderByCreatedAtDesc` | 🔴 |
| `Expenditure` | `status` | Query: `findByStatusOrderByCreatedAtDesc` | 🟠 |
| `FeedPostComment` | `post_id` | Query: `findByPostIdOrderByCreatedAtDesc` | 🔴 |
| `FeedPostComment` | `parent_comment_id` | Query: `findByParentCommentId` | 🟠 |
| `FeedPostComment` | `user_id` | Query: `findByUserIdOrderByCreatedAtDesc` | 🟠 |
| `Flag` | `status` | Query: `findByStatus` | 🟠 |
| `Flag` | `user_id, campaign_id` | Query: `existsByUserIdAndCampaignId` | 🟠 |
| `Flag` | `user_id, post_id` | Query: `existsByUserIdAndPostId` | 🟠 |
| `FeedPostLike` | `post_id, user_id` | Đã có UniqueConstraint nhưng nên verify index | ✅ |
| `ExpenditureItem` | `expenditure_id` | Query: `findByExpenditureId` | 🟠 |
| `ExpenditureItem` | `catology_id` | Query: `deleteByCatologyId` | 🟠 |
| `ExpenditureCatology` | `expenditure_id` | Query: `findByExpenditureId` | 🟠 |
| `ExpenditureTransaction` | `expenditure_id` | Query: `findByExpenditureId` | 🟠 |
| `ExpenditureEvidence` | `status, due_at` | Query: `findByStatusAndDueAtBefore` (scheduler) | 🟠 |
| `TrustScoreLog` | `user_id` | Query: `findByUserIdOrderByCreatedAtDesc` | 🟠 |
| `TrustScoreLog` | `user_id, rule_key, reference_id` | Query: `findByUserIdAndRuleKeyAndReference` | 🟠 |
| `InternalTransaction` | `from_campaign_id` | Query: `findByFromCampaignIdOrToCampaignId` | 🟠 |
| `InternalTransaction` | `to_campaign_id` | Query: `findByToCampaignIdAndStatus` | 🟠 |
| `ApprovalTask` | `type, target_id` | Query: `findByTypeAndTargetId` | 🟠 |
| `ApprovalTask` | `staff_id, status` | Query: `findByStaffIdAndStatus` | 🟠 |
| `CampaignCommitment` | `campaign_id` | Lookup by campaign | 🟡 |
| `FundraisingGoal` | `campaign_id` | Query: `findByCampaignId` | 🟡 |
| `UserPostSeen` | `user_id, post_id` | Query: `existsByUserIdAndPostId` | 🟠 |

**Fix suggestion — thêm `@Table` indexes:**

```java
// Campaign.java
@Entity
@Table(name = "campaigns", indexes = {
    @Index(name = "idx_campaign_fund_owner", columnList = "fund_owner_id"),
    @Index(name = "idx_campaign_status", columnList = "status"),
    @Index(name = "idx_campaign_category", columnList = "category_id"),
    @Index(name = "idx_campaign_type", columnList = "type"),
    @Index(name = "idx_campaign_owner_type", columnList = "fund_owner_id, type")
})
public class Campaign { ... }

// FeedPost.java
@Entity
@Table(name = "feed_post", indexes = {
    @Index(name = "idx_feedpost_author_status", columnList = "author_id, status"),
    @Index(name = "idx_feedpost_status_locked_vis", columnList = "status, is_locked, visibility"),
    @Index(name = "idx_feedpost_target", columnList = "target_id, target_type"),
    @Index(name = "idx_feedpost_category_status", columnList = "category_id, status")
})
public class FeedPost { ... }

// Expenditure.java
@Entity
@Table(name = "expenditures", indexes = {
    @Index(name = "idx_expenditure_campaign", columnList = "campaign_id"),
    @Index(name = "idx_expenditure_status", columnList = "status")
})
public class Expenditure { ... }

// FeedPostComment.java
@Entity
@Table(name = "feed_post_comment", indexes = {
    @Index(name = "idx_comment_post", columnList = "post_id"),
    @Index(name = "idx_comment_parent", columnList = "parent_comment_id"),
    @Index(name = "idx_comment_user", columnList = "user_id")
})
public class FeedPostComment { ... }
```

**Hoặc migration SQL trực tiếp:**
```sql
-- Campaign indexes
CREATE INDEX idx_campaign_fund_owner ON campaigns(fund_owner_id);
CREATE INDEX idx_campaign_status ON campaigns(status);
CREATE INDEX idx_campaign_category ON campaigns(category_id);
CREATE INDEX idx_campaign_type ON campaigns(type);

-- FeedPost indexes
CREATE INDEX idx_feedpost_author_status ON feed_post(author_id, status);
CREATE INDEX idx_feedpost_status_locked_vis ON feed_post(status, is_locked, visibility);
CREATE INDEX idx_feedpost_target ON feed_post(target_id, target_type);

-- Expenditure indexes
CREATE INDEX idx_expenditure_campaign ON expenditures(campaign_id);
CREATE INDEX idx_expenditure_status ON expenditures(status);

-- FeedPostComment indexes
CREATE INDEX idx_comment_post ON feed_post_comment(post_id);
CREATE INDEX idx_comment_parent ON feed_post_comment(parent_comment_id);
CREATE INDEX idx_comment_user ON feed_post_comment(user_id);

-- Flag indexes
CREATE INDEX idx_flag_status ON flags(status);
CREATE INDEX idx_flag_user_campaign ON flags(user_id, campaign_id);
CREATE INDEX idx_flag_user_post ON flags(user_id, post_id);
```

---

## 3. 🟠 HIGH — Lazy Loading → N+1 SQL Queries

### Issue 3.1: `Expenditure.transactions` & `Expenditure.evidences` — EAGER by default (OneToMany)

**File:** [`Expenditure.java`](campaign-service/src/main/java/com/trustfund/model/Expenditure.java:26)

**Vấn đề:** `@OneToMany` không khai báo `fetch = FetchType.LAZY` → **LAZY mặc định cho `@OneToMany`** (đúng chuẩn JPA). Tuy nhiên, khi `mapToResponse()` truy cập `expenditure.getTransactions()` và `expenditure.getEvidences()`, Hibernate fire thêm 2 SELECT queries.

**Khi gọi `getExpendituresByCampaign()`** (line 358-364):
```java
List<Expenditure> expenditures = expenditureRepository.findByCampaignId(campaignId);
return expenditures.stream().map(this::mapToResponse).collect(Collectors.toList());
```
→ 1 query lấy N expenditures + N×2 queries lazy load (transactions + evidences) = **1 + 2N queries**

**File:** [`ExpenditureServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/ExpenditureServiceImpl.java:358)

**Fix suggestion:**
```java
// Option A: Thêm JOIN FETCH query trong repository
@Query("SELECT e FROM Expenditure e " +
       "LEFT JOIN FETCH e.transactions " + 
       "LEFT JOIN FETCH e.evidences " +
       "WHERE e.campaignId = :campaignId")
List<Expenditure> findByCampaignIdWithRelations(@Param("campaignId") Long campaignId);

// Option B: EntityGraph
@EntityGraph(attributePaths = {"transactions", "evidences"})
List<Expenditure> findByCampaignId(Long campaignId);
```

### Issue 3.2: `ExpenditureCatology.items` — Lazy load N+1

**File:** [`ExpenditureCatology.java`](campaign-service/src/main/java/com/trustfund/model/ExpenditureCatology.java:56)

**Vấn đề:** `getExpenditureCategories()` trong [`ExpenditureServiceImpl.java:1191`](campaign-service/src/main/java/com/trustfund/service/impl/ExpenditureServiceImpl.java:1191) load catologies rồi access `cat.getItems()` → N+1 query:
```java
List<ExpenditureCatology> catologies = catologyRepository.findByExpenditureId(expenditureId);
return catologies.stream().map(cat -> {
    List<ExpenditureItemResponse> itemResponses = cat.getItems() != null // <-- triggers lazy load PER catology
        ? cat.getItems().stream().map(this::mapToItemResponse).collect(...)
```

**Fix suggestion:**
```java
@Query("SELECT ec FROM ExpenditureCatology ec LEFT JOIN FETCH ec.items WHERE ec.expenditure.id = :expenditureId")
List<ExpenditureCatology> findByExpenditureIdWithItems(@Param("expenditureId") Long expenditureId);
```

### Issue 3.3: `ExpenditureItem.catology` access trong `mapToItemResponse()`

**File:** [`ExpenditureServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/ExpenditureServiceImpl.java:409)

**Vấn đề:** Line 409: `item.getCatology() != null ? item.getCatology().getName() : null` → lazy load `catology` PER item.

**Fix suggestion:**
```java
// Thêm JOIN FETCH trong query lấy items
@Query("SELECT ei FROM ExpenditureItem ei LEFT JOIN FETCH ei.catology WHERE ei.expenditure.id = :expenditureId")
List<ExpenditureItem> findByExpenditureIdWithCatology(@Param("expenditureId") Long expenditureId);
```

### Issue 3.4: `Campaign.category` Lazy Load trong list operations

**File:** [`Campaign.java`](campaign-service/src/main/java/com/trustfund/model/Campaign.java:49)

**Vấn đề:** `@ManyToOne(fetch = FetchType.LAZY)` cho category (line 49). Khi `toCampaignResponse()` truy cập `campaign.getCategory().getId()`, `getName()` → lazy load PER campaign.

**Đặc biệt trong `getByCategoryId()`** (line 186-195):
```java
.filter(c -> c.getCategory().getId().equals(categoryId)) // <-- triggers lazy load PER campaign
```
Load ALL campaigns rồi filter in-memory theo category → lazy load category cho MỖI campaign → N+1.

**Fix suggestion:**
```java
// Dùng EntityGraph hoặc JPQL JOIN FETCH cho list queries
@EntityGraph(attributePaths = {"category"})
List<Campaign> findByTypeNot(String type, Sort sort);
```

---

## 4. 🟠 HIGH — `findAll()` không pagination + In-memory Filtering

### Issue 4.1: `CampaignServiceImpl.getByStatus()` — Load ALL rồi filter in-memory

**File:** [`CampaignServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:174)

**Vấn đề:** Line 174-183:
```java
public List<CampaignResponse> getByStatus(String status) {
    return campaignRepository
        .findByTypeNot(Campaign.TYPE_GENERAL_FUND, Sort.by(DESC, "createdAt"))
        .stream()
        .filter(c -> c.getStatus().equalsIgnoreCase(status)) // <-- IN-MEMORY FILTER!
        .map(this::toCampaignResponse) // <-- 3-5 HTTP calls per campaign
        .collect(Collectors.toList());
}
```
Load **TẤT CẢ campaigns** (không pagination) → filter in-memory → gọi toCampaignResponse cho mỗi kết quả.

**Fix suggestion:**
```java
// Dùng đúng query đã có trong repo
public List<CampaignResponse> getByStatus(String status) {
    return campaignRepository.findByStatusAndTypeNot(status, Campaign.TYPE_GENERAL_FUND)
        .stream().map(this::toCampaignResponse).collect(Collectors.toList());
}
// Hoặc tốt hơn: thêm pagination
Page<Campaign> findByStatusAndTypeNot(String status, String type, Pageable pageable);
```

### Issue 4.2: `CampaignServiceImpl.getByCategoryId()` — Load ALL rồi filter in-memory

**File:** [`CampaignServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:186)

**Vấn đề:** Line 186-195 — giống hệt issue 4.1 nhưng filter theo category:
```java
.filter(c -> c.getCategory().getId().equals(categoryId)) // IN-MEMORY + lazy load!
```

**Fix suggestion:**
```java
public List<CampaignResponse> getByCategoryId(Long categoryId) {
    return campaignRepository.findByCategoryIdAndTypeNot(categoryId, Campaign.TYPE_GENERAL_FUND)
        .stream().map(this::toCampaignResponse).collect(Collectors.toList());
}
```

### Issue 4.3: `CampaignServiceImpl.getAll()` (no-pageable version) — Load ALL

**File:** [`CampaignServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:38)

**Vấn đề:** Line 38-46 trả về `List<CampaignResponse>` — KHÔNG giới hạn. Nếu có 10,000 campaigns → load tất cả + gọi 30,000-50,000 HTTP calls.

**Fix suggestion:** Xóa method `getAll()` không có pagination, chỉ giữ bản có `Pageable`.

### Issue 4.4: `FeedPostServiceImpl.syncAllCommentCounts()` — `findAll()` + N queries

**File:** [`FeedPostServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/implementServices/FeedPostServiceImpl.java:407)

**Vấn đề:** Line 407-420:
```java
List<FeedPost> all = feedPostRepository.findAll(); // Load ALL posts
for (FeedPost post : all) {
    int actual = feedPostCommentRepository.countByPostId(post.getId()); // N queries
}
```

**Fix suggestion:**
```java
// Batch count với GROUP BY
@Query("SELECT c.postId, COUNT(c) FROM FeedPostComment c GROUP BY c.postId")
List<Object[]> countByPostIdGrouped();

// Hoặc dùng bulk update SQL
@Modifying
@Query("UPDATE FeedPost p SET p.commentCount = (SELECT COUNT(c) FROM FeedPostComment c WHERE c.postId = p.id), p.replyCount = (SELECT COUNT(c) FROM FeedPostComment c WHERE c.postId = p.id)")
void syncAllCommentCounts();
```

### Issue 4.5: `ExpenditureServiceImpl.getPendingEvidenceByUser()` — `findAll()` + in-memory filter

**File:** [`ExpenditureServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/ExpenditureServiceImpl.java:1344)

**Vấn đề:** Line 1344:
```java
return evidenceRepository.findAll().stream() // Load ALL evidence records
    .filter(ev -> campaignIds.contains(ev.getCampaignId()) && ...)
```

**Fix suggestion:**
```java
// Thêm query trong repository
@Query("SELECT e FROM ExpenditureEvidence e WHERE e.campaignId IN :campaignIds AND e.status IN ('PENDING', 'OVERDUE')")
List<ExpenditureEvidence> findPendingByCampaignIds(@Param("campaignIds") List<Long> campaignIds);
```

---

## 5. 🟡 MEDIUM — Thiếu DTO Projection

### Issue 5.1: Nhiều repository methods trả về full Entity khi chỉ cần vài field

**Files:**
- [`CampaignRepository.java:15`](campaign-service/src/main/java/com/trustfund/repository/CampaignRepository.java:15) — `findByStatusAndTypeNot()` trả về full `Campaign` khi có thể chỉ cần `id, title, status`
- [`ExpenditureRepository.java:11`](campaign-service/src/main/java/com/trustfund/repository/ExpenditureRepository.java:11) — `findByCampaignId()` trả về full `Expenditure` với tất cả columns

### Issue 5.2: `CampaignServiceImpl.getByFundOwnerId()` — PageRequest.of(0, Integer.MAX_VALUE)

**File:** [`CampaignServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignServiceImpl.java:63)

**Vấn đề:** Line 64-65:
```java
campaignRepository.findByFundOwnerIdAndTypeNot(fundOwnerId, Campaign.TYPE_GENERAL_FUND,
    PageRequest.of(0, Integer.MAX_VALUE, Sort.by(DESC, "createdAt")))
```
Tạo Page request với size `Integer.MAX_VALUE` = 2,147,483,647 → load toàn bộ DB.

### Issue 5.3: `ExpenditureServiceImpl.getAllTransactions()` — No pagination

**File:** [`ExpenditureServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/ExpenditureServiceImpl.java:1157)

**Vấn đề:** Line 1158:
```java
List<ExpenditureTransaction> all = transactionRepository.findAll();
```
Load TẤT CẢ transactions → gọi `mapToTransactionResponse` cho mỗi cái. Không pagination.

**Fix suggestion:**
```java
// Controller
@GetMapping("/transactions")
public Page<ExpenditureTransactionResponse> getAllTransactions(Pageable pageable) {
    return expenditureService.getAllTransactions(pageable);
}
```

---

## 6. 🟡 MEDIUM — Miscellaneous Performance Issues

### Issue 6.1: `CampaignStatisticsServiceImpl` — N+1 query cho InternalTransaction

**File:** [`CampaignStatisticsServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/CampaignStatisticsServiceImpl.java:58)

**Vấn đề:** Line 58-67:
```java
for (Long campaignId : campaignIds) {
    BigDecimal sum = internalTransactionRepository
        .sumAmountByFromCampaignIdAndTypeAndStatus(campaignId, ...); // DB query PER campaign
}
```

**Fix suggestion:**
```java
// Batch query
@Query("SELECT SUM(t.amount) FROM InternalTransaction t WHERE t.fromCampaignId IN :campaignIds AND t.type = :type AND t.status = :status")
BigDecimal sumAmountByFromCampaignIdsAndTypeAndStatus(
    @Param("campaignIds") List<Long> campaignIds,
    @Param("type") InternalTransactionType type,
    @Param("status") InternalTransactionStatus status);
```

### Issue 6.2: `FeedPostServiceImpl.getByTarget()` — No pagination

**File:** [`FeedPostServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/implementServices/FeedPostServiceImpl.java:589)

**Vấn đề:** Line 589-599:
```java
return feedPostRepository.findByTargetIdAndTargetTypeOrderByCreatedAtDesc(targetId, targetType)
    .stream()
    .filter(p -> !Boolean.TRUE.equals(p.getIsLocked()))
    .map(p -> toResponse(p, null, 0)) // N HTTP calls per post!
    .collect(Collectors.toList());
```
Không pagination + in-memory filter + N external calls.

### Issue 6.3: `ExpenditureServiceImpl.cleanupOldDatabaseConstraints()` — DDL tại startup

**File:** [`ExpenditureServiceImpl.java`](campaign-service/src/main/java/com/trustfund/service/impl/ExpenditureServiceImpl.java:60)

**Vấn đề:** `@PostConstruct` method chạy `ALTER TABLE` mỗi lần khởi động service:
```java
jdbcTemplate.execute("ALTER TABLE expenditures DROP INDEX campaign_id");
```
Không nguy hiểm lắm (vì try-catch), nhưng nên chuyển sang migration script.

### Issue 6.4: `spring.jpa.show-sql=true` trong production

**File:** [`application.properties`](campaign-service/src/main/resources/application.properties:23)

**Vấn đề:** Line 23: `spring.jpa.show-sql=true` — Log mỗi SQL query ra stdout, gây I/O overhead trong production.

**Fix suggestion:**
```properties
# Tắt trong production
spring.jpa.show-sql=false
# Nếu cần debug, dùng logging thay vì show-sql
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

---

## 7. Tổng hợp đề xuất ưu tiên (Action Plan)

### Phase 1 — Quick Wins (ít effort, impact lớn)
1. ✅ Thêm database indexes (SQL migration) — **Giảm query time 10-100x**
2. ✅ Tắt `show-sql=true` 
3. ✅ Sửa `getByStatus()` và `getByCategoryId()` dùng đúng repository query thay vì in-memory filter
4. ✅ Sửa `getPendingEvidenceByUser()` dùng query thay vì `findAll()`

### Phase 2 — Medium Effort (cần refactor)
5. ✅ Thêm JOIN FETCH / EntityGraph cho Expenditure queries (fix N+1 SQL)
6. ✅ Thêm pagination cho `getAllTransactions()`, `getByTarget()`, `getAll()` (non-pageable)
7. ✅ Sửa `getByFundOwnerId()` — bỏ `Integer.MAX_VALUE`

### Phase 3 — High Effort (cần tạo batch APIs)
8. ✅ Tạo batch API trong identity-service: `POST /api/internal/users/batch` 
9. ✅ Tạo batch API trong media-service: `POST /api/media/batch`
10. ✅ Refactor `toCampaignResponse()`, `toResponse()`, `toCommentResponse()` dùng pre-fetched cache
11. ✅ Tạo batch notification API
12. ✅ Batch query `sumAmountByFromCampaignIdsAndTypeAndStatus`

### Phase 4 — Architecture Improvements
13. ✅ Thêm Redis/Caffeine cache cho user info, media URLs
14. ✅ Cân nhắc CQRS: denormalize `ownerName`, `coverImageUrl` vào Campaign entity
15. ✅ Xem xét async notification sending (`@Async`)

---

## Appendix: Danh sách files đã review

| File | Bottleneck tìm được |
|------|---------------------|
| `model/Campaign.java` | No indexes, lazy category |
| `model/FeedPost.java` | No indexes |
| `model/Expenditure.java` | No indexes, lazy transactions/evidences |
| `model/ExpenditureItem.java` | No indexes, lazy catology |
| `model/ExpenditureCatology.java` | No indexes, lazy items |
| `model/FeedPostComment.java` | No indexes |
| `model/Flag.java` | No indexes |
| `model/FundraisingGoal.java` | No indexes |
| `model/ApprovalTask.java` | No indexes |
| `model/InternalTransaction.java` | No indexes |
| `model/TrustScoreLog.java` | No indexes |
| `repository/CampaignRepository.java` | No JOIN FETCH |
| `repository/ExpenditureRepository.java` | No JOIN FETCH |
| `repository/FeedPostRepository.java` | OK (has pagination) |
| `repository/ExpenditureItemRepository.java` | No JOIN FETCH for catology |
| `repository/ExpenditureCatologyRepository.java` | No JOIN FETCH for items |
| `service/impl/CampaignServiceImpl.java` | **5 bottlenecks** (N+1 HTTP, in-memory filter) |
| `service/implementServices/FeedPostServiceImpl.java` | **4 bottlenecks** (N+1 HTTP, findAll) |
| `service/impl/ExpenditureServiceImpl.java` | **3 bottlenecks** (N+1 SQL, findAll) |
| `service/FlagServiceImpl.java` | **1 bottleneck** (N HTTP notifications) |
| `service/implementServices/FeedPostCommentServiceImpl.java` | **2 bottlenecks** (N+1 HTTP) |
| `service/impl/CampaignStatisticsServiceImpl.java` | **1 bottleneck** (N+1 DB) |
| `service/impl/CampaignTransactionServiceImpl.java` | **1 bottleneck** (N+1 HTTP) |
| `client/IdentityServiceClient.java` | No batch API |
| `client/MediaServiceClient.java` | No batch API |
| `application.properties` | show-sql=true |
