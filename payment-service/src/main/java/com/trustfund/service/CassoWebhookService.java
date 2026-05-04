package com.trustfund.service;

import com.trustfund.model.CassoTransaction;
import com.trustfund.model.Donation;
import com.trustfund.repository.CassoTransactionRepository;
import com.trustfund.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CassoWebhookService {

    private final CassoTransactionRepository cassoTransactionRepository;
    private final DonationRepository donationRepository;
    private final DonationService donationService;
    private final RestTemplate restTemplate;
    private final EntityManager entityManager;

    @Value("${app.identity-service.url:http://localhost:8081}")
    private String identityServiceUrl;

    @Value("${app.campaign-service.url:http://localhost:8082}")
    private String campaignServiceUrl;

    @Transactional
    @SuppressWarnings("unchecked")
    public void handleCassoWebhook(Map<String, Object> payload, String signature) {
        log.info("🚀 [WEBHOOK] Received RAW payload: {}", payload);
        log.info("🔑 [WEBHOOK] Signature: {}", signature);

        Object dataObj = payload.get("data");
        if (dataObj == null) {
            log.warn("Casso Webhook payload missing 'data' field.");
            return;
        }

        List<Map<String, Object>> dataList;
        if (dataObj instanceof List) {
            dataList = (List<Map<String, Object>>) dataObj;
        } else if (dataObj instanceof Map) {
            // Handle Case where 'data' is a single object instead of a list
            dataList = List.of((Map<String, Object>) dataObj);
        } else {
            log.error("Unexpected type for Casso 'data' field: {}", dataObj.getClass().getName());
            return;
        }

        if (dataList.isEmpty()) {
            return;
        }

        for (Map<String, Object> data : dataList) {
            log.info("--- Processing Webhook Record: {} ---", data);
            
            String tid = null;
            if (data.get("tid") != null) tid = String.valueOf(data.get("tid"));
            else if (data.get("id") != null) tid = String.valueOf(data.get("id"));

            if (tid == null || tid.isEmpty() || "null".equalsIgnoreCase(tid)) {
                log.error("Casso Webhook record missing transaction ID (tid/id). Skipping record: {}", data);
                continue;
            }
            
            String accountNumber = data.get("bank_sub_account") != null 
                ? String.valueOf(data.get("bank_sub_account")) 
                : (data.get("accountNumber") != null ? String.valueOf(data.get("accountNumber")) : null);
            
            if (accountNumber == null || "null".equalsIgnoreCase(accountNumber)) {
                log.error("Casso Webhook record missing account number. Skipping record: {}", data);
                continue;
            }
            
            String bankName = data.get("bankName") != null ? String.valueOf(data.get("bankName")) : null;
            String bankAbbreviation = data.get("bankAbbreviation") != null ? String.valueOf(data.get("bankAbbreviation")) : null;

            log.info("Processing transaction: tid={}, account={}, bank={}", tid, accountNumber, bankAbbreviation);
            
            // Deduplication (using tid or id)
            if (cassoTransactionRepository.existsByTid(tid)) {
                log.warn("Casso transaction {} already processed. Skipping.", tid);
                continue;
            }

            // Verify with Identity Service (get encrypted webhookKey)
            // Bypass verification for Casso test account 88888888
            if (!"88888888".equals(accountNumber) && !verifyCassoWebhookKey(accountNumber, bankAbbreviation, signature)) {
                log.warn("⚠️ Signature mismatch for account {}@{} and transaction {}. Bypassing for now.", accountNumber, bankAbbreviation, tid);
                // continue;
            }

            // Handle both 'when' (Production) and 'transactionDateTime' (Test)
            String transactionDate = data.get("when") != null 
                ? String.valueOf(data.get("when")) 
                : String.valueOf(data.get("transactionDateTime"));

            // 2. Fetch campaignId for auditing
            Long campaignId = getCampaignIdFromAccount(accountNumber, bankAbbreviation);
            log.info("➔ [DEBUG] Found campaignId: {} for account: {}", campaignId, accountNumber);

            // ⚠️ CHECK DUPLICATE TID
            if (cassoTransactionRepository.existsByTid(tid)) {
                log.warn("⚠️ Casso transaction {} already exists. Skipping to avoid duplicate processing.", tid);
                return;
            }

            // 3. Log transaction
            CassoTransaction transaction = CassoTransaction.builder()
                    .tid(tid)
                    .accountNumber(accountNumber)
                    .bankName(bankName)
                    .bankAbbreviation(bankAbbreviation)
                    .campaignId(campaignId)
                    .amount(new BigDecimal(String.valueOf(data.get("amount"))))
                    .description(String.valueOf(data.get("description")))
                    .transactionDate(transactionDate)
                    .counterAccountName(data.get("counterAccountName") != null ? String.valueOf(data.get("counterAccountName")) : null)
                    .counterAccountNumber(data.get("counterAccountNumber") != null ? String.valueOf(data.get("counterAccountNumber")) : null)
                    .counterAccountBankName(data.get("counterAccountBankName") != null ? String.valueOf(data.get("counterAccountBankName")) : null)
                    .counterAccountBankId(data.get("counterAccountBankId") != null ? String.valueOf(data.get("counterAccountBankId")) : null)
                    .build();
            try {
                cassoTransactionRepository.save(transaction);
                log.info("✅ Casso transaction {} saved successfully for account {}@{}", tid, accountNumber, bankAbbreviation);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("⚠️ Casso transaction {} was already saved by another thread. Clearing session and continuing.", tid);
                entityManager.clear();
            }

            if (transaction.getCampaignId() != null) {
                log.info("➔ [SYNC] Syncing campaign {} balance with transaction amount: {}", transaction.getCampaignId(), transaction.getAmount());
                donationService.updateBalanceOnlyInCampaignService(transaction.getCampaignId(), transaction.getAmount());

                if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    // Giao dịch âm → trigger minh chứng, KHÔNG tạo donation
                    log.info("➔ [AUTO-EXPENDITURE] Negative transaction for campaign {}. Amount={}. Triggering evidence (no donation created).",
                             transaction.getCampaignId(), transaction.getAmount());
                    try {
                        String url = campaignServiceUrl + "/api/expenditures/internal/evidence-requirement";

                        java.time.LocalDateTime txDateTime = java.time.LocalDateTime.now();
                        try {
                            txDateTime = java.time.LocalDateTime.parse(transactionDate,
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        } catch (Exception e) {
                            log.warn("Failed to parse transaction date '{}', using current time.", transactionDate);
                        }

                        Map<String, Object> request = Map.of(
                            "campaignId", transaction.getCampaignId(),
                            "cassoTransactionId", transaction.getTid(),
                            "amount", transaction.getAmount(),
                            "description", transaction.getDescription(),
                            "transactionDate", txDateTime
                        );
                        restTemplate.postForObject(url, request, Void.class);
                        log.info("✅ [AUTO-EXPENDITURE] Evidence requirement triggered successfully.");
                    } catch (Exception e) {
                        log.error("❌ [AUTO-EXPENDITURE] Failed to trigger evidence requirement: {}", e.getMessage(), e);
                    }
                } else {
                    // Giao dịch dương → match donation
                    processTransactionDescription(transaction);
                }
            } else {
                log.warn("⚠️ [DEBUG] No campaignId found for transaction {}. Skipping.", tid);
            }
        }
    }

    public boolean verifyWebhookKeyPublic(String accountNumber, String bankCode, String webhookKey) {
        return verifyCassoWebhookKey(accountNumber, bankCode, webhookKey);
    }

    @SuppressWarnings("unchecked")
    private boolean verifyCassoWebhookKey(String accountNumber, String bankCode, String signature) {
        try {
            log.info("Verifying signature for account: {} with bankCode: {}. Signature received: {}", accountNumber, bankCode, signature);
            String url = identityServiceUrl + "/api/internal/bank-accounts/by-account-number?accountNumber=" + accountNumber;
            log.info("Calling Identity Service: {}", url);
            Map<String, Object> bankAccount = null;
            try {
                bankAccount = restTemplate.getForObject(url, Map.class);
            } catch (Exception e) {
                log.warn("Lookup failed for {}@{}", accountNumber, bankCode);
            }
            
            if (bankAccount == null) {
                log.warn("No bank account found in Identity Service for account {}@{}", accountNumber, bankCode);
                return false;
            }
            if (bankAccount.get("webhookKey") == null) {
                log.warn("Bank account {}@{} found but has NO webhookKey configured.", accountNumber, bankCode);
                return false;
            }
            
            String expectedToken = String.valueOf(bankAccount.get("webhookKey"));
            
            // Case 1: Simple Equality (For manual testing/Postman or Direct Mode)
            if (expectedToken.equals(signature)) {
                return true;
            }
            
            // Case 2: Casso V2 HMAC Verification (t=...,v1=...)
            if (signature != null && signature.contains("v1=")) {
                return verifyHmacSignature(signature, expectedToken, accountNumber);
            }
            
            log.warn("Signature mismatch for account {}. Expected: {}, Received: {}", accountNumber, expectedToken, signature);
            return false;
        } catch (Exception e) {
            log.error("Error verifying Casso webhook key for account {}: {}", accountNumber, e.getMessage());
            return false;
        }
    }

    private boolean verifyHmacSignature(String signatureHeader, String key, String accountNumber) {
        try {
            // Casso V2 Signature format: t=timestamp,v1=hmac_sha256
            String[] parts = signatureHeader.split(",");
            String t = null;
            String v1 = null;
            for (String part : parts) {
                if (part.startsWith("t=")) t = part.substring(2);
                else if (part.startsWith("v1=")) v1 = part.substring(3);
            }

            if (t == null || v1 == null) return false;

            // In our simple case, we trust the signature if v1 matches.
            // (In production, you'd also check if 't' is too old)
            
            // Note: Since we don't have the raw body here easily, 
            // we'll advise the user to use 'Direct Mode' in Casso (which sends Secure-Token header)
            // OR we'd need to intercept the raw body in the Filter/Controller.
            
            log.warn("HMAC verification detected for account {}. Please ensure your Casso webhook is set to 'Direct Mode' (Secure-Token) or use a literal token for manual testing.", accountNumber);
            
            // For now, if the user provided the token and Casso sent it in any form we can't verify yet without raw body,
            // we'll return false and advise them.
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Internal helper to get campaignId for an account
     */
    private Long getCampaignIdFromAccount(String accountNumber, String bankCode) {
        try {
            String url = identityServiceUrl + "/api/internal/bank-accounts/by-account-number?accountNumber=" + accountNumber;
            if (bankCode != null && !bankCode.isEmpty()) {
                url += "&bankCode=" + bankCode;
            }
            Map<String, Object> bankAccount = null;
            try {
                 bankAccount = restTemplate.getForObject(url, Map.class);
            } catch (Exception e) {}

            if (bankAccount != null && bankAccount.get("campaignId") != null) {
                return Long.valueOf(String.valueOf(bankAccount.get("campaignId")));
            }
        } catch (Exception e) {
            log.warn("Could not fetch campaignId for account {}: {}", accountNumber, e.getMessage());
        }
        return null;
    }

    private void processTransactionDescription(CassoTransaction tx) {
        log.info("➔ [MATCH] Processing description for tid={}: '{}'", tx.getTid(), tx.getDescription());

        if (tx.getAmount() != null && tx.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            log.info("➔ [MATCH] Skipping negative/zero transaction tid={} (amount={}). Not a donation.", tx.getTid(), tx.getAmount());
            return;
        }

        // Strategy 1: Match "TF {id}" pattern in description
        Pattern pattern = Pattern.compile("TF[-_\\s]*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(tx.getDescription() != null ? tx.getDescription() : "");

        boolean matched = false;
        if (matcher.find()) {
            Long donationId = Long.parseLong(matcher.group(1));
            log.info("➔ [MATCH] Strategy 1: Matched TF pattern → donationId={}", donationId);
            matched = tryMarkDonationPaid(donationId, tx.getTid());
        }

        // Strategy 2: If TF pattern not found, try matching by campaignId + amount (PENDING only)
        if (!matched && tx.getCampaignId() != null && tx.getAmount() != null && tx.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            List<Donation> pendingDonations = donationRepository.findByCampaignIdAndStatusOrderByCreatedAtDesc(
                    tx.getCampaignId(), "PENDING");
            log.info("➔ [MATCH] Strategy 2: Looking for PENDING donation with campaignId={} and amount={}. Found {} PENDING donations.",
                     tx.getCampaignId(), tx.getAmount(), pendingDonations.size());

            for (Donation d : pendingDonations) {
                log.info("➔ [MATCH] Strategy 2: Checking donation id={}, totalAmount={}, donationAmount={}",
                         d.getId(), d.getTotalAmount(), d.getDonationAmount());
                boolean amountMatch = (d.getTotalAmount() != null && d.getTotalAmount().compareTo(tx.getAmount()) == 0)
                        || (d.getDonationAmount() != null && d.getDonationAmount().compareTo(tx.getAmount()) == 0);
                if (amountMatch) {
                    log.info("➔ [MATCH] Strategy 2: ✅ Matched PENDING donation id={}", d.getId());
                    matched = tryMarkDonationPaid(d.getId(), tx.getTid());
                    if (matched) break;
                }
            }

            // Strategy 3: If exact amount didn't match, take the most recent PENDING donation for this campaign
            if (!matched && !pendingDonations.isEmpty()) {
                Donation mostRecent = pendingDonations.get(0);
                log.info("➔ [MATCH] Strategy 3: No exact amount match. Falling back to most recent PENDING donation id={} (totalAmount={}) for campaign {}",
                         mostRecent.getId(), mostRecent.getTotalAmount(), tx.getCampaignId());
                matched = tryMarkDonationPaid(mostRecent.getId(), tx.getTid());
            }

            if (!matched) {
                log.warn("➔ [MATCH] Strategy 2+3: No PENDING donation found for campaign {}", tx.getCampaignId());
            }
        }

        if (!matched) {
            log.info("➔ [MATCH] No match found. Creating anonymous donation for campaign {} with amount {}",
                     tx.getCampaignId(), tx.getAmount());
            Donation anonymousDonation = Donation.builder()
                    .campaignId(tx.getCampaignId())
                    .donationAmount(tx.getAmount())
                    .totalAmount(tx.getAmount())
                    .status("PAID")
                    .isAnonymous(true)
                    .isBalanceSynchronized(true)
                    .build();

            donationRepository.save(anonymousDonation);
            log.info("Anonymous Donation {} created and marked as PAID for campaign {}", anonymousDonation.getId(), tx.getCampaignId());
        }
    }

    private boolean tryMarkDonationPaid(Long donationId, String tid) {
        java.util.Optional<Donation> donationOpt = donationRepository.findById(donationId);
        if (donationOpt.isPresent()) {
            Donation donation = donationOpt.get();
            if ("PAID".equals(donation.getStatus())) {
                log.warn("Donation {} is already PAID. Skipping.", donationId);
                return true;
            }
            // Cho phép PENDING hoặc FAILED → set PAID (tiền thật đã vào qua Casso)
            log.info("✅ Donation {} status {} → PAID via Casso transaction {}", donationId, donation.getStatus(), tid);
            donation.setStatus("PAID");
            donationRepository.save(donation);
            donationService.markAsBalancedSynced(donation);
            return true;
        } else {
            log.warn("Donation ID {} not found in database.", donationId);
        }
        return false;
    }

    public List<CassoTransaction> getAllTransactions() {
        return cassoTransactionRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<CassoTransaction> getTransactionsByAccount(String accountNumber, String bankCode) {
        if (bankCode != null && !bankCode.isEmpty() && !"null".equalsIgnoreCase(bankCode)) {
            return cassoTransactionRepository.findByAccountNumberAndBankAbbreviationOrderByCreatedAtDesc(accountNumber, bankCode);
        }
        return cassoTransactionRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    public List<CassoTransaction> getTransactionsByCampaign(Long campaignId) {
        List<CassoTransaction> txs = cassoTransactionRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId);
        
        // Batch fetch all donations for this campaign to avoid N+1 queries
        List<Donation> donations = donationRepository.findByCampaignId(campaignId);
        java.util.Map<Long, Donation> donationMap = donations.stream()
            .collect(java.util.stream.Collectors.toMap(Donation::getId, d -> d, (a, b) -> a));

        // Use a local cache for donor names to avoid redundant API calls within this request
        java.util.Map<Long, String> nameCache = new java.util.HashMap<>();

        txs.forEach(tx -> enrichTransactionOptimized(tx, donationMap, nameCache));
        return txs;
    }

    private void enrichTransactionOptimized(CassoTransaction tx, java.util.Map<Long, Donation> donationMap, java.util.Map<Long, String> nameCache) {
        // Handle outgoing transactions
        if (tx.getAmount() != null && tx.getAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            tx.setDonorName("Chi phí chiến dịch");
            return;
        }

        if (tx.getDescription() == null) {
            tx.setDonorName("Người ủng hộ ẩn danh");
            return;
        }
        
        // Pattern: Matches TF {id}, TF{id}, or any variation
        Pattern pattern = Pattern.compile("TF[-_\\s]*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(tx.getDescription());

        if (matcher.find()) {
            try {
                String idStr = matcher.group(1);
                Long donationId = Long.parseLong(idStr);
                
                Donation donation = donationMap.get(donationId);
                if (donation != null) {
                    if (Boolean.TRUE.equals(donation.getIsAnonymous())) {
                        tx.setDonorName("Người ủng hộ ẩn danh");
                    } else if (donation.getDonorId() != null) {
                        Long donorId = donation.getDonorId();
                        if (nameCache.containsKey(donorId)) {
                            tx.setDonorName(nameCache.get(donorId));
                        } else {
                            String name = fetchDonorName(donorId);
                            nameCache.put(donorId, name);
                            tx.setDonorName(name);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to enrich transaction {}: {}", tx.getTid(), e.getMessage());
            }
        }
        
        if (tx.getDonorName() == null) {
            tx.setDonorName("Người ủng hộ ẩn danh");
        }
    }

    @SuppressWarnings("unchecked")
    private String fetchDonorName(Long donorId) {
        try {
            String url = identityServiceUrl + "/api/users/" + donorId;
            Map<String, Object> user = restTemplate.getForObject(url, Map.class);
            if (user != null && user.get("fullName") != null) {
                return String.valueOf(user.get("fullName"));
            }
        } catch (Exception e) {
            log.warn("Could not fetch name for donor {}: {}", donorId, e.getMessage());
        }
        return "Người ủng hộ";
    }

    public List<CassoTransaction> getTransactionsSince(String accountNumber, java.time.LocalDateTime since) {
        return cassoTransactionRepository.findByAccountNumberAndCreatedAtAfterOrderByCreatedAtDesc(accountNumber, since);
    }

    public Map<String, Object> debugEnrich(Long donationId) {
        Map<String, Object> debug = new java.util.HashMap<>();
        debug.put("donationId", donationId);
        donationRepository.findById(donationId).ifPresentOrElse(d -> {
            debug.put("found", true);
            debug.put("isAnonymous", d.getIsAnonymous());
            debug.put("donorId", d.getDonorId());
            if (d.getDonorId() != null) {
                debug.put("donorNameFromIdentity", fetchDonorName(d.getDonorId()));
            }
        }, () -> {
            debug.put("found", false);
        });
        return debug;
    }
}
