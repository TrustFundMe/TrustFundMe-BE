package com.trustfund.service;

import com.trustfund.dto.response.CheckItemLimitResponse;
import com.trustfund.model.Donation;
import com.trustfund.repository.DonationItemRepository;
import com.trustfund.repository.DonationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private DonationItemRepository donationItemRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private vn.payos.PayOS payOS;

    @InjectMocks
    private DonationService donationService;

    // ─── checkExpenditureItemLimit ──────────────────────────

    @Nested
    @DisplayName("checkExpenditureItemLimit()")
    class CheckExpenditureItemLimit {

        @Test
        @DisplayName("returns canDonateMore=true when quantity is available")
        void returnsCanDonateMoreWhenQuantityAvailable() {
            when(restTemplate.getForObject(anyString(), eq(java.util.Map.class)))
                    .thenReturn(java.util.Map.of(
                            "quantityLeft", 50,
                            "quantity", 100
                    ));

            CheckItemLimitResponse result = donationService.checkExpenditureItemLimit(1L, 5);

            assertThat(result.isCanDonateMore()).isTrue();
            assertThat(result.getQuantityLeft()).isEqualTo(50);
        }

        @Test
        @DisplayName("returns canDonateMore=false when quantity exceeded")
        void returnsCanDonateMoreFalseWhenExceeded() {
            when(restTemplate.getForObject(anyString(), eq(java.util.Map.class)))
                    .thenReturn(java.util.Map.of(
                            "quantityLeft", 3,
                            "quantity", 100
                    ));

            CheckItemLimitResponse result = donationService.checkExpenditureItemLimit(1L, 5);

            assertThat(result.isCanDonateMore()).isFalse();
        }

        @Test
        @DisplayName("returns canDonateMore=false when item not found (null response)")
        void returnsFalseWhenItemNotFound() {
            when(restTemplate.getForObject(anyString(), eq(java.util.Map.class)))
                    .thenReturn(null);

            CheckItemLimitResponse result = donationService.checkExpenditureItemLimit(1L, 5);

            assertThat(result.isCanDonateMore()).isFalse();
        }
    }

    // ─── getUserDonationCount ───────────────────────────────

    @Nested
    @DisplayName("getUserDonationCount()")
    class GetUserDonationCount {

        @Test
        @DisplayName("returns count of paid donations for user")
        void returnsPaidDonationCount() {
            when(donationRepository.countByDonorId(42L)).thenReturn(12L);

            Long result = donationService.getUserDonationCount(42L);

            assertThat(result).isEqualTo(12L);
        }

        @Test
        @DisplayName("returns 0 when user has no paid donations")
        void returnsZeroWhenNoDonations() {
            when(donationRepository.countByDonorId(99L)).thenReturn(0L);

            Long result = donationService.getUserDonationCount(99L);

            assertThat(result).isZero();
        }
    }

    // ─── existsDonationItem ────────────────────────────────

    @Nested
    @DisplayName("existsDonationItem()")
    class ExistsDonationItem {

        @Test
        @DisplayName("returns true when donation items exist")
        void returnsTrueWhenExists() {
            when(donationItemRepository.countByExpenditureItemIdAndDonationStatusIn(eq(5L), anyList()))
                    .thenReturn(1L);

            boolean result = donationService.existsDonationItem(5L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when no donation items exist")
        void returnsFalseWhenNotExists() {
            when(donationItemRepository.countByExpenditureItemIdAndDonationStatusIn(eq(99L), anyList()))
                    .thenReturn(0L);

            boolean result = donationService.existsDonationItem(99L);

            assertThat(result).isFalse();
        }
    }

    // ─── getDonationsByStatus ─────────────────────────────

    @Nested
    @DisplayName("getDonationsByStatus()")
    class GetDonationsByStatus {

        @Test
        @DisplayName("returns donations filtered by status")
        void returnsDonationsByStatus() {
            Donation d1 = buildDonation(1L, "PAID");
            Donation d2 = buildDonation(2L, "PAID");
            when(donationRepository.findByStatusOrderByCreatedAtDesc("PAID"))
                    .thenReturn(List.of(d1, d2));

            List<Donation> result = donationService.getDonationsByStatus("PAID");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no donations match status")
        void returnsEmptyWhenNoMatch() {
            when(donationRepository.findByStatusOrderByCreatedAtDesc("CANCELLED"))
                    .thenReturn(List.of());

            List<Donation> result = donationService.getDonationsByStatus("CANCELLED");

            assertThat(result).isEmpty();
        }
    }

    // ─── getTotalRaisedByCampaignIds ─────────────────────

    @Nested
    @DisplayName("getTotalRaisedByCampaignIds()")
    class GetTotalRaisedByCampaignIds {

        @Test
        @DisplayName("returns sum of paid donations for campaign ids")
        void returnsTotalRaised() {
            when(donationRepository.sumDonationAmountByCampaignIds(List.of(1L, 2L, 3L)))
                    .thenReturn(new BigDecimal("1500000"));

            BigDecimal result = donationService.getTotalRaisedByCampaignIds(List.of(1L, 2L, 3L));

            assertThat(result).isEqualByComparingTo(new BigDecimal("1500000"));
        }

        @Test
        @DisplayName("returns null-safe zero for null sum result")
        void returnsZeroWhenNull() {
            when(donationRepository.sumDonationAmountByCampaignIds(anyList()))
                    .thenReturn(null);

            BigDecimal result = donationService.getTotalRaisedByCampaignIds(List.of(1L));

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ─── Helpers ───────────────────────────────────────────

    private Donation buildDonation(Long id, String status) {
        return Donation.builder()
                .id(id)
                .donorId(1L)
                .campaignId(1L)
                .donationAmount(new BigDecimal("100000"))
                .tipAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100000"))
                .isAnonymous(false)
                .isBalanceSynchronized(false)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}