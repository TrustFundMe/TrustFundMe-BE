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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

        private final PayOS payOS;
        private final DonationRepository donationRepository;
        private final DonationItemRepository donationItemRepository;
        private final PaymentRepository paymentRepository;

        @Value("${app.frontend.url:http://localhost:3000}")
        private String frontendUrl;

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

                paymentRepository.findByPaymentLinkId(paymentLinkId).ifPresent(payment -> {
                        payment.setStatus(status);
                        paymentRepository.save(payment);

                        // Also update Donation status
                        donationRepository.findByPayment(payment).ifPresent(donation -> {
                                donation.setStatus(status);
                                donationRepository.save(donation);
                        });

                        log.info("✅ Verified and Updated Payment [{}] and Donation status to: {}", paymentLinkId,
                                        status);
                });
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
}
