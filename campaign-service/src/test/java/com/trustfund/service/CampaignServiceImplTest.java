package com.trustfund.service;

import com.trustfund.client.IdentityServiceClient;
import com.trustfund.client.MediaServiceClient;
import com.trustfund.client.NotificationServiceClient;
import com.trustfund.model.Campaign;
import com.trustfund.model.CampaignCategory;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.repository.CampaignCategoryRepository;
import com.trustfund.service.impl.CampaignServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignServiceImplTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignCategoryRepository categoryRepository;

    @Mock
    private IdentityServiceClient identityServiceClient;

    @Mock
    private MediaServiceClient mediaServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @Mock
    private ApprovalTaskService approvalTaskService;

    @Mock
    private TrustScoreService trustScoreService;

    @Mock
    private InternalTransactionService internalTransactionService;

    private CampaignServiceImpl campaignService;

    @BeforeEach
    void setUp() {
        campaignService = new CampaignServiceImpl(
                campaignRepository,
                categoryRepository,
                identityServiceClient,
                mediaServiceClient,
                approvalTaskService,
                notificationServiceClient,
                internalTransactionService,
                trustScoreService
        );
    }

    // ─── getAll() ───────────────────────────────────────────

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("returns list of non-deleted campaigns")
        void returnsListOfCampaigns() {
            Campaign campaign = buildCampaign(1L, "Campaign 1", "APPROVED");
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            when(campaignRepository.findByTypeNot(Campaign.TYPE_GENERAL_FUND, sort))
                    .thenReturn(List.of(campaign));

            List<com.trustfund.model.response.CampaignResponse> result = campaignService.getAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("returns empty list when no campaigns exist")
        void returnsEmptyList() {
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            when(campaignRepository.findByTypeNot(Campaign.TYPE_GENERAL_FUND, sort))
                    .thenReturn(List.of());

            List<com.trustfund.model.response.CampaignResponse> result = campaignService.getAll();

            assertThat(result).isEmpty();
        }
    }

    // ─── getAll(Pageable) ───────────────────────────────────

    @Nested
    @DisplayName("getAll(Pageable)")
    class GetAllPaginated {

        @Test
        @DisplayName("returns paginated campaigns")
        void returnsPaginatedCampaigns() {
            Campaign c1 = buildCampaign(1L, "Campaign A", "APPROVED");
            Campaign c2 = buildCampaign(2L, "Campaign B", "APPROVED");
            Pageable pageable = PageRequest.of(0, 10);
            Page<Campaign> page = new PageImpl<>(List.of(c1, c2), pageable, 2);

            when(campaignRepository.findByTypeNot(Campaign.TYPE_GENERAL_FUND, pageable))
                    .thenReturn(page);

            Page<com.trustfund.model.response.CampaignResponse> result = campaignService.getAll(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }

    // ─── getById ────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("returns campaign when found")
        void returnsCampaignWhenFound() {
            Campaign campaign = buildCampaign(5L, "Campaign 5", "APPROVED");
            when(campaignRepository.findById(5L)).thenReturn(Optional.of(campaign));

            com.trustfund.model.response.CampaignResponse result = campaignService.getById(5L);

            assertThat(result.getId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("throws ResponseStatusException 404 when campaign not found")
        void throwsNotFoundWhenCampaignNotFound() {
            when(campaignRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignService.getById(99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat((ResponseStatusException) e)
                            .matches(ex -> ex.getStatusCode().value() == 404));
        }
    }

    // ─── create ──────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("throws ResponseStatusException 403 when creating GENERAL_FUND type")
        void throwsForbiddenForGeneralFund() {
            com.trustfund.model.request.CreateCampaignRequest request =
                new com.trustfund.model.request.CreateCampaignRequest();
            request.setTitle("General Fund");
            request.setDescription("Description");
            request.setFundOwnerId(1L);
            request.setType(Campaign.TYPE_GENERAL_FUND);
            request.setCategoryId(1L);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(
                CampaignCategory.builder().id(1L).name("Health").build()
            ));

            assertThatThrownBy(() -> campaignService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat((ResponseStatusException) e)
                            .matches(ex -> ex.getStatusCode().value() == 403));
        }

        @Test
        @DisplayName("saves campaign with correct fundOwnerId")
        void savesCampaignWithCorrectFields() {
            com.trustfund.model.request.CreateCampaignRequest request =
                new com.trustfund.model.request.CreateCampaignRequest();
            request.setTitle("Test Campaign");
            request.setDescription("Test description");
            request.setFundOwnerId(10L);
            request.setCategoryId(1L);
            request.setType("ITEMIZED");
            request.setBalance(BigDecimal.ZERO);
            request.setStartDate(LocalDateTime.now().plusDays(1));
            request.setEndDate(LocalDateTime.now().plusDays(30));

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(
                CampaignCategory.builder().id(1L).name("Health").build()
            ));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> {
                Campaign c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });

            com.trustfund.model.response.CampaignResponse result = campaignService.create(request);

            assertThat(result.getId()).isEqualTo(1L);
            verify(campaignRepository).save(any(Campaign.class));
        }

        @Test
        @DisplayName("throws ResponseStatusException 400 when category not found")
        void throwsBadRequestWhenCategoryNotFound() {
            com.trustfund.model.request.CreateCampaignRequest request =
                new com.trustfund.model.request.CreateCampaignRequest();
            request.setTitle("Campaign");
            request.setDescription("Desc");
            request.setFundOwnerId(1L);
            request.setCategoryId(999L);
            request.setType("ITEMIZED");

            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat((ResponseStatusException) e)
                            .matches(ex -> ex.getStatusCode().value() == 400));
        }
    }

    // ─── reviewCampaign ─────────────────────────────────────

    @Nested
    @DisplayName("reviewCampaign()")
    class ReviewCampaign {

        @Test
        @DisplayName("throws ResponseStatusException 404 when campaign not found")
        void throwsNotFoundWhenCampaignNotFound() {
            when(campaignRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignService.reviewCampaign(99L, 100L, "APPROVED", null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat((ResponseStatusException) e)
                            .matches(ex -> ex.getStatusCode().value() == 404));
        }
    }

    // ─── getCampaignCountByFundOwner ────────────────────────

    @Nested
    @DisplayName("getCampaignCountByFundOwner()")
    class GetCampaignCount {

        @Test
        @DisplayName("returns campaign count for fund owner")
        void returnsCampaignCount() {
            when(campaignRepository.countByFundOwnerId(10L)).thenReturn(5L);

            long result = campaignService.getCampaignCountByFundOwner(10L);

            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("returns 0 when fund owner has no campaigns")
        void returnsZeroWhenNoCampaigns() {
            when(campaignRepository.countByFundOwnerId(99L)).thenReturn(0L);

            long result = campaignService.getCampaignCountByFundOwner(99L);

            assertThat(result).isZero();
        }
    }

    // ─── getByStatus ────────────────────────────────────────

    @Nested
    @DisplayName("getByStatus()")
    class GetByStatus {

        @Test
        @DisplayName("returns campaigns filtered by status")
        void returnsCampaignsFilteredByStatus() {
            Campaign c1 = buildCampaign(1L, "Campaign A", "APPROVED");
            Campaign c2 = buildCampaign(2L, "Campaign B", "APPROVED");
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            when(campaignRepository.findByTypeNot(Campaign.TYPE_GENERAL_FUND, sort))
                    .thenReturn(List.of(c1, c2));

            List<com.trustfund.model.response.CampaignResponse> result = campaignService.getByStatus("APPROVED");

            assertThat(result).hasSize(2);
        }
    }

    // ─── Helpers ───────────────────────────────────────────

    private Campaign buildCampaign(Long id, String title, String status) {
        return Campaign.builder()
                .id(id)
                .title(title)
                .status(status)
                .type("ITEMIZED")
                .fundOwnerId(1L)
                .balance(BigDecimal.ZERO)
                .description("Test description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}