package com.trustfund.service.interfaceServices;

import com.trustfund.model.AppointmentStatus;
import com.trustfund.model.request.AppointmentScheduleRequest;
import com.trustfund.model.response.AppointmentScheduleResponse;

import java.util.List;

public interface AppointmentScheduleService {
    AppointmentScheduleResponse createAppointment(AppointmentScheduleRequest request);

    List<AppointmentScheduleResponse> getAllAppointments();

    AppointmentScheduleResponse getAppointmentById(Long id);

    AppointmentScheduleResponse updateAppointment(Long id, AppointmentScheduleRequest request);

    AppointmentScheduleResponse updateStatus(Long id, AppointmentStatus status);

    List<AppointmentScheduleResponse> getAppointmentsByDonor(Long donorId);

    List<AppointmentScheduleResponse> getAppointmentsByStaff(Long staffId);
}
