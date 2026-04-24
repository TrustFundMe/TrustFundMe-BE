package com.trustfund.service;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.mapper.ModuleGroupMapper;
import com.trustfund.model.ModuleGroup;
import com.trustfund.model.dto.request.CreateModuleGroupRequest;
import com.trustfund.model.dto.request.UpdateModuleGroupRequest;
import com.trustfund.model.dto.response.ModuleGroupDetailResponse;
import com.trustfund.model.dto.response.ModuleGroupResponse;
import com.trustfund.repository.ModuleGroupRepository;
import com.trustfund.service.implementServices.ModuleGroupServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleGroupServiceImplTest {

    @Mock private ModuleGroupRepository repo;
    @Mock private ModuleGroupMapper mapper;

    @InjectMocks private ModuleGroupServiceImpl service;

    private ModuleGroup group;

    @BeforeEach
    void setUp() {
        group = new ModuleGroup();
        group.setId(1L); group.setName("G1"); group.setDisplayOrder(1); group.setIsActive(true);
    }

    @Test @DisplayName("getAll_returnsMappedList")
    void getAll() {
        when(repo.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(group));
        when(mapper.toResponse(any(ModuleGroup.class), anyBoolean())).thenReturn(new ModuleGroupDetailResponse());
        assertThat(service.getAll()).hasSize(1);
    }

    @Test @DisplayName("getAllDetails_returnsMappedListWithModules")
    void getAllDetails() {
        when(repo.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(group));
        when(mapper.toResponse(any(ModuleGroup.class), anyBoolean())).thenReturn(new ModuleGroupDetailResponse());
        assertThat(service.getAllDetails()).hasSize(1);
    }

    @Test @DisplayName("getDetailById_notFound_throws")
    void getDetail_notFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDetailById(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test @DisplayName("getDetailById_found_returnsResponse")
    void getDetail_ok() {
        when(repo.findById(1L)).thenReturn(Optional.of(group));
        when(mapper.toResponse(group, true)).thenReturn(new ModuleGroupDetailResponse());
        assertThat(service.getDetailById(1L)).isNotNull();
    }

    @Test @DisplayName("create_duplicateName_throws")
    void create_dup() {
        CreateModuleGroupRequest r = new CreateModuleGroupRequest();
        r.setName("G1");
        when(repo.existsByName(any())).thenReturn(true);
        assertThatThrownBy(() -> service.create(r)).isInstanceOf(BadRequestException.class);
    }

    @Test @DisplayName("create_success")
    void create_ok() {
        CreateModuleGroupRequest r = new CreateModuleGroupRequest();
        r.setName("NewGroup"); r.setDescription("d"); r.setDisplayOrder(1); r.setIsActive(true);
        when(repo.existsByName(any())).thenReturn(false);
        when(repo.count()).thenReturn(0L);
        when(repo.save(any())).thenAnswer(i -> { ModuleGroup g = i.getArgument(0); g.setId(2L); return g; });
        when(mapper.toResponse(any(ModuleGroup.class))).thenReturn(new ModuleGroupResponse());
        assertThat(service.create(r)).isNotNull();
        verify(repo).shiftOrdersForInsert(1);
    }

    @Test @DisplayName("update_notFound_throws")
    void update_notFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        UpdateModuleGroupRequest r = new UpdateModuleGroupRequest();
        r.setName("X");
        assertThatThrownBy(() -> service.update(1L, r)).isInstanceOf(NotFoundException.class);
    }

    @Test @DisplayName("update_duplicateName_throws")
    void update_dupName() {
        when(repo.findById(1L)).thenReturn(Optional.of(group));
        when(repo.existsByName(any())).thenReturn(true);
        UpdateModuleGroupRequest r = new UpdateModuleGroupRequest();
        r.setName("OtherName");
        assertThatThrownBy(() -> service.update(1L, r)).isInstanceOf(BadRequestException.class);
    }

    @Test @DisplayName("update_orderExceedsCount_throws")
    void update_orderExceeds() {
        when(repo.findById(1L)).thenReturn(Optional.of(group));
        when(repo.count()).thenReturn(2L);
        UpdateModuleGroupRequest r = new UpdateModuleGroupRequest();
        r.setName("G1"); r.setDisplayOrder(5);
        assertThatThrownBy(() -> service.update(1L, r)).isInstanceOf(BadRequestException.class);
    }

    @Test @DisplayName("delete_notFound_throws")
    void delete_notFound() {
        when(repo.existsById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test @DisplayName("delete_ok")
    void delete_ok() {
        when(repo.existsById(1L)).thenReturn(true);
        service.delete(1L);
        verify(repo).deleteById(1L);
    }

    @Test @DisplayName("getActiveGroupsWithActiveModules_filtersEmpty")
    void activeGroups() {
        ModuleGroupDetailResponse empty = new ModuleGroupDetailResponse();
        empty.setTotalModules(0);
        ModuleGroupDetailResponse nonEmpty = new ModuleGroupDetailResponse();
        nonEmpty.setTotalModules(3);
        when(repo.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(group, group));
        when(mapper.toActiveResponse(any())).thenReturn(empty, nonEmpty);
        assertThat(service.getActiveGroupsWithActiveModules()).hasSize(1);
    }
}
