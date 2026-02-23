package com.trustfund.model.response;

import com.trustfund.model.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentScheduleResponse {
    private Long id;
    private Long donorId;
    private String donorName;
    private Long staffId;
    private String staffName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentStatus status;
    private String location;
    private String purpose;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
