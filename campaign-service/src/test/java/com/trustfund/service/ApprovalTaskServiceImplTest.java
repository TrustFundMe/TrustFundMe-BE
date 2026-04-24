package com.trustfund.service;

import com.trustfund.client.IdentityServiceClient;
import com.trustfund.model.ApprovalTask;
import com.trustfund.repository.ApprovalTaskRepository;
import com.trustfund.service.impl.ApprovalTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalTaskServiceImplTest {

    @Mock private ApprovalTaskRepository repo;
    @Mock private IdentityServiceClient identityClient;

    @InjectMocks private ApprovalTaskServiceImpl service;

    @Test @DisplayName("createAndAssignTask_assignsToRandomStaff")
    void create_ok() {
        when(identityClient.getStaffIds()).thenReturn(List.of(10L, 20L));
        when(repo.save(any())).thenAnswer(i -> { ApprovalTask t = i.getArgument(0); t.setId(1L); return t; });
        ApprovalTask t = service.createAndAssignTask("CAMPAIGN", 100L);
        assertThat(t.getStatus()).isEqualTo("PENDING");
        assertThat(t.getStaffId()).isIn(10L, 20L);
    }

    @Test @DisplayName("createAndAssignTask_emptyStaff_assignsNull")
    void create_noStaff() {
        when(identityClient.getStaffIds()).thenReturn(List.of());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        ApprovalTask t = service.createAndAssignTask("CAMPAIGN", 100L);
        assertThat(t.getStaffId()).isNull();
    }

    @Test @DisplayName("createAndAssignTask_nullStaffList_assignsNull")
    void create_nullList() {
        when(identityClient.getStaffIds()).thenReturn(null);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        ApprovalTask t = service.createAndAssignTask("FLAG", 50L);
        assertThat(t.getStaffId()).isNull();
    }

    @Test @DisplayName("reassignTask_notFound_throws")
    void reassign_notFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reassignTask(1L, 99L)).isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("reassignTask_updatesStaffId")
    void reassign_ok() {
        ApprovalTask t = ApprovalTask.builder().id(1L).staffId(10L).status("PENDING").build();
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        ApprovalTask r = service.reassignTask(1L, 99L);
        assertThat(r.getStaffId()).isEqualTo(99L);
        assertThat(r.getStatus()).isEqualTo("REASSIGNED");
    }

    @Test @DisplayName("getAllTasks_returnsAll")
    void getAll() {
        when(repo.findAll()).thenReturn(List.of(ApprovalTask.builder().id(1L).build()));
        assertThat(service.getAllTasks()).hasSize(1);
    }

    @Test @DisplayName("getTasksByStaff_filtersByStaffId")
    void getByStaff() {
        when(repo.findByStaffId(10L)).thenReturn(List.of(ApprovalTask.builder().id(1L).build()));
        assertThat(service.getTasksByStaff(10L)).hasSize(1);
    }

    @Test @DisplayName("completeTask_existingTask_setsCompleted")
    void complete_ok() {
        ApprovalTask t = ApprovalTask.builder().id(1L).status("PENDING").build();
        when(repo.findByTypeAndTargetId("CAMPAIGN", 1L)).thenReturn(Optional.of(t));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        ApprovalTask r = service.completeTask("CAMPAIGN", 1L);
        assertThat(r.getStatus()).isEqualTo("COMPLETED");
    }

    @Test @DisplayName("completeTask_notFound_returnsNull")
    void complete_notFound() {
        when(repo.findByTypeAndTargetId("CAMPAIGN", 99L)).thenReturn(Optional.empty());
        assertThat(service.completeTask("CAMPAIGN", 99L)).isNull();
    }

    @Test @DisplayName("getTaskByCampaignId_returnsTaskOrNull")
    void getByCampaignId() {
        when(repo.findByTypeAndTargetId("CAMPAIGN", 1L))
                .thenReturn(Optional.of(ApprovalTask.builder().id(5L).build()));
        assertThat(service.getTaskByCampaignId(1L).getId()).isEqualTo(5L);
    }

    @Test @DisplayName("getTaskByTypeAndTargetId_notFound_returnsNull")
    void getByType_notFound() {
        when(repo.findByTypeAndTargetId("X", 1L)).thenReturn(Optional.empty());
        assertThat(service.getTaskByTypeAndTargetId("X", 1L)).isNull();
    }
}
