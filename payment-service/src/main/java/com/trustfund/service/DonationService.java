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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

        private final PayOS payOS;
        private final DonationRepository donationRepository;
        private final DonationItemRepository donationItemRepository;
        private final PaymentRepository paymentRepository;
        private final RestTemplate restTemplate;

        @Value("${app.frontend.url:http://localhost:3000}")
        private String frontendUrl;

        @Value("${app.campaign-service.url:http://campaign-service}")
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

                long orderCode = donation.getId();
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
                                .returnUrl(frontendUrl + "/donation/success?id=" + donation.getId())
                                .cancelUrl(frontendUrl + "/donation/cancel?id=" + donation.getId() + "&campaignId="
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
                                        log.info("🚀 TRIGGERING quantity update for donation: {}", donation.getId());
                                        List<DonationItem> items = donationItemRepository
                                                        .findByDonationId(donation.getId());
                                        log.info("Found {} items for donation {}", items.size(), donation.getId());

                                        for (DonationItem item : items) {
                                                try {
                                                        String updateUrl = campaignServiceUrl
                                                                        + "/api/expenditures/items/"
                                                                        + item.getExpenditureItemId()
                                                                        + "/update-quantity?amount="
                                                                        + item.getQuantity();
                                                        log.info("Calling campaign-service: PUT {}", updateUrl);
                                                        restTemplate.put(updateUrl, null);
                                                        log.info("✅ SUCCESS: Updated Item {} (amount {})",
                                                                        item.getExpenditureItemId(),
                                                                        item.getQuantity());
                                                } catch (Exception e) {
                                                        log.error("❌ FAILED to update Item {}: {}",
                                                                        item.getExpenditureItemId(), e.getMessage());
                                                }
                                        }
                                } else {
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
                                // Check real status from PayOS API using donation ID as orderCode
                                vn.payos.model.v2.paymentRequests.PaymentLink payosData = payOS.paymentRequests()
                                                .get(donation.getId());
                                String realStatus = payosData.getStatus().toString();

                                log.info("🔍 PayOS Status for Donation {}: {}", donationId, realStatus);

                                // Sync status if it's different
                                if (!realStatus.equals(donation.getStatus())) {
                                        log.info("🔄 Syncing status for Donation {}: {} -> {}", donationId,
                                                        donation.getStatus(), realStatus);
                                        donation.setStatus(realStatus);
                                        if (donation.getPayment() != null) {
                                                donation.getPayment().setStatus(realStatus);
                                        }
                                        donationRepository.save(donation);
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

                        Integer quantityFromDB = (Integer) itemData.get("quantity");
                        Integer quantityLeftFromDB = (Integer) itemData.get("quantityLeft");

                        // Fallback logic for quantities
                        int originalQuantity = (quantityFromDB != null) ? quantityFromDB : 0;
                        int quantityLeft = (quantityLeftFromDB != null) ? quantityLeftFromDB : originalQuantity;

                        int requested = (requestedQuantity != null) ? requestedQuantity : 1;

                        // Correct logic: Only block if requested > quantityLeft
                        boolean canDonateMore = requested <= quantityLeft;

                        Map<String, Object> response = new HashMap<>();
                        response.put("canDonateMore", canDonateMore);
                        response.put("currentTotal", originalQuantity - quantityLeft);
                        response.put("quantityLeft", quantityLeft);
                        response.put("message", canDonateMore ? "Có thể nhận thêm"
                                        : "Số lượng vượt giới hạn cho phép. Còn lại: " + quantityLeft);
                        return response;
                } catch (Exception e) {
                        log.error("Error checking expenditure item limit", e);
                        throw new RuntimeException("Could not verify expenditure item limit: " + e.getMessage());
                }
        }
}
