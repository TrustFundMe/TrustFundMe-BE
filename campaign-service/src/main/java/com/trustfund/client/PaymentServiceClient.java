package com.trustfund.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
public class PaymentServiceClient {

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public PaymentServiceClient(RestTemplate restTemplate,
            @Value("${payment.service.url:http://localhost:8083}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl.trim().replaceAll("/$", "");
    }

    /**
     * Lấy tổng số lượng donation thực tế cho danh sách expenditure item ids.
     * Trả về BigDecimal tổng = sum(donatedQuantity * expectedPrice) của các item đã donated.
     * @param expenditureItemIds Danh sách ID của các ExpenditureItem
     * @param unitPrices Danh sách đơn giá tương ứng (chỉ số đơn giá, không dùng planned quantity)
     */
    public BigDecimal getTotalDonatedAmount(List<Long> expenditureItemIds, List<BigDecimal> unitPrices) {
        if (expenditureItemIds == null || expenditureItemIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String idsParam = expenditureItemIds.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        String url = paymentServiceUrl + "/api/payments/donations/summary?expenditureItemIds=" + idsParam;
        try {
            log.info("Calling payment service: {}", url);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object>[] response = (java.util.Map<String, Object>[]) restTemplate.getForObject(url, java.util.Map[].class);
            log.info("Payment service response: {}", (Object) java.util.Arrays.deepToString(response));
            if (response == null || response.length == 0) {
                log.warn("Payment service returned empty/null response for items: {}", expenditureItemIds);
                return BigDecimal.ZERO;
            }
            // Build lookup map: expenditureItemId -> donatedAmount
            java.util.Map<Long, BigDecimal> donatedAmountMap = new java.util.HashMap<>();
            for (java.util.Map<String, Object> entry : response) {
                Object idObj = entry.get("expenditureItemId");
                Object donatedObj = entry.get("donatedAmount");
                if (idObj != null && donatedObj != null) {
                    long itemId = ((Number) idObj).longValue();
                    BigDecimal donatedAmt = new BigDecimal(donatedObj.toString());
                    donatedAmountMap.put(itemId, donatedAmt);
                    log.info("Donation summary - itemId={}, donatedAmount={}", itemId, donatedAmt);
                }
            }

            // Sum total donated amounts across all expenditure items
            BigDecimal total = BigDecimal.ZERO;
            for (Long itemId : expenditureItemIds) {
                BigDecimal donatedAmt = donatedAmountMap.get(itemId);
                if (donatedAmt != null && donatedAmt.compareTo(BigDecimal.ZERO) > 0) {
                    total = total.add(donatedAmt);
                    log.info("Adding item {}: donatedAmount={}", itemId, donatedAmt);
                }
            }
            log.info("Total donated amount for items {}: {}", expenditureItemIds, total);
            return total;
        } catch (Exception e) {
            log.error("Failed to fetch donation summary from payment service: {} - {}", e.getClass().getName(), e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }
}