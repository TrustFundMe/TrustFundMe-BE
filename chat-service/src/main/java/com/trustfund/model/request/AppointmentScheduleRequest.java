package com.trustfund.model.request;

import com.trustfund.model.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentScheduleRequest {

    @NotNull(message = "Donor ID is required")
    private Long donorId;

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private String location;

    private String purpose;

    private AppointmentStatus status;
}
