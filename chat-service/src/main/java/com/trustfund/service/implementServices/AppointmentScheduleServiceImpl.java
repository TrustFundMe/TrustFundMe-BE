package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.model.AppointmentSchedule;
import com.trustfund.model.AppointmentStatus;
import com.trustfund.model.request.AppointmentScheduleRequest;
import com.trustfund.model.response.AppointmentScheduleResponse;
import com.trustfund.repository.AppointmentScheduleRepository;
import com.trustfund.service.interfaceServices.AppointmentScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.trustfund.client.NotificationServiceClient;
import com.trustfund.model.request.NotificationRequest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentScheduleServiceImpl implements AppointmentScheduleService {

    private final AppointmentScheduleRepository repository;
    private final RestTemplate restTemplate;
    private final NotificationServiceClient notificationServiceClient;

    @Value("${identity.service.url:http://localhost:8081}")
    private String identityServiceUrl;

    /** Phải đặt lịch trước tối thiểu 24 tiếng */
    private static final long MIN_ADVANCE_HOURS = 24;

    /** Phải xác nhận trước tối thiểu 24 tiếng so với giờ hẹn */
    private static final long MAX_CONFIRM_HOURS_BEFORE = 24;

    @Override
    @Transactional
    public AppointmentScheduleResponse createAppointment(AppointmentScheduleRequest request) {
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilStart = ChronoUnit.HOURS.between(now, request.getStartTime());
        if (hoursUntilStart < MIN_ADVANCE_HOURS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lịch hẹn phải được đặt trước tối thiểu " + MIN_ADVANCE_HOURS + " tiếng. " +
                            "Hiện tại còn " + hoursUntilStart + " tiếng đến thời điểm bắt đầu.");
        }

        AppointmentSchedule appointment = AppointmentSchedule.builder()
                .donorId(request.getDonorId())
                .staffId(request.getStaffId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .purpose(request.getPurpose())
                .status(request.getStatus() != null ? request.getStatus() : AppointmentStatus.PENDING)
                .build();

        return mapToResponse(repository.save(appointment));
    }

    @Override
    public List<AppointmentScheduleResponse> getAllAppointments() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentScheduleResponse getAppointmentById(Long id) {
        AppointmentSchedule appointment = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));
        return mapToResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentScheduleResponse updateAppointment(Long id, AppointmentScheduleRequest request) {
        AppointmentSchedule appointment = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getEndTime());
        appointment.setLocation(request.getLocation());
        appointment.setPurpose(request.getPurpose());
        if (request.getStatus() != null) {
            appointment.setStatus(request.getStatus());
        }

        return mapToResponse(repository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentScheduleResponse updateStatus(Long id, AppointmentStatus status) {
        AppointmentSchedule appointment = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

        if (status == AppointmentStatus.CONFIRMED) {
            LocalDateTime now = LocalDateTime.now();
            long hoursUntilStart = ChronoUnit.HOURS.between(now, appointment.getStartTime());
            if (hoursUntilStart < MAX_CONFIRM_HOURS_BEFORE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không thể xác nhận lịch hẹn. Phải xác nhận trước tối thiểu " + MAX_CONFIRM_HOURS_BEFORE
                                + " tiếng. " +
                                "Hiện tại chỉ còn " + hoursUntilStart + " tiếng đến buổi gặp.");
            }
        }

        if (status == AppointmentStatus.COMPLETED) {
            if (LocalDateTime.now().isBefore(appointment.getEndTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không thể hoàn thành lịch hẹn trước khi kết thúc. " +
                                "Giờ kết thúc: " + appointment.getEndTime() + ".");
            }
        }

        appointment.setStatus(status);
        AppointmentSchedule saved = repository.save(appointment);

        // Send notification for CONFIRMED or CANCELLED status
        if (status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.CANCELLED) {
            sendAppointmentNotification(saved, status);
        }

        return mapToResponse(saved);
    }

    @Override
    public List<AppointmentScheduleResponse> getAppointmentsByDonor(Long donorId) {
        return repository.findByDonorId(donorId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentScheduleResponse> getAppointmentsByStaff(Long staffId) {
        return repository.findByStaffId(staffId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String fetchUserName(Long userId) {
        if (userId == null)
            return null;
        try {
            String url = identityServiceUrl + "/api/internal/users/" + userId + "/name";
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.warn("Could not fetch name for user {}: {}", userId, e.getMessage());
            return "User #" + userId;
        }
    }

    private void sendAppointmentNotification(AppointmentSchedule appointment, AppointmentStatus status) {
        try {
            boolean isConfirmed = status == AppointmentStatus.CONFIRMED;
            String title = isConfirmed ? "Lịch hẹn đã được xác nhận" : "Lịch hẹn đã bị hủy";

            String startTimeStr = appointment.getStartTime().toString().replace("T", " ");
            String content = isConfirmed
                    ? "Lịch hẹn của bạn vào lúc " + startTimeStr + " tại " + appointment.getLocation()
                            + " đã được xác nhận."
                    : "Rất tiếc, lịch hẹn của bạn vào lúc " + startTimeStr + " đã bị hủy.";

            java.util.Map<String, Object> notificationData = new java.util.HashMap<>();
            notificationData.put("appointmentId", appointment.getId());
            notificationData.put("status", status.name());

            NotificationRequest notificationRequest = NotificationRequest.builder()
                    .userId(appointment.getDonorId())
                    .type(isConfirmed ? "APPOINTMENT_CONFIRMED" : "APPOINTMENT_CANCELLED")
                    .targetId(appointment.getId())
                    .targetType("APPOINTMENT")
                    .title(title)
                    .content(content)
                    .data(notificationData)
                    .build();

            log.info("[AppointmentService] Sending notification to user {} for appointment {}",
                    appointment.getDonorId(), appointment.getId());
            notificationServiceClient.sendNotification(notificationRequest);
        } catch (Exception e) {
            log.error("Error sending appointment notification for user {}: {}", appointment.getDonorId(),
                    e.getMessage());
        }
    }

    private AppointmentScheduleResponse mapToResponse(AppointmentSchedule appointment) {
        return AppointmentScheduleResponse.builder()
                .id(appointment.getId())
                .donorId(appointment.getDonorId())
                .donorName(fetchUserName(appointment.getDonorId()))
                .staffId(appointment.getStaffId())
                .staffName(fetchUserName(appointment.getStaffId()))
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .location(appointment.getLocation())
                .purpose(appointment.getPurpose())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}
