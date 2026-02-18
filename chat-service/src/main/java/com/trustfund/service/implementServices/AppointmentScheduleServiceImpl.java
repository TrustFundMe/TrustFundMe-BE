package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.model.AppointmentSchedule;
import com.trustfund.model.AppointmentStatus;
import com.trustfund.model.request.AppointmentScheduleRequest;
import com.trustfund.model.response.AppointmentScheduleResponse;
import com.trustfund.repository.AppointmentScheduleRepository;
import com.trustfund.service.interfaceServices.AppointmentScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentScheduleServiceImpl implements AppointmentScheduleService {

    private final AppointmentScheduleRepository repository;

    @Override
    @Transactional
    public AppointmentScheduleResponse createAppointment(AppointmentScheduleRequest request) {
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

        appointment.setStatus(status);
        return mapToResponse(repository.save(appointment));
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

    private AppointmentScheduleResponse mapToResponse(AppointmentSchedule appointment) {
        return AppointmentScheduleResponse.builder()
                .id(appointment.getId())
                .donorId(appointment.getDonorId())
                .staffId(appointment.getStaffId())
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
