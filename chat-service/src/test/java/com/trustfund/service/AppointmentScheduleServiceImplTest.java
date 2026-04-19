package com.trustfund.service;

import com.trustfund.client.NotificationServiceClient;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.model.AppointmentSchedule;
import com.trustfund.model.AppointmentStatus;
import com.trustfund.model.request.AppointmentScheduleRequest;
import com.trustfund.model.response.AppointmentScheduleResponse;
import com.trustfund.repository.AppointmentScheduleRepository;
import com.trustfund.service.implementServices.AppointmentScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentScheduleServiceImplTest {

    @Mock
    private AppointmentScheduleRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @InjectMocks
    private AppointmentScheduleServiceImpl appointmentService;

    @Captor
    private ArgumentCaptor<AppointmentSchedule> appointmentCaptor;

    private static final String IDENTITY_SERVICE_URL = "http://localhost:8081";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(appointmentService, "identityServiceUrl", IDENTITY_SERVICE_URL);
    }

    private AppointmentSchedule buildAppointment(Long id, Long donorId, Long staffId,
            AppointmentStatus status, LocalDateTime startTime, LocalDateTime endTime) {
        AppointmentSchedule appt = AppointmentSchedule.builder()
                .id(id)
                .donorId(donorId)
                .staffId(staffId)
                .startTime(startTime)
                .endTime(endTime)
                .location("123 Main St")
                .purpose("Fund discussion")
                .status(status)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
        return appt;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // createAppointment tests
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createAppointment")
    class CreateAppointmentTests {

        @Test
        @DisplayName("createAppointment_validRequest_savesAndReturnsAppointmentScheduleResponse")
        void createAppointment_validRequest_savesAndReturnsAppointmentScheduleResponse() {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(1).plusHours(1);

            AppointmentScheduleRequest request = AppointmentScheduleRequest.builder()
                    .donorId(10L)
                    .staffId(20L)
                    .startTime(startTime)
                    .endTime(endTime)
                    .location("123 Main St")
                    .purpose("Fund discussion")
                    .status(AppointmentStatus.PENDING)
                    .build();

            AppointmentSchedule savedAppointment = buildAppointment(
                    1L, 10L, 20L, AppointmentStatus.PENDING, startTime, endTime);

            when(repository.save(any(AppointmentSchedule.class))).thenReturn(savedAppointment);
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            // Act
            AppointmentScheduleResponse response = appointmentService.createAppointment(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getDonorId()).isEqualTo(10L);
            assertThat(response.getStaffId()).isEqualTo(20L);
            assertThat(response.getStatus()).isEqualTo(AppointmentStatus.PENDING);
            assertThat(response.getLocation()).isEqualTo("123 Main St");
            assertThat(response.getPurpose()).isEqualTo("Fund discussion");

            verify(repository).save(appointmentCaptor.capture());
            AppointmentSchedule saved = appointmentCaptor.getValue();
            assertThat(saved.getDonorId()).isEqualTo(10L);
            assertThat(saved.getStaffId()).isEqualTo(20L);
            assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        }

        @Test
        @DisplayName("createAppointment_nullStatus_defaultsToPending")
        void createAppointment_nullStatus_defaultsToPending() {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(1).plusHours(1);

            AppointmentScheduleRequest request = AppointmentScheduleRequest.builder()
                    .donorId(10L)
                    .staffId(20L)
                    .startTime(startTime)
                    .endTime(endTime)
                    .location("Room A")
                    .purpose("Meeting")
                    .status(null)
                    .build();

            AppointmentSchedule savedAppointment = buildAppointment(
                    2L, 10L, 20L, AppointmentStatus.PENDING, startTime, endTime);

            when(repository.save(any(AppointmentSchedule.class))).thenReturn(savedAppointment);
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            // Act
            AppointmentScheduleResponse response = appointmentService.createAppointment(request);

            // Assert
            assertThat(response.getStatus()).isEqualTo(AppointmentStatus.PENDING);
            verify(repository).save(appointmentCaptor.capture());
            assertThat(appointmentCaptor.getValue().getStatus()).isEqualTo(AppointmentStatus.PENDING);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // getAppointmentById tests
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAppointmentById")
    class GetAppointmentByIdTests {

        @Test
        @DisplayName("getAppointmentById_found_returnsAppointmentScheduleResponse")
        void getAppointmentById_found_returnsAppointmentScheduleResponse() {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusDays(2);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2).plusHours(1);
            AppointmentSchedule appointment = buildAppointment(
                    5L, 10L, 20L, AppointmentStatus.CONFIRMED, startTime, endTime);

            when(repository.findById(5L)).thenReturn(Optional.of(appointment));
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            // Act
            AppointmentScheduleResponse response = appointmentService.getAppointmentById(5L);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(5L);
            assertThat(response.getDonorId()).isEqualTo(10L);
            assertThat(response.getStaffId()).isEqualTo(20L);
            assertThat(response.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
            verify(repository).findById(5L);
        }

        @Test
        @DisplayName("getAppointmentById_notFound_throwsNotFoundException")
        void getAppointmentById_notFound_throwsNotFoundException() {
            // Arrange
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> appointmentService.getAppointmentById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Appointment not found with id: 999");

            verify(repository).findById(999L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // updateStatus tests
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("updateStatus_validStatusChange_updatesAndReturnsAppointmentScheduleResponse")
        void updateStatus_validStatusChange_updatesAndReturnsAppointmentScheduleResponse() {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusDays(2);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2).plusHours(1);
            AppointmentSchedule appointment = buildAppointment(
                    1L, 10L, 20L, AppointmentStatus.PENDING, startTime, endTime);
            AppointmentSchedule updatedAppointment = buildAppointment(
                    1L, 10L, 20L, AppointmentStatus.CONFIRMED, startTime, endTime);

            when(repository.findById(1L)).thenReturn(Optional.of(appointment));
            when(repository.save(any(AppointmentSchedule.class))).thenReturn(updatedAppointment);
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            // Act
            AppointmentScheduleResponse response = appointmentService.updateStatus(1L, AppointmentStatus.CONFIRMED);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

            verify(repository).save(appointmentCaptor.capture());
            assertThat(appointmentCaptor.getValue().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

            // Notification should be sent for CONFIRMED
            verify(notificationServiceClient).sendNotification(any());
        }

        @Test
        @DisplayName("updateStatus_toCancelled_sendsNotificationAndReturnsResponse")
        void updateStatus_toCancelled_sendsNotificationAndReturnsResponse() {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusDays(3);
            LocalDateTime endTime = LocalDateTime.now().plusDays(3).plusHours(1);
            AppointmentSchedule appointment = buildAppointment(
                    2L, 10L, 20L, AppointmentStatus.PENDING, startTime, endTime);
            AppointmentSchedule updatedAppointment = buildAppointment(
                    2L, 10L, 20L, AppointmentStatus.CANCELLED, startTime, endTime);

            when(repository.findById(2L)).thenReturn(Optional.of(appointment));
            when(repository.save(any(AppointmentSchedule.class))).thenReturn(updatedAppointment);
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            // Act
            AppointmentScheduleResponse response = appointmentService.updateStatus(2L, AppointmentStatus.CANCELLED);

            // Assert
            assertThat(response.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            verify(notificationServiceClient).sendNotification(any());
        }

        @Test
        @DisplayName("updateStatus_completedBeforeEndTime_throwsResponseStatusException400")
        void updateStatus_completedBeforeEndTime_throwsResponseStatusException400() {
            // Arrange
            // endTime is in the future, so attempting COMPLETED should fail
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(1).plusHours(2); // ends 2 hours from now
            AppointmentSchedule appointment = buildAppointment(
                    3L, 10L, 20L, AppointmentStatus.CONFIRMED, startTime, endTime);

            when(repository.findById(3L)).thenReturn(Optional.of(appointment));

            // Act & Assert
            assertThatThrownBy(() -> appointmentService.updateStatus(3L, AppointmentStatus.COMPLETED))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .hasMessageContaining("Không thể hoàn thành lịch hẹn trước khi kết thúc");

            verify(repository, never()).save(any(AppointmentSchedule.class));
            verify(notificationServiceClient, never()).sendNotification(any());
        }

        @Test
        @DisplayName("updateStatus_completedAfterEndTime_succeeds")
        void updateStatus_completedAfterEndTime_succeeds() {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().minusDays(1);
            LocalDateTime endTime = LocalDateTime.now().minusMinutes(30); // ended 30 min ago
            AppointmentSchedule appointment = buildAppointment(
                    4L, 10L, 20L, AppointmentStatus.CONFIRMED, startTime, endTime);
            AppointmentSchedule completedAppointment = buildAppointment(
                    4L, 10L, 20L, AppointmentStatus.COMPLETED, startTime, endTime);

            when(repository.findById(4L)).thenReturn(Optional.of(appointment));
            when(repository.save(any(AppointmentSchedule.class))).thenReturn(completedAppointment);
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            // Act
            AppointmentScheduleResponse response = appointmentService.updateStatus(4L, AppointmentStatus.COMPLETED);

            // Assert
            assertThat(response.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
            verify(repository).save(appointmentCaptor.capture());
            assertThat(appointmentCaptor.getValue().getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        }

        @Test
        @DisplayName("updateStatus_notFound_throwsNotFoundException")
        void updateStatus_notFound_throwsNotFoundException() {
            // Arrange
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> appointmentService.updateStatus(999L, AppointmentStatus.CONFIRMED))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Appointment not found with id: 999");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // getAppointmentsByDonor tests
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAppointmentsByDonor")
    class GetAppointmentsByDonorTests {

        @Test
        @DisplayName("getAppointmentsByDonor_returnsFilteredList")
        void getAppointmentsByDonor_returnsFilteredList() {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(1).plusHours(1);

            AppointmentSchedule appt1 = buildAppointment(
                    1L, 10L, 20L, AppointmentStatus.PENDING, startTime, endTime);
            AppointmentSchedule appt2 = buildAppointment(
                    2L, 10L, 30L, AppointmentStatus.CONFIRMED, startTime.plusDays(2), endTime.plusDays(2));

            when(repository.findByDonorId(10L)).thenReturn(List.of(appt1, appt2));
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            // Act
            List<AppointmentScheduleResponse> responses = appointmentService.getAppointmentsByDonor(10L);

            // Assert
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(AppointmentScheduleResponse::getDonorId)
                    .containsOnly(10L);

            verify(repository).findByDonorId(10L);
        }

        @Test
        @DisplayName("getAppointmentsByDonor_noAppointments_returnsEmptyList")
        void getAppointmentsByDonor_noAppointments_returnsEmptyList() {
            // Arrange
            when(repository.findByDonorId(999L)).thenReturn(List.of());

            // Act
            List<AppointmentScheduleResponse> responses = appointmentService.getAppointmentsByDonor(999L);

            // Assert
            assertThat(responses).isEmpty();
            verify(repository).findByDonorId(999L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // getAppointmentsByStaff tests
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAppointmentsByStaff")
    class GetAppointmentsByStaffTests {

        @Test
        @DisplayName("getAppointmentsByStaff_returnsFilteredList")
        void getAppointmentsByStaff_returnsFilteredList() {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(1).plusHours(1);

            AppointmentSchedule appt1 = buildAppointment(
                    1L, 10L, 20L, AppointmentStatus.CONFIRMED, startTime, endTime);
            AppointmentSchedule appt2 = buildAppointment(
                    2L, 15L, 20L, AppointmentStatus.PENDING, startTime.plusDays(3), endTime.plusDays(3));

            when(repository.findByStaffId(20L)).thenReturn(List.of(appt1, appt2));
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            // Act
            List<AppointmentScheduleResponse> responses = appointmentService.getAppointmentsByStaff(20L);

            // Assert
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(AppointmentScheduleResponse::getStaffId)
                    .containsOnly(20L);

            verify(repository).findByStaffId(20L);
        }

        @Test
        @DisplayName("getAppointmentsByStaff_noAppointments_returnsEmptyList")
        void getAppointmentsByStaff_noAppointments_returnsEmptyList() {
            // Arrange
            when(repository.findByStaffId(999L)).thenReturn(List.of());

            // Act
            List<AppointmentScheduleResponse> responses = appointmentService.getAppointmentsByStaff(999L);

            // Assert
            assertThat(responses).isEmpty();
            verify(repository).findByStaffId(999L);
        }
    }
}
