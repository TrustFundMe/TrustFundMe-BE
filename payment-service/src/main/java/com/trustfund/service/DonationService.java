package com.trustfund.service;

import com.trustfund.dto.request.CreatePaymentRequest;
import com.trustfund.dto.response.PaymentResponse;
import com.trustfund.model.Donation;
import com.trustfund.model.DonationItem;
import com.trustfund.model.Payment;
import com.trustfund.repository.DonationItemRepository;
import com.trustfund.repository.DonationRepository;
import com.trustfund.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.client.RestTemplate;
import com.trustfund.dto.response.CampaignAnalyticsResponse;
import com.trustfund.dto.response.CampaignProgressResponse;
import com.trustfund.dto.response.MyDonationImpactResponse;
import com.trustfund.dto.response.RecentDonorResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

        private final PayOS payOS;
        private final DonationRepository donationRepository;
        private final DonationItemRepository donationItemRepository;
        private final PaymentRepository paymentRepository;
        private final RestTemplate restTemplate;

        @Qualifier("campaignEnrichmentRestTemplate")
        private final RestTemplate campaignEnrichmentRestTemplate;

        @Value("${app.frontend.url:http://localhost:3000}")
        private String frontendUrl;

        @Value("${app.campaign-service.url:http://localhost:8082}")
        private String campaignServiceUrl;

        @Transactional
        public PaymentResponse createPayment(CreatePaymentRequest request) throws Exception {
                Payment payment = Payment.builder()
                                .description(request.getDescription())
                                .amount(request.getDonationAmount().add(request.getTipAmount()))
                                .status("PENDING")
                                .build();

                Donation donation = Donation.builder()
                                .donorId(request.getDonorId())
                                .campaignId(request.getCampaignId())
                                .donationAmount(request.getDonationAmount())
                                .tipAmount(request.getTipAmount())
                                .totalAmount(request.getDonationAmount().add(request.getTipAmount()))
                                .status("PENDING")
                                .isAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous()
                                                : (request.getDonorId() == null))
                                .payment(payment)
                                .build();

                donation = donationRepository.save(donation);

                if (request.getItems() != null) {
                        final Donation finalDonation = donation;
                        List<DonationItem> items = request.getItems().stream()
                                        .map(itemReq -> DonationItem.builder()
                                                        .donation(finalDonation)
                                                        .expenditureItemId(itemReq.getExpenditureItemId())
                                                        .quantity(itemReq.getQuantity())
                                                        .amount(itemReq.getAmount())
                                                        .build())
                                        .collect(Collectors.toList());
                        donationItemRepository.saveAll(items);
                }

                // Generate unique orderCode using the current time substring and the donation
                // ID
                long orderCode = Long.parseLong(String.valueOf(System.currentTimeMillis()).substring(4)
                                + String.format("%04d", donation.getId() % 10000));

                payment.setOrderCode(orderCode);
                paymentRepository.save(payment);

                long totalAmountLong = donation.getTotalAmount().longValue();

                List<PaymentLinkItem> payosItems = List.of(
                                PaymentLinkItem.builder()
                                                .name("Donation for Campaign #" + request.getCampaignId())
                                                .quantity(1)
                                                .price(totalAmountLong)
                                                .build());

                String paymentDescription = request.getDescription() != null && !request.getDescription().isEmpty()
                                ? request.getDescription()
                                : "Campaign " + request.getCampaignId();

                // Truncate if too long (PayOS limit is usually 255 but banks truncate to 20-50
                // chars)
                if (paymentDescription.length() > 50) {
                        paymentDescription = paymentDescription.substring(0, 47) + "...";
                }

                CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                                .orderCode(orderCode)
                                .amount(totalAmountLong)
                                .description(paymentDescription)
                                .returnUrl(frontendUrl + "/donation/success?donationId=" + donation.getId())
                                .cancelUrl(frontendUrl + "/donation/cancel?donationId=" + donation.getId()
                                                + "&campaignId="
                                                + donation.getCampaignId())
                                .items(payosItems)
                                .build();

                CreatePaymentLinkResponse checkoutResponseData = payOS.paymentRequests().create(paymentData);

                payment.setPaymentLinkId(checkoutResponseData.getPaymentLinkId());
                payment.setQrCode(checkoutResponseData.getQrCode());
                paymentRepository.save(payment);

                return PaymentResponse.builder()
                                .paymentUrl(checkoutResponseData.getCheckoutUrl())
                                .qrCode(checkoutResponseData.getQrCode())
                                .paymentLinkId(checkoutResponseData.getPaymentLinkId())
                                .donationId(donation.getId())
                                .build();
        }

        @Transactional
        @SuppressWarnings("unchecked")
        public void handleWebhook(Map<String, Object> webhookBody) throws Exception {
                Map<String, Object> data = (Map<String, Object>) webhookBody.get("data");
                if (data == null) {
                        log.warn("Webhook received without data: {}", webhookBody);
                        return;
                }
                String paymentLinkId = String.valueOf(data.get("paymentLinkId"));
                String status = String.valueOf(data.get("status"));

                paymentRepository.findByPaymentLinkId(paymentLinkId).ifPresentOrElse(payment -> {
                        String oldPaymentStatus = payment.getStatus();
                        log.info("Found payment for linkId {}: oldStatus={}, newStatus={}", paymentLinkId,
                                        oldPaymentStatus, status);
                        payment.setStatus(status);
                        paymentRepository.save(payment);

                        donationRepository.findByPayment(payment).ifPresentOrElse(donation -> {
                                String oldDonationStatus = donation.getStatus();
                                log.info("Found donation {} for payment {}: oldStatus={}, newStatus={}",
                                                donation.getId(), payment.getId(), oldDonationStatus, status);
                                donation.setStatus(status);
                                donationRepository.save(donation);

                                if (!"PAID".equals(oldDonationStatus) && "PAID".equals(status)) {
                                        processQuantityUpdate(donation);
                                }

                                if (!"PAID".equals(status)
                                                || Boolean.TRUE.equals(donation.getIsBalanceSynchronized())) {
                                        log.info("No quantity update needed: status transition {} -> {}",
                                                        oldDonationStatus, status);
                                }
                        }, () -> log.warn("No donation found for payment id: {}", payment.getId()));
                }, () -> log.error("No payment found for paymentLinkId: {}", paymentLinkId));
                log.info("✅ Verified and Updated Payment [{}] and Donation status to: {}", paymentLinkId,
                                status);

        }

        @Transactional
        public void verifyPayment(Long donationId) throws Exception {
                Donation donation = donationRepository.findById(donationId)
                                .orElseThrow(() -> new RuntimeException("Donation not found"));

                if (donation.getPayment() != null) {
                        try {
                                if (donation.getPayment().getOrderCode() == null) {
                                        throw new RuntimeException("Payment orderCode is missing");
                                }
                                // Check real status from PayOS API using the payment's orderCode
                                vn.payos.model.v2.paymentRequests.PaymentLink payosData = payOS.paymentRequests()
                                                .get(donation.getPayment().getOrderCode());
                                String realStatus = payosData.getStatus().toString();

                                log.info("🔍 PayOS Status for Donation {}: {}", donationId, realStatus);

                                // Sync status if it's different
                                if (!realStatus.equals(donation.getStatus())) {
                                        log.info("🔄 Syncing status for Donation {}: {} -> {}", donationId,
                                                        donation.getStatus(), realStatus);
                                        String oldStatus = donation.getStatus();
                                        donation.setStatus(realStatus);
                                        if (donation.getPayment() != null) {
                                                donation.getPayment().setStatus(realStatus);
                                        }
                                        donationRepository.save(donation);

                                        if (!"PAID".equals(oldStatus) && "PAID".equals(realStatus)) {
                                                processQuantityUpdate(donation);
                                        }

                                        if ("PAID".equals(realStatus)
                                                        && !Boolean.TRUE.equals(donation.getIsBalanceSynchronized())) {
                                                // updateCampaignBalance(donation); // Removed to use FE-driven update
                                                // as requested
                                        }
                                }
                        } catch (Exception e) {
                                log.error("❌ Failed to verify payment with PayOS for donation {}: {}", donationId,
                                                e.getMessage());
                        }
                }
        }

        @Transactional(readOnly = true)
        public PaymentResponse getDonation(Long id) {
                return donationRepository.findById(id).map(donation -> PaymentResponse.builder()
                                .donationId(donation.getId())
                                .campaignId(donation.getCampaignId())
                                .donationAmount(donation.getDonationAmount())
                                .totalAmount(donation.getTotalAmount())
                                .status(donation.getPayment() != null ? donation.getPayment().getStatus() : "PENDING")
                                .paymentLinkId(donation.getPayment() != null ? donation.getPayment().getPaymentLinkId()
                                                : null)
                                .build())
                                .orElseThrow(() -> new RuntimeException("Donation not found with id: " + id));
        }

        public Map<String, Object> checkExpenditureItemLimit(Long expenditureItemId, Integer requestedQuantity) {
                try {
                        String url = campaignServiceUrl + "/api/expenditures/items/" + expenditureItemId;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemData = restTemplate.getForObject(url, Map.class);

                        if (itemData == null) {
                                throw new RuntimeException("Expenditure item not found in campaign service");
                        }

                        // Just read directly what campaign-service tells us is the quantityLeft
                        Integer quantityLeftFromDB = (Integer) itemData.get("quantityLeft");
                        int quantityLeft = (quantityLeftFromDB != null) ? quantityLeftFromDB : 0;

                        log.info("➔ Pre-payment Check limits: Item {} has quantity_left = {}", expenditureItemId,
                                        quantityLeft);

                        int requested = (requestedQuantity != null) ? requestedQuantity : 1;
                        boolean canDonateMore = requested <= quantityLeft;

                        Map<String, Object> response = new HashMap<>();
                        response.put("canDonateMore", canDonateMore);
                        response.put("quantityLeft", quantityLeft);
                        response.put("message", canDonateMore ? "Có thể nhận thêm"
                                        : "Số lượng quyên góp vượt quá giới hạn cho phép. Hiện tại chỉ còn lại: "
                                                        + quantityLeft);
                        return response;
                } catch (Exception e) {
                        log.error("Error checking expenditure item limit", e);
                        throw new RuntimeException("Could not verify expenditure item limit: " + e.getMessage());
                }
        }

        private void processQuantityUpdate(Donation donation) {
                log.info("🚀 TRIGGERING quantity update for donation: {}", donation.getId());
                List<DonationItem> items = donationItemRepository.findByDonationId(donation.getId());
                log.info("Found {} items for donation {}", items.size(), donation.getId());

                for (DonationItem item : items) {
                        try {
                                int amountToDeduct = item.getQuantity();
                                log.info("➔ Preparing to deduct quantity for item ID {}. Exact amount to deduct: {}",
                                                item.getExpenditureItemId(), amountToDeduct);

                                String updateUrl = campaignServiceUrl
                                                + "/api/expenditures/items/"
                                                + item.getExpenditureItemId()
                                                + "/update-quantity?amount="
                                                + amountToDeduct;

                                log.info("➔ Calling campaign-service: PUT {}", updateUrl);
                                restTemplate.put(updateUrl, null);
                                log.info("✅ SUCCESS: Deducted {} from quantityLeft for ExpenditureItem {}",
                                                amountToDeduct, item.getExpenditureItemId());
                        } catch (Exception e) {
                                log.error("❌ FAILED to deduct quantity for ExpenditureItem {}: {}",
                                                item.getExpenditureItemId(), e.getMessage());
                        }
                }
        }

        @Transactional(readOnly = true)
        public CampaignProgressResponse getCampaignProgress(Long campaignId) {
                log.info("📊 Getting campaign progress for campaignId: {}", campaignId);

                // 1. Total raised from paid donations
                BigDecimal raised = donationRepository.sumDonationAmountByCampaignId(campaignId);
                if (raised == null)
                        raised = BigDecimal.ZERO;

                // 2. Get goal amount from campaign-service
                BigDecimal goal = BigDecimal.ZERO;
                try {
                        String goalUrl = campaignServiceUrl + "/api/fundraising-goals/campaign/" + campaignId;
                        log.info("➔ Fetching all goals from: {}", goalUrl);
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> goals = restTemplate.getForObject(goalUrl, List.class);
                        if (goals != null && !goals.isEmpty()) {
                                log.info("✅ Goals fetched: count={}", goals.size());
                                // Find the active goal
                                for (Map<String, Object> g : goals) {
                                        if (Boolean.TRUE.equals(g.get("active"))
                                                        || Boolean.TRUE.equals(g.get("isActive"))) {
                                                if (g.get("targetAmount") != null) {
                                                        goal = new BigDecimal(g.get("targetAmount").toString());
                                                        log.info("✅ Found active goal: id={}, targetAmount={}",
                                                                        g.get("id"), goal);
                                                        break;
                                                }
                                        }
                                }
                        } else {
                                log.warn("⚠️ No goals found for campaignId: {}", campaignId);
                        }
                } catch (Exception e) {
                        log.warn("⚠️ Could not fetch goals from campaign-service: {}", e.getMessage());
                }

                // 3. Calculate percentage
                int pct = 0;
                if (goal.compareTo(BigDecimal.valueOf(0)) > 0) {
                        double raisedVal = raised.doubleValue();
                        double goalVal = goal.doubleValue();
                        double ratio = raisedVal / goalVal;
                        double calc = ratio * 100;
                        pct = (int) Math.round(calc);

                        log.info("📊 Calculation: ratio = {}, calc = {}, pct before 100 limit = {}", ratio, calc, pct);

                        if (pct > 100)
                                pct = 100;
                        if (pct == 0 && raisedVal > 0)
                                pct = 1;
                }

                log.info("📊 FINAL PROGRESS API for campaign {}: raised={}, goal={}, pct={}%", campaignId, raised, goal,
                                pct);

                return CampaignProgressResponse.builder()
                                .campaignId(campaignId)
                                .raisedAmount(raised)
                                .goalAmount(goal)
                                .progressPercentage(pct)
                                .build();
        }

        @Transactional(readOnly = true)
        public List<RecentDonorResponse> getRecentDonors(Long campaignId, int limit) {
                log.info("👥 Getting recent {} donors for campaignId: {}", limit, campaignId);
                List<Donation> recent = donationRepository.findRecentPaidDonationsByCampaignId(
                                campaignId, PageRequest.of(0, limit));

                return recent.stream().map(d -> {
                        log.info("👥 Processing donor: id={}, donorId={}, is_anonymous_raw={}",
                                        d.getId(), d.getDonorId(), d.getIsAnonymous());

                        // Strict check as user requested: is_anonymous == 1 OR donorId == null (guest)
                        boolean anon = d.getDonorId() == null || Boolean.TRUE.equals(d.getIsAnonymous());
                        log.info("👥 Donor {} anonymity determined: {}", d.getId(), anon);

                        String name = "Người ủng hộ ẩn danh";

                        String avatar = null;

                        if (!anon && d.getDonorId() != null) {
                                try {
                                        // Use internal API as identified in identity-service
                                        String userUrl = "http://identity-service/api/internal/users/" + d.getDonorId();
                                        log.info("➔ Fetching internal donor info from: {}", userUrl);
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> userData = restTemplate.getForObject(userUrl, Map.class);
                                        if (userData != null) {
                                                log.info("✅ Internal donor info fetched: {}", userData);
                                                // Response from InternalUserController.getUserInfo is the object itself
                                                if (userData.get("fullName") != null) {
                                                        name = userData.get("fullName").toString();
                                                }
                                                if (userData.get("avatarUrl") != null) {
                                                        avatar = userData.get("avatarUrl").toString();
                                                }
                                        }
                                } catch (Exception e) {
                                        log.warn("⚠️ Could not fetch user info from identity-service internal API for donorId {}: {}",
                                                        d.getDonorId(), e.getMessage());
                                }
                        }

                        return RecentDonorResponse.builder()
                                        .donationId(d.getId())
                                        .donorId(anon ? null : d.getDonorId())
                                        .donorName(name)
                                        .donorAvatar(avatar)
                                        .amount(d.getTotalAmount())
                                        .createdAt(d.getCreatedAt())
                                        .anonymous(anon)
                                        .build();
                }).collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<MyDonationImpactResponse> getMyPaidDonations(Long donorId, int limit) {
                List<Donation> rows = donationRepository.findByDonorIdAndStatusOrderByCreatedAtDesc(donorId, "PAID");
                if (limit > 0 && rows.size() > limit) {
                        rows = rows.subList(0, limit);
                }

                Set<Long> uniqueCampaignIds = rows.stream()
                                .map(Donation::getCampaignId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toCollection(HashSet::new));

                Map<Long, String> campaignTitleById = new HashMap<>();
                for (Long campaignId : uniqueCampaignIds) {
                        try {
                                String url = campaignServiceUrl + "/api/campaigns/" + campaignId;
                                @SuppressWarnings("unchecked")
                                Map<String, Object> campaignData = campaignEnrichmentRestTemplate.getForObject(url,
                                                Map.class);
                                if (campaignData != null && campaignData.get("title") != null) {
                                        campaignTitleById.put(campaignId, campaignData.get("title").toString());
                                }
                        } catch (Exception e) {
                                log.warn("Could not fetch campaign title for campaignId {}: {}",
                                                campaignId, e.getMessage());
                        }
                }

                return rows.stream().map(d -> {
                        String campaignTitle = d.getCampaignId() != null
                                        ? campaignTitleById.get(d.getCampaignId())
                                        : null;

                        return MyDonationImpactResponse.builder()
                                        .donationId(d.getId())
                                        .donorId(d.getDonorId())
                                        .campaignId(d.getCampaignId())
                                        .campaignTitle(campaignTitle)
                                        .donationAmount(d.getDonationAmount())
                                        .tipAmount(d.getTipAmount())
                                        .totalAmount(d.getTotalAmount())
                                        .status(d.getStatus())
                                        .anonymous(Boolean.TRUE.equals(d.getIsAnonymous()))
                                        .createdAt(d.getCreatedAt())
                                        .build();
                }).collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public CampaignAnalyticsResponse getCampaignAnalytics(Long campaignId, String period) {
                log.info("📈 Calculating analytics for campaignId: {}, period: {}", campaignId, period);

                // 1. Fetch campaign info (balance, approvedAt)
                BigDecimal currentBalance = BigDecimal.ZERO;
                java.time.LocalDateTime approvedAt = java.time.LocalDateTime.now();
                try {
                        String url = campaignServiceUrl + "/api/campaigns/" + campaignId;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> campaignData = campaignEnrichmentRestTemplate.getForObject(url, Map.class);
                        if (campaignData != null) {
                                if (campaignData.get("balance") != null) {
                                        currentBalance = new BigDecimal(campaignData.get("balance").toString());
                                }
                                if (campaignData.get("approvedAt") != null) {
                                        approvedAt = java.time.LocalDateTime
                                                        .parse(campaignData.get("approvedAt").toString());
                                } else if (campaignData.get("createdAt") != null) {
                                        approvedAt = java.time.LocalDateTime
                                                        .parse(campaignData.get("createdAt").toString());
                                }
                        }
                } catch (Exception e) {
                        log.warn("Could not fetch campaign details for analytics: {}", e.getMessage());
                }

                // 2. Fetch target amount
                BigDecimal targetAmount = BigDecimal.ZERO;
                try {
                        String goalUrl = campaignServiceUrl + "/api/fundraising-goals/campaign/" + campaignId;
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> goals = restTemplate.getForObject(goalUrl, List.class);
                        if (goals != null) {
                                for (Map<String, Object> g : goals) {
                                        if (Boolean.TRUE.equals(g.get("active"))
                                                        || Boolean.TRUE.equals(g.get("isActive"))) {
                                                if (g.get("targetAmount") != null) {
                                                        targetAmount = new BigDecimal(g.get("targetAmount").toString());
                                                        break;
                                                }
                                        }
                                }
                        }
                } catch (Exception e) {
                        log.warn("Could not fetch goals for analytics: {}", e.getMessage());
                }

                // 3. Define time filter based on period
                java.time.LocalDateTime filterStart = approvedAt;
                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                if ("Ngày".equalsIgnoreCase(period)) {
                        filterStart = now.minusDays(1);
                } else if ("Tuần".equalsIgnoreCase(period)) {
                        filterStart = now.minusWeeks(1);
                } else if ("Tháng".equalsIgnoreCase(period)) {
                        filterStart = now.minusMonths(1);
                } else if ("Năm".equalsIgnoreCase(period)) {
                        filterStart = now.minusYears(1);
                }

                if (filterStart.isBefore(approvedAt)) {
                        filterStart = approvedAt;
                }

                // 4. Fetch all transactions (for initial balance calc)
                List<Donation> allDonations = donationRepository.findByCampaignIdAndStatusOrderByCreatedAtAsc(
                                campaignId,
                                "PAID");
                List<Map<String, Object>> allExpenditures = new java.util.ArrayList<>();
                BigDecimal totalReceived = BigDecimal.ZERO;
                BigDecimal totalSpent = BigDecimal.ZERO;

                try {
                        String expUrl = campaignServiceUrl + "/api/expenditures/campaign/" + campaignId;
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> allExps = restTemplate.getForObject(expUrl, List.class);
                        if (allExps != null) {
                                for (Map<String, Object> exp : allExps) {
                                        if ("DISBURSED".equals(exp.get("status"))) {
                                                allExpenditures.add(exp);
                                        }
                                }
                        }
                } catch (Exception e) {
                        log.warn("Could not fetch expenditures for analytics: {}", e.getMessage());
                }

                // Merge events and calculate running balance
                class Event {
                        java.time.LocalDateTime time;
                        BigDecimal amount;
                        boolean isWithdrawal;

                        Event(java.time.LocalDateTime t, BigDecimal a, boolean w) {
                                time = t;
                                amount = a;
                                isWithdrawal = w;
                        }
                }

                List<Event> events = new java.util.ArrayList<>();
                for (Donation d : allDonations) {
                        events.add(new Event(d.getCreatedAt(), d.getDonationAmount(), false));
                }
                for (Map<String, Object> exp : allExpenditures) {
                        java.time.LocalDateTime t = exp.get("updatedAt") != null
                                        ? java.time.LocalDateTime.parse(exp.get("updatedAt").toString())
                                        : java.time.LocalDateTime.now();
                        BigDecimal a = new BigDecimal(exp.get("totalAmount").toString());
                        events.add(new Event(t, a, true));
                }

                events.sort(java.util.Comparator.comparing(e -> e.time));

                // Generate Timeline Points
                List<CampaignAnalyticsResponse.ChartPoint> chartData = new java.util.ArrayList<>();
                BigDecimal runningBalance = BigDecimal.ZERO;

                // Set date format based on period
                java.time.format.DateTimeFormatter dtf;
                if ("Ngày".equalsIgnoreCase(period)) {
                        dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
                } else if ("Năm".equalsIgnoreCase(period)) {
                        dtf = java.time.format.DateTimeFormatter.ofPattern("MM/yy");
                } else {
                        dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
                }

                // Always start with a point at approvedAt (or filterStart)
                chartData.add(CampaignAnalyticsResponse.ChartPoint.builder()
                                .date(approvedAt.format(dtf))
                                .balanceGreen(BigDecimal.ZERO)
                                .build());

                for (Event e : events) {
                        if (!e.isWithdrawal) {
                                runningBalance = runningBalance.add(e.amount);
                                totalReceived = totalReceived.add(e.amount);
                        } else {
                                runningBalance = runningBalance.subtract(e.amount);
                                totalSpent = totalSpent.add(e.amount);
                        }

                        // Only add to chart if it's within the period
                        if (e.time.isAfter(filterStart) || e.time.isEqual(filterStart)) {
                                String dateStr = e.time.format(dtf);
                                if (!e.isWithdrawal) {
                                        chartData.add(CampaignAnalyticsResponse.ChartPoint.builder()
                                                        .date(dateStr)
                                                        .balanceGreen(runningBalance)
                                                        .build());
                                } else {
                                        BigDecimal oldBalance = runningBalance.add(e.amount);
                                        // Vertical drop visual
                                        chartData.add(CampaignAnalyticsResponse.ChartPoint.builder()
                                                        .date(dateStr)
                                                        .balanceRed(oldBalance)
                                                        .build());
                                        chartData.add(CampaignAnalyticsResponse.ChartPoint.builder()
                                                        .date(dateStr)
                                                        .balanceRed(runningBalance)
                                                        .build());
                                        chartData.add(CampaignAnalyticsResponse.ChartPoint.builder()
                                                        .date(dateStr)
                                                        .balanceGreen(runningBalance)
                                                        .build());
                                }
                        }
                }

                // If no events in period, add a final point to maintain the line
                if (chartData.size() == 1) {
                        chartData.add(CampaignAnalyticsResponse.ChartPoint.builder()
                                        .date(now.format(dtf))
                                        .balanceGreen(runningBalance)
                                        .build());
                }

                return CampaignAnalyticsResponse.builder()
                                .campaignId(campaignId)
                                .totalReceived(totalReceived)
                                .totalSpent(totalSpent)
                                .currentBalance(currentBalance)
                                .targetAmount(targetAmount)
                                .approvedAt(approvedAt)
                                .chartData(chartData)
                                .build();
        }

        public List<Donation> getDonationsByStatus(String status) {
                return donationRepository.findByStatusOrderByCreatedAtDesc(status);
        }
}
