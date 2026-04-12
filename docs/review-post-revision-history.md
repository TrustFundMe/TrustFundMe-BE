# Code Review Document — Post Revision History Feature

> **Mục đích**: Tài liệu này liệt kê toàn bộ file đã thêm/sửa, logic nghiệp vụ, API contract, edge case đã xử lý và các điểm cần AI reviewer kiểm tra kỹ bằng cách đọc code.

---

## 1. Tổng quan nghiệp vụ

Khi author chỉnh sửa một `FeedPost`, hệ thống tự động lưu **snapshot của trạng thái bài viết TRƯỚC khi sửa** vào bảng `feed_post_revisions`. Người dùng có thể xem lại các phiên bản cũ (chỉ xem, không restore).

### Luồng dữ liệu
```
Author gọi PUT /api/feed-posts/{id} hoặc PATCH /api/feed-posts/{id}/content
  → FeedPostServiceImpl.update() / updateContent()
  → snapshotRevision(post, currentUserId, null)   ← ghi vào feed_post_revisions
  → feedPostRepository.save(post)                 ← ghi bài mới vào feed_post
  → trả về FeedPostResponse (có hasRevisions: true nếu ≥1 revision tồn tại)
```

---

## 2. Database

### File: `deploy/init-scripts/migrate-add-feed-post-revisions.sql`

```sql
USE trustfundme_campaign_db;

CREATE TABLE IF NOT EXISTS feed_post_revisions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id         BIGINT NOT NULL COMMENT 'FK -> feed_post.id',
    revision_no     INT NOT NULL COMMENT 'Version number, increments per post',
    title           NVARCHAR(255) NULL,
    content         NVARCHAR(2000) NOT NULL,
    status          NVARCHAR(50) NOT NULL,
    media_snapshot_json JSON NULL,
    edited_by       BIGINT NOT NULL COMMENT 'user_id who triggered the edit',
    edited_by_name  VARCHAR(255) NULL,
    edit_note       VARCHAR(500) NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_fpr_post
        FOREIGN KEY (post_id) REFERENCES feed_post(id) ON DELETE CASCADE,
    CONSTRAINT uq_fpr_post_revision_no
        UNIQUE (post_id, revision_no),

    INDEX idx_fpr_post_id (post_id),
    INDEX idx_fpr_edited_by (edited_by),
    INDEX idx_fpr_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Reviewer cần kiểm tra:**
- [ ] `UNIQUE (post_id, revision_no)` — revision_no có thực sự tăng dần đúng không? (xem `findMaxRevisionNoByPostId + 1`)
- [ ] `ON DELETE CASCADE` — khi xóa post thì revisions xóa theo, có đúng với nghiệp vụ không?
- [ ] `content NVARCHAR(2000)` — giới hạn này có khớp với `feed_post.content` không?

---

## 3. Backend (campaign-service)

### 3.1 Entity: `model/FeedPostRevision.java`

```java
@Entity @Table(name = "feed_post_revisions")
public class FeedPostRevision {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "post_id", nullable = false) private Long postId;
    @Column(name = "revision_no", nullable = false) private Integer revisionNo;
    @Column(name = "title", length = 255) private String title;
    @Column(name = "content", nullable = false, length = 2000) private String content;
    @Column(name = "status", nullable = false, length = 50) private String status;
    @Column(name = "media_snapshot_json", columnDefinition = "JSON") private String mediaSnapshotJson;
    @Column(name = "edited_by", nullable = false) private Long editedBy;
    @Column(name = "edited_by_name", length = 255) private String editedByName;
    @Column(name = "edit_note", length = 500) private String editNote;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
```

**Reviewer cần kiểm tra:**
- [ ] Không có `@UpdateTimestamp` hay `@PreUpdate` — `createdAt` là immutable, đúng không?
- [ ] `mediaSnapshotJson` là `String` (raw JSON) — Jackson serialize/deserialize đúng không?

---

### 3.2 Repository: `repository/FeedPostRevisionRepository.java`

```java
public interface FeedPostRevisionRepository extends JpaRepository<FeedPostRevision, Long> {
    Page<FeedPostRevision> findByPostIdOrderByRevisionNoDesc(Long postId, Pageable pageable);

    @Query("SELECT COALESCE(MAX(r.revisionNo), 0) FROM FeedPostRevision r WHERE r.postId = :postId")
    int findMaxRevisionNoByPostId(@Param("postId") Long postId);

    Optional<FeedPostRevision> findByPostIdAndId(Long postId, Long id);

    boolean existsByPostId(Long postId);
}
```

**Reviewer cần kiểm tra:**
- [ ] `findMaxRevisionNoByPostId` trả về `0` khi chưa có revision nào — `COALESCE(MAX(...), 0)` có hoạt động đúng với JPQL không?
- [ ] `findByPostIdAndId` dùng để tránh access một revision thuộc post khác — đây là security check quan trọng.
- [ ] `existsByPostId` được gọi trong `toResponse()` cho mọi post trong feed list — có N+1 query issue không? (Hiện chấp nhận được cho MVP)

---

### 3.3 Service: `FeedPostServiceImpl.java` — các method liên quan

#### `snapshotRevision(FeedPost post, Long editedBy, String editNote)`

```java
private void snapshotRevision(FeedPost post, Long editedBy, String editNote) {
    if (post.getId() == null) return;
    try {
        List<Map<String, Object>> mediaList = mediaServiceClient.getMediaByPostId(post.getId());
        String mediaJson = serializeMediaSnapshot(mediaList);

        int nextRevNo = feedPostRevisionRepository.findMaxRevisionNoByPostId(post.getId()) + 1;

        String editorName = null;
        try {
            UserInfoClient.UserInfo info = userInfoClient.getUserInfo(editedBy);
            if (info != null) editorName = info.fullName();
        } catch (Exception ignored) {}

        FeedPostRevision rev = FeedPostRevision.builder()
                .postId(post.getId()).revisionNo(nextRevNo)
                .title(post.getTitle()).content(post.getContent()).status(post.getStatus())
                .mediaSnapshotJson(mediaJson)
                .editedBy(editedBy).editedByName(editorName).editNote(editNote)
                .build();

        feedPostRevisionRepository.save(rev);
    } catch (Exception e) {
        // Never block the main edit if snapshot fails
        logger.error("Failed to snapshot revision for post {}: {}", post.getId(), e.getMessage());
    }
}
```

**Reviewer cần kiểm tra:**
- [ ] **Race condition**: `findMaxRevisionNoByPostId + 1` rồi `save` — nếu 2 request đồng thời, cả hai đọc `maxRevNo = 5`, cả hai tạo `revisionNo = 6` → vi phạm `UNIQUE(post_id, revision_no)`. Exception sẽ bị catch và logged — main edit vẫn thành công, nhưng **một revision sẽ bị mất**. Có chấp nhận được không?
- [ ] `snapshotRevision` được gọi bên trong `@Transactional`. Nếu `save(rev)` ném `DataIntegrityViolationException`, Spring có mark transaction là rollback-only không? (Không, vì exception bị catch trước khi propagate ra ngoài transaction boundary)
- [ ] `mediaServiceClient.getMediaByPostId()` là remote call — nếu media-service down thì sao? (Exception bị catch → revision không có media snapshot, nhưng main edit vẫn thành công)
- [ ] `snapshotRevision` KHÔNG được gọi trong `updateContentByAdmin()` — admin edits không được track. Có đúng với nghiệp vụ không?

#### `getRevisions(Long postId, Long currentUserId, Pageable pageable)`

```java
public Page<FeedPostRevisionResponse> getRevisions(Long postId, Long currentUserId, Pageable pageable) {
    FeedPost post = feedPostRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Feed post not found"));

    // Visibility rule
    if (!"PUBLISHED".equals(post.getStatus())) {
        boolean isOwner = currentUserId != null && currentUserId.equals(post.getAuthorId());
        if (!isOwner) {
            throw new ForbiddenException("Lịch sử chỉnh sửa chỉ hiển thị với bài đã đăng");
        }
    }

    return feedPostRevisionRepository.findByPostIdOrderByRevisionNoDesc(postId, pageable)
            .map(this::toRevisionResponse);
}
```

**Reviewer cần kiểm tra:**
- [ ] Rule: PUBLISHED post → ai cũng xem được history; non-PUBLISHED → chỉ author. Có đúng không?
- [ ] Không có role check cho STAFF/ADMIN — họ có nên được xem history của non-PUBLISHED posts không?
- [ ] `currentUserId = null` (anonymous user) với PUBLISHED post vẫn cho phép xem — đúng không?

#### `hasRevisions` trong `toResponse()`

```java
.hasRevisions(entity.getId() != null && feedPostRevisionRepository.existsByPostId(entity.getId()))
```

**Reviewer cần kiểm tra:**
- [ ] `existsByPostId` trong `toResponse()` bị gọi cho MỌI post trong feed list → N+1 query. Với feed 20 posts = 20 extra queries. Có acceptable không?
- [ ] `entity.getId() != null` guard — khi nào `id` của entity đã persist có thể là null?

---

### 3.4 Response DTO: `model/response/FeedPostRevisionResponse.java`

```java
public class FeedPostRevisionResponse {
    private Long id;
    private Long postId;
    private Integer revisionNo;
    private String title;           // snapshot: trước khi edit
    private String content;         // snapshot: trước khi edit
    private String status;          // snapshot: trước khi edit
    private List<Map<String, Object>> mediaSnapshot;  // deserialized từ media_snapshot_json
    private Long editedBy;
    private String editedByName;
    private String editNote;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
```

**Reviewer cần kiểm tra:**
- [ ] `mediaSnapshot` dùng `List<Map<String, Object>>` thay vì typed DTO — có đảm bảo backward compatibility khi media schema thay đổi không?
- [ ] `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` — client cần parse đúng format này (replace `" "` với `"T"` trước `new Date()`).

---

### 3.5 Controller: `FeedPostController.java`

```java
@GetMapping("/{id}/revisions")
public ResponseEntity<Page<FeedPostRevisionResponse>> getRevisions(
        @PathVariable("id") Long id,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size) {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Long currentUserId = null;
    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
        try { currentUserId = Long.parseLong(auth.getName()); }
        catch (NumberFormatException ignored) {}
    }

    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(feedPostService.getRevisions(id, currentUserId, pageable));
}

@GetMapping("/{id}/revisions/{revisionId}")
public ResponseEntity<FeedPostRevisionResponse> getRevisionById(
        @PathVariable("id") Long id,
        @PathVariable("revisionId") Long revisionId) {
    // same auth extraction...
    return ResponseEntity.ok(feedPostService.getRevisionById(id, revisionId, currentUserId));
}
```

**Reviewer cần kiểm tra:**
- [ ] Không có `@PreAuthorize` — endpoint công khai với PUBLISHED posts. An toàn không?
- [ ] `size` không bị giới hạn max — user có thể gọi `?size=10000`? Nên thêm `Math.min(size, 50)`.
- [ ] Sort được handle bởi `findByPostIdOrderByRevisionNoDesc` (method name) — không cần truyền Sort vào Pageable nữa (đã fix).

---

### 3.6 `FeedPostResponse.java` — field mới

```java
/**
 * True khi post có ≥1 entry trong feed_post_revisions.
 * Dùng field này (không dùng updatedAt vs createdAt) để hiển thị label "Đã chỉnh sửa".
 * Lý do: @PrePersist set cả createdAt và updatedAt bằng LocalDateTime.now() hai lần riêng biệt
 * → updatedAt luôn lệch vài millisecond so với createdAt dù chưa bao giờ edit.
 */
@JsonProperty("hasRevisions")
private boolean hasRevisions;
```

---

## 4. Web Frontend (danbox — Next.js 15 + React 19)

### 4.1 Types: `src/types/feedPost.ts`

```typescript
/** One item from media_snapshot_json */
export type RevisionMediaItem = {
  mediaId?: number;
  url: string;
  mediaType?: string;
  sortOrder?: number;
};

/** Revision snapshot — state of post BEFORE an edit */
export type FeedPostRevisionDto = {
  id: number;
  postId: number;
  revisionNo: number;
  title: string | null;
  content: string;
  status: string;
  mediaSnapshot: RevisionMediaItem[];
  editedBy: number;
  editedByName: string | null;
  editNote: string | null;
  createdAt: string;   // format: "yyyy-MM-dd HH:mm:ss"
};

export type RevisionPage = {
  content: FeedPostRevisionDto[];
  totalElements: number;
  totalPages: number;
};

// FeedPostDto — added field:
hasRevisions?: boolean;

// FeedPost (frontend type) — added field:
hasRevisions?: boolean;
```

### 4.2 Mapper: `src/lib/feedPostUtils.ts`

```typescript
export function dtoToFeedPost(dto: FeedPostDtoFromApi): FeedPost {
  return {
    // ... existing fields ...
    hasRevisions: dto.hasRevisions === true,   // strict equality: null/undefined → false
  };
}
```

**Reviewer cần kiểm tra:**
- [ ] `dto.hasRevisions === true` — backend trả `false` (primitive boolean khi không có revision) hoặc `true`. `=== true` đảm bảo null/undefined → false.
- [ ] Khi backend chưa restart (field chưa có trong response) → `hasRevisions` sẽ là `false` → label không hiện. Đây là safe default.

### 4.3 Service: `src/services/feedPostService.ts`

```typescript
async getRevisions(postId: number, params?: { page?: number; size?: number }): Promise<RevisionPage> {
  const res = await api.get<RevisionPage>(`/api/feed-posts/${postId}/revisions`, {
    params: { page: params?.page ?? 0, size: params?.size ?? 10 },
  });
  // Guard: nếu backend trả Array thay vì Page object
  if (Array.isArray(res.data)) {
    return { content: res.data as FeedPostRevisionDto[], totalElements: res.data.length, totalPages: 1 };
  }
  return res.data;
}

async getRevisionById(postId: number, revisionId: number): Promise<FeedPostRevisionDto> {
  const res = await api.get<FeedPostRevisionDto>(`/api/feed-posts/${postId}/revisions/${revisionId}`);
  return res.data;
}
```

**Reviewer cần kiểm tra:**
- [ ] `Array.isArray(res.data)` guard — khi nào backend trả Array? (không bao giờ với Page, nhưng đây là defensive code)
- [ ] Không có error handling ở service layer — component tự catch ở useState.

### 4.4 Component: `src/components/feed-post/PostRevisionHistoryModal.tsx`

```
State:
  revisions: FeedPostRevisionDto[]   // paginated list
  loading: boolean
  error: string | null
  selected: FeedPostRevisionDto | null   // null = list view, non-null = detail view
  page: number
  totalPages: number

Load flow:
  useEffect[open, load] → khi open=true: reset state → load(0)
  load(p) → getRevisions(postId, {page: p, size: 10})
           → p===0: setRevisions(newItems)
           → p>0:   setRevisions(prev => [...prev, ...newItems])

Views:
  selected === null → ListView (danh sách revision)
  selected !== null → DetailView (RevisionDetail component)

AnimatePresence:
  <AnimatePresence>
    {open && <div>...</div>}   ← conditional bên trong để exit animation hoạt động
  </AnimatePresence>
```

**Reviewer cần kiểm tra:**
- [ ] `if (!open) return null` đã bị xóa → `AnimatePresence` bọc `{open && ...}` → exit animation hoạt động đúng.
- [ ] Double-click "Xem lịch sử": `loading` state disable nút "Xem thêm" nhưng không disable nút mở modal lần đầu → gọi load() đúng 1 lần vì useEffect dependency `[open, load]`.
- [ ] Load more: `page + 1 < totalPages` → nút hiện. Sau khi load, `setPage(p)` không auto-load tiếp. Đúng.
- [ ] `selected` state reset về `null` khi `open` changes → mỗi lần mở lại modal bắt đầu từ list view.

**`RevisionDetail` component:**
```typescript
const images = (revision.mediaSnapshot ?? []).filter(
  (m) => !m.mediaType || m.mediaType.toUpperCase().includes("IMAGE") || m.mediaType.toUpperCase() === "PHOTO"
);
```
**Reviewer cần kiểm tra:**
- [ ] Backend lưu `mediaType: "PHOTO"` (không phải "IMAGE") — filter có `=== "PHOTO"` check → đúng.
- [ ] `m.mediaType === undefined` (field thiếu) → `!m.mediaType` là true → image được hiện. Có muốn hiện cả video/doc không?
- [ ] `onError` fallback: `"https://placehold.co/120x120?text=Lỗi+ảnh"` — URL encode đúng không?

### 4.5 `FeedPostDetail.tsx` — "Đã chỉnh sửa" label

```tsx
{/* Chỉ hiện khi post.hasRevisions === true (tránh false positive từ @PrePersist timestamp drift) */}
{post.hasRevisions && post.updatedAt && (
  <div style={{ display: "flex", alignItems: "center", gap: 5, marginTop: 8 }}>
    <Clock size={12} color="rgba(0,0,0,0.35)" />
    <span style={{ fontSize: 12, color: "rgba(0,0,0,0.35)" }}>
      Đã chỉnh sửa lúc {new Date(post.updatedAt.replace(" ", "T")).toLocaleString("vi-VN", {...})}
      {" · "}
      <button onClick={() => setHistoryOpen(true)}>Xem lịch sử</button>
    </span>
  </div>
)}
```

**Reviewer cần kiểm tra:**
- [ ] `.replace(" ", "T")` chỉ replace lần đầu — `"2026-04-01 10:30:00"` → `"2026-04-01T10:30:00"` — đúng.
- [ ] `post.hasRevisions` falsy (undefined/false) → label ẩn → không mở history modal khi click "Xem lịch sử" vì button không hiện. Đúng.
- [ ] "Lịch sử chỉnh sửa" trong settings dropdown (canEdit) KHÔNG check `hasRevisions` — author có thể mở modal kể cả khi chưa có revision → hiện "Bài viết chưa được chỉnh sửa lần nào." Có muốn ẩn nút này không?
- [ ] "Lịch sử" button trong action bar chỉ hiện khi `post.status === "PUBLISHED"` — đúng với visibility rule.

---

## 5. Mobile Frontend (TrustFundMe-Mobile — Flutter)

### 5.1 Model: `lib/core/models/feed_post_model.dart`

```dart
class FeedPostModel {
  // ...
  final bool hasRevisions;  // mới thêm
  // ...

  factory FeedPostModel.fromJson(Map<String, dynamic> json) {
    return FeedPostModel(
      // ...
      hasRevisions: _bool(json['hasRevisions']),  // default false nếu field thiếu
    );
  }
  // copyWithViewCount và copyWithLike đều forward hasRevisions
}
```

**Reviewer cần kiểm tra:**
- [ ] `_bool(null) = false` — field thiếu trong response → `hasRevisions = false`. An toàn.
- [ ] `hasRevisions` được copy đúng trong cả hai `copyWith` methods — nếu thiếu sẽ reset về default.

### 5.2 Model: `lib/core/models/feed_post_revision_model.dart`

```dart
class RevisionMediaItem {
  final int? mediaId;
  final String url;
  final String? mediaType;
  final int? sortOrder;
  factory RevisionMediaItem.fromJson(Map<String, dynamic> json) {
    return RevisionMediaItem(
      mediaId: json['mediaId'] is int ? json['mediaId'] as int : null,
      url: (json['url'] as String?) ?? '',
      mediaType: json['mediaType'] as String?,
      sortOrder: json['sortOrder'] is int ? json['sortOrder'] as int : null,
    );
  }
}

class FeedPostRevisionModel {
  final int id, postId, revisionNo, editedBy;
  final String? title, editedByName, editNote;
  final String content, status, createdAt;
  final List<RevisionMediaItem> mediaSnapshot;

  factory FeedPostRevisionModel.fromJson(Map<String, dynamic> json) {
    // mediaSnapshot: parse raw từ json['mediaSnapshot'] (là List<Map> sau khi BE deserialize)
    // createdAt: lưu dạng String, format "yyyy-MM-dd HH:mm:ss"
  }
}
```

**Reviewer cần kiểm tra:**
- [ ] `mediaId is int` check — tránh crash nếu backend trả `"1"` (String) thay vì `1` (int). Có cần int.tryParse không?
- [ ] `content` là `required` (non-nullable) nhưng fallback `?? ''` — đúng.
- [ ] `_int(json['id'])` có thể trả `0` nếu parse fail — có accept id=0 không?

### 5.3 API: `lib/core/api/api_service.dart`

```dart
Future<Response<dynamic>> getFeedPostRevisions(int postId, {int page = 0}) async {
  return dio.get('/api/feed-posts/$postId/revisions', queryParameters: {'page': page, 'size': 10});
}

Future<Response<dynamic>> getFeedPostRevisionById(int postId, int revisionId) async {
  return dio.get('/api/feed-posts/$postId/revisions/$revisionId');
}
```

**Reviewer cần kiểm tra:**
- [ ] Không có auth header cho revision list — đúng với PUBLISHED posts (public), nhưng nếu post không PUBLISHED thì sao? → Dio interceptor tự thêm Bearer token nếu user đã login không?
- [ ] Không có timeout override — dùng global Dio timeout.

### 5.4 Widget: `lib/widgets/feed/post_revision_history_sheet.dart`

```dart
_RevisionHistorySheetState:
  - _load() dùng if (_loading || !_hasMore) return; → tránh duplicate call
  - if (!mounted) return; → đúng sau mỗi async call
  - _page bắt đầu từ 0, tăng sau mỗi load thành công
  - _hasMore = _page < totalPages

_buildDetail():
  - filter images: mediaType == null || contains('IMAGE') || == 'PHOTO'
  - CachedNetworkImage với placeholder và errorWidget
  - ListView (không phải ListView.builder) trong detail view — OK vì số lượng ảnh ít (<20)
```

**Reviewer cần kiểm tra:**
- [ ] `_RevisionHistorySheet` dùng `final ApiService _api = ApiService()` — tạo instance mới thay vì inject. Có ảnh hưởng đến shared Dio interceptors không?
- [ ] `showModalBottomSheet` không pass `routeSettings` — nếu có deep link đến revision thì cần thêm.
- [ ] `DraggableScrollableSheet(initialChildSize: 0.65)` — trên màn hình nhỏ (480px height) chiếm 312px → đủ không?

### 5.5 `feed_post_detail_screen.dart` — "Đã chỉnh sửa" label

```dart
if (_post!.hasRevisions && _post!.updatedAt != null)
  GestureDetector(
    onTap: () => showPostRevisionHistorySheet(context, postId: _post!.id),
    child: Row(children: [
      const Icon(Icons.edit_outlined, size: 11, color: Color(0xFF9CA3AF)),
      Text('Đã chỉnh sửa ${_formatPostDate(_post!.updatedAt!)}'),
    ]),
  ),
```

**Reviewer cần kiểm tra:**
- [ ] `_post!.updatedAt != null` với `hasRevisions = true` — nếu `hasRevisions=true` mà `updatedAt=null` (lý thuyết không xảy ra) thì label ẩn. An toàn.
- [ ] `showPostRevisionHistorySheet` không kiểm tra `mounted` trước khi gọi — nếu user tap rất nhanh rồi navigate back, `context` có còn valid không? (GestureDetector tự handle, không crash)

---

## 6. API Contract

### `GET /api/feed-posts/{id}/revisions`

**Auth**: Optional (anonymous = null userId)

**Params**:
| Param | Type | Default |
|-------|------|---------|
| page  | int  | 0       |
| size  | int  | 10      |

**Response** (200):
```json
{
  "content": [
    {
      "id": 42,
      "postId": 28,
      "revisionNo": 1,
      "title": "Tiêu đề cũ",
      "content": "Nội dung cũ trước khi edit",
      "status": "PUBLISHED",
      "mediaSnapshot": [
        {"mediaId": 11, "url": "https://...", "mediaType": "PHOTO", "sortOrder": 0}
      ],
      "editedBy": 301,
      "editedByName": "Thep Mai Tân",
      "editNote": null,
      "createdAt": "2026-04-10 08:30:00"
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "number": 0,
  "size": 10
}
```

**Error cases**:
- `404` — post không tồn tại
- `403` — post không PUBLISHED và currentUserId không phải author

### `GET /api/feed-posts/{id}/revisions/{revisionId}`

**Auth**: Optional (same rules)

**Response** (200): Single `FeedPostRevisionResponse` object (same shape as content[] item)

**Error cases**:
- `404` — post hoặc revision không tồn tại
- `403` — same as above

### `GET /api/feed-posts/{id}` — field mới trong response

```json
{
  "id": 28,
  "hasRevisions": true,
  ...
}
```

`hasRevisions: false` khi post chưa có revision. `hasRevisions: true` khi có ≥1 revision trong `feed_post_revisions`.

---

## 7. Checklist cho AI Reviewer

### Logic nghiệp vụ
- [ ] Revision #1 = trạng thái của post TRƯỚC edit lần 1. Revision #2 = trạng thái TRƯỚC edit lần 2. Post hiện tại = state sau tất cả edits. Thứ tự này có đúng không?
- [ ] Post được sửa TRƯỚC khi feature deploy → `hasRevisions = false` → không có history → label không hiện → đúng hành vi mong muốn không?
- [ ] Admin/staff edit (qua `updateContentByAdmin`) KHÔNG tạo revision → admin edits invisible → có chấp nhận không?
- [ ] Visibility rule: PUBLISHED = public history; non-PUBLISHED = author only. Reviewer có đồng ý không?

### Security
- [ ] `/api/feed-posts/{id}/revisions` không có `@PreAuthorize` — dựa vào service layer check. Có `@PreAuthorize("isAuthenticated()")` thiếu không (với non-PUBLISHED posts)?
- [ ] `findByPostIdAndId(postId, revisionId)` trong `getRevisionById` — tránh IDOR (Insecure Direct Object Reference). Reviewer confirm đây là đủ bảo vệ.

### Performance
- [ ] `existsByPostId` trong `toResponse()` = N+1 queries cho feed list. Với 20 posts/page = 20 extra queries. Acceptable cho MVP?
- [ ] `snapshotRevision` trong mỗi edit gọi `mediaServiceClient.getMediaByPostId()` — một remote HTTP call. Nếu media service slow → edit slow. Đây có ảnh hưởng UX không?

### Data integrity
- [ ] Race condition trong `snapshotRevision`: 2 concurrent edits → cùng `revisionNo` → unique constraint violation → 1 revision bị mất (caught, logged). Có cần fix bằng pessimistic lock hay `REQUIRES_NEW` transaction không?
- [ ] `content` trong revision có length `2000` — nếu original post content > 2000 chars thì sẽ bị truncate khi save entity? (JPA sẽ throw khi persist nếu không truncate)

---

## 8. Files đã thay đổi (tóm tắt)

### Thêm mới
| File | Mô tả |
|------|-------|
| `campaign-service/.../model/FeedPostRevision.java` | JPA entity |
| `campaign-service/.../repository/FeedPostRevisionRepository.java` | Spring Data JPA repo |
| `campaign-service/.../model/response/FeedPostRevisionResponse.java` | Response DTO |
| `deploy/init-scripts/migrate-add-feed-post-revisions.sql` | Migration DDL |
| `danbox/src/components/feed-post/PostRevisionHistoryModal.tsx` | Web modal component |
| `TrustFundMe-Mobile/lib/core/models/feed_post_revision_model.dart` | Mobile model |
| `TrustFundMe-Mobile/lib/widgets/feed/post_revision_history_sheet.dart` | Mobile bottom sheet |

### Sửa đổi
| File | Thay đổi |
|------|---------|
| `campaign-service/.../model/response/FeedPostResponse.java` | + `hasRevisions: boolean` |
| `campaign-service/.../service/impl/FeedPostServiceImpl.java` | + `snapshotRevision`, `getRevisions`, `getRevisionById`, `hasRevisions` trong `toResponse()`, `@Transactional` trên `update`/`updateContent` |
| `campaign-service/.../controller/FeedPostController.java` | + 2 revision endpoints, remove redundant Sort |
| `campaign-service/.../service/iface/FeedPostService.java` | + `getRevisions`, `getRevisionById` interface methods |
| `danbox/src/types/feedPost.ts` | + `RevisionMediaItem`, `FeedPostRevisionDto`, `RevisionPage`, `hasRevisions` vào `FeedPostDto` và `FeedPost` |
| `danbox/src/services/feedPostService.ts` | + `getRevisions`, `getRevisionById` |
| `danbox/src/lib/feedPostUtils.ts` | + `hasRevisions: dto.hasRevisions === true` |
| `danbox/src/components/feed-post/FeedPostDetail.tsx` | + "Đã chỉnh sửa" label, "Lịch sử" button, `PostRevisionHistoryModal` |
| `TrustFundMe-Mobile/lib/core/api/api_service.dart` | + `getFeedPostRevisions`, `getFeedPostRevisionById` |
| `TrustFundMe-Mobile/lib/core/models/feed_post_model.dart` | + `hasRevisions` field + fromJson + copyWith |
| `TrustFundMe-Mobile/lib/screens/feed_post_detail_screen.dart` | + "Đã chỉnh sửa" label, "Lịch sử chỉnh sửa" menu item |

---

## 9. Known Limitations (chấp nhận cho MVP)

1. **N+1 query**: `existsByPostId` chạy cho mỗi post trong feed list. Fix sau: thêm `hasRevisions` vào một batch query hoặc JOIN.
2. **No admin tracking**: `updateContentByAdmin()` không tạo revision. Fix sau: thêm `snapshotRevision` vào admin edit flow.
3. **Race condition**: 2 concurrent edits cùng post → 1 revision lost. Fix sau: pessimistic lock hoặc `REQUIRES_NEW` transaction với retry.
4. **No size cap**: `?size=10000` không bị giới hạn. Fix: `Math.min(size, 50)` trong controller.
5. **Old edits invisible**: Posts sửa trước khi feature deploy sẽ không có history (hasRevisions=false). Đây là behavior chủ động.
6. **No diff view**: Chỉ xem snapshot, không có diff/compare. Scope tương lai.
