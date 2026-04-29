# Blockchain Hash Integration - Brainstorm & Implementation Guide

**Date:** 2026-04-28
**Budget:** $100
**Goal:** Chứng minh file minh chứng (evidence) không thể bị thay đổi sau khi upload

---

## 1. Phân tích hiện trạng

### Tech stack hiện tại
- **Backend:** Java 17 + Spring Boot 3.2.5 (Microservices)
- **Database:** MySQL 8.0 (JPA/Hibernate)
- **File storage:** Supabase Storage (S3-compatible)
- **Services liên quan:** `media-service` (upload/store), `campaign-service` (expenditure/evidence logic)

### Luồng evidence hiện tại
```
User upload file → MediaController → MediaServiceImpl.uploadMedia()
  → SupabaseStorageService.uploadFile() → lưu lên Supabase
  → Save metadata vào bảng `media` (url, fileName, contentType, sizeBytes)
  → KHÔNG có hash, KHÔNG có integrity check
```

### Vấn đề
- File upload xong **không có bằng chứng nào** chứng minh file không bị sửa
- Ai có quyền DB hoặc Supabase admin đều có thể **thay thế file** mà không ai biết
- Không có audit trail cho file integrity

---

## 2. Giải pháp đề xuất: SHA-256 Hash + Blockchain Anchoring

### Tại sao chọn cách này?

| Tiêu chí | Chỉ hash local | Hash + Blockchain anchor |
|-----------|----------------|--------------------------|
| Chống sửa bởi admin DB | Không | **Có** - hash on-chain không ai sửa được |
| Chi phí | $0 | ~$0.01-0.05/tx trên Polygon |
| Độ phức tạp | Thấp | Trung bình |
| Giá trị pháp lý | Thấp | Cao hơn - có timestamp on-chain |
| Budget $100 đủ không? | N/A | **Đủ cho ~2000-10000 transactions** |

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    EVIDENCE UPLOAD FLOW                       │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  User upload file                                             │
│       │                                                       │
│       ▼                                                       │
│  ┌──────────────────┐                                        │
│  │  media-service    │                                        │
│  │                    │                                        │
│  │  1. Nhận file bytes│                                       │
│  │  2. SHA-256(bytes) │──► fileHash = "0xabc123..."          │
│  │  3. Upload Supabase│──► url = "https://...supabase..."    │
│  │  4. Save to MySQL  │──► media row + fileHash column       │
│  │  5. Queue tx       │──► blockchain_hash_log (PENDING)     │
│  └──────────────────┘                                        │
│       │                                                       │
│       ▼ (async, scheduled job)                                │
│  ┌──────────────────┐                                        │
│  │  Blockchain        │                                       │
│  │  Anchor Service    │                                       │
│  │                    │                                        │
│  │  Option A: Đơn lẻ │──► 1 hash = 1 tx (~$0.01)            │
│  │  Option B: Batch   │──► N hash = 1 Merkle root tx         │
│  │                    │                                        │
│  │  Ghi lên Polygon   │──► txHash = "0xdef456..."            │
│  │  Update DB status  │──► ANCHORED + txHash                 │
│  └──────────────────┘                                        │
│       │                                                       │
│       ▼                                                       │
│  ┌──────────────────┐                                        │
│  │  Verify Endpoint   │                                       │
│  │                    │                                        │
│  │  Input: file       │                                       │
│  │  1. SHA-256(file)  │                                       │
│  │  2. Lookup hash DB │                                       │
│  │  3. Check on-chain │                                       │
│  │  Output: MATCH ✓   │                                       │
│  └──────────────────┘                                        │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Thiết kế chi tiết

### 3.1 Database Changes

#### Bảng `media` - thêm cột hash
```sql
ALTER TABLE media ADD COLUMN file_hash VARCHAR(64) AFTER size_bytes;
ALTER TABLE media ADD COLUMN hash_algorithm VARCHAR(10) DEFAULT 'SHA-256' AFTER file_hash;
```

#### Bảng mới `blockchain_hash_log` (trong media-service DB)
```sql
CREATE TABLE blockchain_hash_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    media_id BIGINT NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    chain_name VARCHAR(20) DEFAULT 'POLYGON',
    tx_hash VARCHAR(66),               -- "0x..." transaction hash
    block_number BIGINT,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SUBMITTED, ANCHORED, FAILED
    retry_count INT DEFAULT 0,
    error_message VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    anchored_at DATETIME,
    
    INDEX idx_media_id (media_id),
    INDEX idx_file_hash (file_hash),
    INDEX idx_status (status),
    INDEX idx_tx_hash (tx_hash)
);
```

### 3.2 Hashing tại upload time

**File cần sửa:** `MediaServiceImpl.java`

```java
// Thêm vào uploadMedia() - TRƯỚC khi upload Supabase
import java.security.MessageDigest;

byte[] fileBytes = request.getFile().getBytes();
String fileHash = computeSHA256(fileBytes);

// Upload to Supabase (dùng fileBytes đã có, không cần đọc lại)
// Save to media table với fileHash
// Insert vào blockchain_hash_log với status=PENDING
```

**Utility method:**
```java
private String computeSHA256(byte[] data) {
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    } catch (Exception e) {
        throw new RuntimeException("SHA-256 hashing failed", e);
    }
}
```

### 3.3 Blockchain Anchoring Service

#### Chọn chain: **Polygon PoS**

| Chain | Gas/tx | Thời gian confirm | Phù hợp? |
|-------|--------|-------------------|-----------|
| Ethereum | ~$0.50-5.00 | 15s | Quá đắt |
| **Polygon PoS** | **~$0.001-0.01** | **2-5s** | **Tốt nhất cho budget $100** |
| BSC | ~$0.03-0.10 | 3s | OK nhưng đắt hơn Polygon |
| Base | ~$0.001-0.01 | 2s | Tốt, nhưng ecosystem nhỏ hơn |
| Arbitrum | ~$0.01-0.05 | <1s | Tốt |

**Ước tính với Polygon:**
- $100 budget ≈ mua được ~150+ MATIC (giá ~$0.60-0.70)
- Mỗi tx ghi hash ≈ 0.001-0.005 MATIC
- **Đủ cho 30,000 - 150,000 transactions** - DƯ SỨC cho MVP

#### Smart Contract đơn giản (Solidity)

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EvidenceAnchor {
    event HashAnchored(
        bytes32 indexed fileHash,
        uint256 indexed mediaId,
        uint256 timestamp
    );

    // Ghi 1 hash
    function anchorHash(bytes32 fileHash, uint256 mediaId) external {
        emit HashAnchored(fileHash, mediaId, block.timestamp);
    }

    // Ghi batch nhiều hash (tiết kiệm gas)
    function anchorBatch(bytes32[] calldata fileHashes, uint256[] calldata mediaIds) external {
        require(fileHashes.length == mediaIds.length, "Length mismatch");
        for (uint256 i = 0; i < fileHashes.length; i++) {
            emit HashAnchored(fileHashes[i], mediaIds[i], block.timestamp);
        }
    }
}
```

**Tại sao dùng Event thay vì Storage?**
- Event (log) rẻ hơn ~5-10x so với storage write
- Vẫn immutable, queryable qua `eth_getLogs`
- Đủ cho mục đích "proof of existence"

#### Java Integration (Web3j)

**Thêm dependency vào `media-service/pom.xml`:**
```xml
<dependency>
    <groupId>org.web3j</groupId>
    <artifactId>core</artifactId>
    <version>4.12.3</version>
</dependency>
```

**BlockchainAnchorService.java:**
```java
@Service
public class BlockchainAnchorService {
    
    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;
    
    // Constructor: connect to Polygon RPC + load wallet
    
    // anchorHash(fileHash, mediaId) → send tx → return txHash
    
    // Scheduled job: poll blockchain_hash_log where status=PENDING
    //   → batch anchor → update status=ANCHORED
}
```

### 3.4 Verify Endpoint

**API:** `GET /api/media/{id}/verify` hoặc `POST /api/media/verify` (upload file để verify)

```
Input: file (multipart) hoặc mediaId
Output:
{
  "verified": true,
  "fileHash": "abc123...",
  "matchesDatabase": true,
  "blockchain": {
    "anchored": true,
    "txHash": "0xdef456...",
    "blockNumber": 12345678,
    "anchoredAt": "2026-04-28T10:30:00Z",
    "chainName": "Polygon",
    "explorerUrl": "https://polygonscan.com/tx/0xdef456..."
  }
}
```

---

## 4. Implementation Phases

### Phase 1: Local Hash (1-2 ngày) - KHÔNG CẦN BLOCKCHAIN
- Thêm `file_hash` column vào bảng `media`
- Compute SHA-256 khi upload
- Thêm verify endpoint (so sánh hash)
- **Giá trị ngay lập tức:** detect file tampering trong DB/storage

### Phase 2: Blockchain Anchoring (3-5 ngày)
- Deploy smart contract lên Polygon Mumbai testnet → rồi mainnet
- Tạo `BlockchainAnchorService` với Web3j
- Tạo `blockchain_hash_log` table
- Scheduled job anchor hash lên chain
- Verify endpoint check on-chain

### Phase 3: Verification UI (1-2 ngày)
- Trang verify cho user/admin
- Upload file → so sánh hash → hiển thị kết quả + link Polygonscan
- Badge "Blockchain Verified" trên evidence đã anchor

---

## 5. Chi phí ước tính

| Hạng mục | Chi phí | Ghi chú |
|----------|---------|---------|
| Polygon MATIC | ~$5-10 | Đủ cho hàng chục nghìn tx |
| RPC Provider | $0 | Polygon public RPC hoặc Alchemy free tier (300M compute units/tháng) |
| Smart contract deploy | ~$0.50-2 | Deploy 1 lần |
| Thời gian dev | 0 (tự làm) | 5-9 ngày tổng |
| **Tổng** | **~$5-12** | **Dư budget rất nhiều** |

---

## 6. Rủi ro & Mitigation

| Rủi ro | Mức độ | Cách xử lý |
|--------|--------|------------|
| Private key bị lộ | **CAO** | Dùng environment variable, KHÔNG hardcode. Production nên dùng KMS |
| Hash sai do serialize khác nhau | Trung bình | Hash raw bytes trực tiếp, không qua transform |
| Tx pending/fail | Trung bình | Retry mechanism + status tracking trong `blockchain_hash_log` |
| Polygon RPC downtime | Thấp | Fallback RPC endpoint (Alchemy + Infura + public) |
| Gas price spike | Thấp | Polygon gas rất rẻ, spike cũng chỉ ~$0.05/tx |

---

## 7. Câu hỏi quyết định

### Đã quyết định
- **Chain:** Polygon PoS (rẻ nhất, mature, có Polygonscan)
- **Approach:** Hash at upload + async blockchain anchor (Cách 1 + 2 hybrid)
- **Scope:** Chỉ file evidence (media linked to expenditure)

### Cần quyết định
1. **Anchor đơn lẻ hay batch?**
   - Đơn lẻ: đơn giản hơn, mỗi file 1 tx, ~$0.001/tx
   - Batch (mỗi 5-10 phút): tiết kiệm gas hơn nhưng phức tạp hơn
   - **Khuyến nghị:** Đơn lẻ trước, batch sau khi cần optimize

2. **Anchor tất cả media hay chỉ evidence?**
   - Tất cả: cover hết nhưng tốn gas hơn
   - Chỉ evidence (media có expenditureId): focused, rẻ hơn
   - **Khuyến nghị:** Chỉ evidence (có expenditureId hoặc expenditureItemId)

3. **Verify cho ai?**
   - Public: ai cũng verify được (minh bạch nhất)
   - Chỉ admin/staff
   - **Khuyến nghị:** Public - đây là selling point của tính năng

---

## 8. Files cần thay đổi

### media-service
| File | Thay đổi |
|------|----------|
| `model/Media.java` | Thêm `fileHash`, `hashAlgorithm` columns |
| `model/BlockchainHashLog.java` | **MỚI** - entity cho blockchain anchor log |
| `model/response/MediaFileResponse.java` | Thêm `fileHash`, `blockchainStatus` |
| `service/MediaServiceImpl.java` | Compute SHA-256 khi upload |
| `service/BlockchainAnchorService.java` | **MỚI** - Web3j integration |
| `repository/BlockchainHashLogRepository.java` | **MỚI** |
| `controller/MediaController.java` | Thêm verify endpoint |
| `controller/VerifyController.java` | **MỚI** - public verify page |
| `config/BlockchainConfig.java` | **MỚI** - RPC, contract address, wallet config |
| `pom.xml` | Thêm web3j dependency |
| `application.yml` | Thêm blockchain config properties |

### campaign-service
| File | Thay đổi |
|------|----------|
| Không cần sửa | Hash logic nằm hoàn toàn trong media-service |

### SQL Migration
```
V_add_file_hash.sql
V_create_blockchain_hash_log.sql
```

---

## 9. Tóm tắt

**Approach:** SHA-256 hash file tại thời điểm upload → lưu hash vào MySQL → async ghi hash lên Polygon blockchain → cung cấp verify endpoint cho public.

**Chi phí thực tế:** ~$5-12 (budget $100 dư rất nhiều)

**Thời gian:** 5-9 ngày (Phase 1: 1-2 ngày có hash ngay, Phase 2: 3-5 ngày có blockchain)

**Giá trị:**
- Mọi file evidence đều có "dấu vân tay" SHA-256 không thể giả mạo
- Hash được ghi lên blockchain công khai → bất kỳ ai đều verify được
- Link Polygonscan cho mỗi evidence → minh bạch tuyệt đối
- Chi phí gần như không đáng kể trên Polygon
