package com.trustfund.service;

import com.trustfund.model.Campaign;
import com.trustfund.model.FundraisingGoal;
import com.trustfund.model.request.CreateFundraisingGoalRequest;
import com.trustfund.model.request.UpdateFundraisingGoalRequest;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.repository.FundraisingGoalRepository;
import com.trustfund.service.impl.FundraisingGoalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundraisingGoalServiceImplTest {

    @Mock
    private FundraisingGoalRepository fundraisingGoalRepository;

    @Mock
    private CampaignRepository campaignRepository;

    private FundraisingGoalServiceImpl fundraisingGoalService;

    @BeforeEach
    void setUp() {
        fundraisingGoalService = new FundraisingGoalServiceImpl(
                fundraisingGoalRepository,
                campaignRepository
        );
    }

    // ─── getAll ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("returns all goals")
        void returnsAllGoals() {
            FundraisingGoal goal1 = buildGoal(1L, 10L, BigDecimal.valueOf(5000));
            FundraisingGoal goal2 = buildGoal(2L, 20L, BigDecimal.valueOf(10000));

            when(fundraisingGoalRepository.findAll()).thenReturn(List.of(goal1, goal2));

            List<FundraisingGoal> result = fundraisingGoalService.getAll();

            assertThat(result).hasSize(2);
            verify(fundraisingGoalRepository).findAll();
        }

        @Test
        @DisplayName("returns empty list when no goals exist")
        void returnsEmptyList() {
            when(fundraisingGoalRepository.findAll()).thenReturn(Collections.emptyList());

            List<FundraisingGoal> result = fundraisingGoalService.getAll();

            assertThat(result).isEmpty();
        }
    }

    // ─── getById ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("returns goal when found")
        void returnsGoalWhenFound() {
            FundraisingGoal goal = buildGoal(5L, 10L, BigDecimal.valueOf(5000));
            when(fundraisingGoalRepository.findById(5L)).thenReturn(Optional.of(goal));

            FundraisingGoal result = fundraisingGoalService.getById(5L);

            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getTargetAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        }

        @Test
        @DisplayName("throws ResponseStatusException 404 when not found")
        void throwsNotFoundWhenNotFound() {
            when(fundraisingGoalRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fundraisingGoalService.getById(99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat((ResponseStatusException) e)
                            .matches(ex -> ex.getStatusCode().value() == 404));
        }
    }

    // ─── getByCampaignId ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByCampaignId()")
    class GetByCampaignId {

        @Test
        @DisplayName("filters correctly by campaign id")
        void filtersCorrectlyByCampaignId() {
            FundraisingGoal goal1 = buildGoal(1L, 10L, BigDecimal.valueOf(3000));
            FundraisingGoal goal2 = buildGoal(2L, 10L, BigDecimal.valueOf(5000));

            when(fundraisingGoalRepository.findByCampaignId(10L))
                    .thenReturn(List.of(goal1, goal2));

            List<FundraisingGoal> result = fundraisingGoalService.getByCampaignId(10L);

            assertThat(result).hasSize(2);
            result.forEach(g -> assertThat(g.getCampaignId()).isEqualTo(10L));
        }

        @Test
        @DisplayName("returns empty list when no goals for campaign")
        void returnsEmptyListWhenNoGoals() {
            when(fundraisingGoalRepository.findByCampaignId(99L))
                    .thenReturn(Collections.emptyList());

            List<FundraisingGoal> result = fundraisingGoalService.getByCampaignId(99L);

            assertThat(result).isEmpty();
        }
    }

    // ─── create ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("saves goal when campaign exists")
        void savesGoalWhenCampaignExists() {
            Long campaignId = 7L;
            Campaign campaign = Campaign.builder()
                    .id(campaignId)
                    .title("Help Needy")
                    .status("APPROVED")
                    .build();

            CreateFundraisingGoalRequest request = CreateFundraisingGoalRequest.builder()
                    .campaignId(campaignId)
                    .targetAmount(BigDecimal.valueOf(10000))
                    .description("Help the community with this goal")
                    .isActive(true)
                    .build();

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
            when(fundraisingGoalRepository.findByCampaignIdAndIsActive(campaignId, true))
                    .thenReturn(Collections.emptyList());
            when(fundraisingGoalRepository.save(any(FundraisingGoal.class))).thenAnswer(inv -> {
                FundraisingGoal g = inv.getArgument(0);
                g.setId(1L);
                return g;
            });

            FundraisingGoal result = fundraisingGoalService.create(request);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCampaignId()).isEqualTo(campaignId);
            assertThat(result.getTargetAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
            assertThat(result.getIsActive()).isTrue();
            verify(fundraisingGoalRepository).save(any(FundraisingGoal.class));
        }

        @Test
        @DisplayName("throws ResponseStatusException 404 when campaign not found")
        void throwsNotFoundWhenCampaignNotFound() {
            CreateFundraisingGoalRequest request = CreateFundraisingGoalRequest.builder()
                    .campaignId(999L)
                    .targetAmount(BigDecimal.valueOf(5000))
                    .description("Test description here")
                    .isActive(true)
                    .build();

            when(campaignRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fundraisingGoalService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat((ResponseStatusException) e)
                            .matches(ex -> ex.getStatusCode().value() == 404));
        }

        @Test
        @DisplayName("throws ResponseStatusException 403 when campaign is disabled")
        void throwsForbiddenWhenCampaignDisabled() {
            Long campaignId = 8L;
            Campaign disabledCampaign = Campaign.builder()
                    .id(campaignId)
                    .status("DISABLED")
                    .build();

            CreateFundraisingGoalRequest request = CreateFundraisingGoalRequest.builder()
                    .campaignId(campaignId)
                    .targetAmount(BigDecimal.valueOf(5000))
                    .description("Test description for disabled campaign")
                    .isActive(true)
                    .build();

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(disabledCampaign));

            assertThatThrownBy(() -> fundraisingGoalService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat((ResponseStatusException) e)
                            .matches(ex -> ex.getStatusCode().value() == 403));
        }

        @Test
        @DisplayName("deactivates existing active goals when new goal is set active")
        void deactivatesExistingActiveGoals() {
            Long campaignId = 15L;
            Campaign campaign = Campaign.builder()
                    .id(campaignId)
                    .status("APPROVED")
                    .build();

            FundraisingGoal existingActiveGoal = buildGoal(2L, campaignId, BigDecimal.valueOf(3000));
            existingActiveGoal.setIsActive(true);

            CreateFundraisingGoalRequest request = CreateFundraisingGoalRequest.builder()
                    .campaignId(campaignId)
                    .targetAmount(BigDecimal.valueOf(7000))
                    .description("New active goal description here")
                    .isActive(true)
                    .build();

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
            when(fundraisingGoalRepository.findByCampaignIdAndIsActive(campaignId, true))
                    .thenReturn(List.of(existingActiveGoal));
            when(fundraisingGoalRepository.saveAll(anyList())).thenReturn(List.of(existingActiveGoal));
            when(fundraisingGoalRepository.save(any(FundraisingGoal.class))).thenAnswer(inv -> {
                FundraisingGoal g = inv.getArgument(0);
                g.setId(3L);
                return g;
            });

            FundraisingGoal result = fundraisingGoalService.create(request);

            assertThat(result.getIsActive()).isTrue();
            verify(fundraisingGoalRepository).saveAll(anyList());
            assertThat(existingActiveGoal.getIsActive()).isFalse();
        }
    }

    // ─── update ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("modifies goal fields")
        void modifiesGoalFields() {
            Long goalId = 3L;
            Long campaignId = 12L;

            FundraisingGoal existingGoal = buildGoal(goalId, campaignId, BigDecimal.valueOf(5000));
            existingGoal.setIsActive(false);

            Campaign campaign = Campaign.builder()
                    .id(campaignId)
                    .status("APPROVED")
                    .build();

            UpdateFundraisingGoalRequest request = UpdateFundraisingGoalRequest.builder()
                    .targetAmount(BigDecimal.valueOf(15000))
                    .description("Updated goal description")
                    .isActive(false)
                    .build();

            when(fundraisingGoalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
            when(fundraisingGoalRepository.save(any(FundraisingGoal.class))).thenAnswer(inv -> inv.getArgument(0));

            FundraisingGoal result = fundraisingGoalService.update(goalId, request);

            assertThat(result.getTargetAmount()).isEqualByComparingTo(BigDecimal.valueOf(15000));
            assertThat(result.getDescription()).isEqualTo("Updated goal description");
            verify(fundraisingGoalRepository).save(existingGoal);
        }

        @Test
        @DisplayName("throws ResponseStatusException 404 when goal not found during update")
        void throwsNotFoundWhenGoalNotFoundDuringUpdate() {
            when(fundraisingGoalRepository.findById(99L)).thenReturn(Optional.empty());

            UpdateFundraisingGoalRequest request = UpdateFundraisingGoalRequest.builder()
                    .targetAmount(BigDecimal.valueOf(5000))
                    .build();

            assertThatThrownBy(() -> fundraisingGoalService.update(99L, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat((ResponseStatusException) e)
                            .matches(ex -> ex.getStatusCode().value() == 404));
        }

        @Test
        @DisplayName("throws ResponseStatusException 403 when campaign is disabled during update")
        void throwsForbiddenWhenCampaignDisabledDuringUpdate() {
            Long goalId = 4L;
            Long campaignId = 22L;

            FundraisingGoal existingGoal = buildGoal(goalId, campaignId, BigDecimal.valueOf(5000));
            Campaign disabledCampaign = Campaign.builder()
                    .id(campaignId)
                    .status("DISABLED")
                    .build();

            UpdateFundraisingGoalRequest request = UpdateFundraisingGoalRequest.builder()
                    .targetAmount(BigDecimal.valueOf(8000))
                    .build();

            when(fundraisingGoalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(disabledCampaign));

            assertThatThrownBy(() -> fundraisingGoalService.update(goalId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat((ResponseStatusException) e)
                            .matches(ex -> ex.getStatusCode().value() == 403));
        }

        @Test
        @DisplayName("activates goal and deactivates others when isActive is set to true")
        void activatesGoalAndDeactivatesOthers() {
            Long goalId = 5L;
            Long campaignId = 30L;

            FundraisingGoal existingGoal = buildGoal(goalId, campaignId, BigDecimal.valueOf(4000));
            existingGoal.setIsActive(false);

            FundraisingGoal otherActiveGoal = buildGoal(6L, campaignId, BigDecimal.valueOf(3000));
            otherActiveGoal.setIsActive(true);

            Campaign campaign = Campaign.builder()
                    .id(campaignId)
                    .status("APPROVED")
                    .build();

            UpdateFundraisingGoalRequest request = UpdateFundraisingGoalRequest.builder()
                    .isActive(true)
                    .build();

            when(fundraisingGoalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
            when(fundraisingGoalRepository.findByCampaignIdAndIsActive(campaignId, true))
                    .thenReturn(List.of(otherActiveGoal));
            when(fundraisingGoalRepository.saveAll(anyList())).thenReturn(List.of(otherActiveGoal));
            when(fundraisingGoalRepository.save(any(FundraisingGoal.class))).thenAnswer(inv -> inv.getArgument(0));

            FundraisingGoal result = fundraisingGoalService.update(goalId, request);

            assertThat(result.getIsActive()).isTrue();
            assertThat(otherActiveGoal.getIsActive()).isFalse();
            verify(fundraisingGoalRepository).saveAll(anyList());
        }
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("calls repository delete when goal and campaign exist")
        void callsRepositoryDelete() {
            Long goalId = 7L;
            Long campaignId = 20L;

            FundraisingGoal existingGoal = buildGoal(goalId, campaignId, BigDecimal.valueOf(5000));
            Campaign campaign = Campaign.builder()
                    .id(campaignId)
                    .status("APPROVED")
                    .build();

            when(fundraisingGoalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

            fundraisingGoalService.delete(goalId);

            verify(fundraisingGoalRepository).delete(existingGoal);
        }

        @Test
        @DisplayName("throws ResponseStatusException 404 when goal not found during delete")
        void throwsNotFoundWhenGoalNotFoundDuringDelete() {
            when(fundraisingGoalRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fundraisingGoalService.delete(99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat((ResponseStatusException) e)
                            .matches(ex -> ex.getStatusCode().value() == 404));
        }

        @Test
        @DisplayName("throws ResponseStatusException 403 when campaign is disabled during delete")
        void throwsForbiddenWhenCampaignDisabledDuringDelete() {
            Long goalId = 8L;
            Long campaignId = 25L;

            FundraisingGoal existingGoal = buildGoal(goalId, campaignId, BigDecimal.valueOf(5000));
            Campaign disabledCampaign = Campaign.builder()
                    .id(campaignId)
                    .status("DISABLED")
                    .build();

            when(fundraisingGoalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(disabledCampaign));

            assertThatThrownBy(() -> fundraisingGoalService.delete(goalId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat((ResponseStatusException) e)
                            .matches(ex -> ex.getStatusCode().value() == 403));
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private FundraisingGoal buildGoal(Long id, Long campaignId, BigDecimal targetAmount) {
        return FundraisingGoal.builder()
                .id(id)
                .campaignId(campaignId)
                .targetAmount(targetAmount)
                .description("Test goal")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}