package com.trustfund.repository;

import com.trustfund.model.AppointmentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentScheduleRepository extends JpaRepository<AppointmentSchedule, Long> {
    List<AppointmentSchedule> findByDonorId(Long donorId);

    List<AppointmentSchedule> findByStaffId(Long staffId);
}
