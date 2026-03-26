package com.trustfund.controller;

import com.trustfund.model.AppointmentStatus;
import com.trustfund.model.request.AppointmentScheduleRequest;
import com.trustfund.model.response.AppointmentScheduleResponse;
import com.trustfund.service.interfaceServices.AppointmentScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment Schedule", description = "Endpoints for managing appointments between Staff and Donors")
public class AppointmentScheduleController {

    private final AppointmentScheduleService service;

    @PostMapping
    @Operation(summary = "Create a new appointment")
    public ResponseEntity<AppointmentScheduleResponse> createAppointment(
            @Valid @RequestBody AppointmentScheduleRequest request) {
        return new ResponseEntity<>(service.createAppointment(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all appointments")
    public ResponseEntity<List<AppointmentScheduleResponse>> getAllAppointments() {
        return ResponseEntity.ok(service.getAllAppointments());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<AppointmentScheduleResponse> getAppointmentById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getAppointmentById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update appointment details")
    public ResponseEntity<AppointmentScheduleResponse> updateAppointment(@PathVariable("id") Long id,
            @Valid @RequestBody AppointmentScheduleRequest request) {
        return ResponseEntity.ok(service.updateAppointment(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update appointment status")
    public ResponseEntity<AppointmentScheduleResponse> updateStatus(@PathVariable("id") Long id,
            @RequestParam("status") AppointmentStatus status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    @GetMapping("/donor/{donorId}")
    @Operation(summary = "Get appointments by donor ID")
    public ResponseEntity<List<AppointmentScheduleResponse>> getAppointmentsByDonor(
            @PathVariable("donorId") Long donorId) {
        return ResponseEntity.ok(service.getAppointmentsByDonor(donorId));
    }

    @GetMapping("/staff/{staffId}")
    @Operation(summary = "Get appointments by staff ID")
    public ResponseEntity<List<AppointmentScheduleResponse>> getAppointmentsByStaff(
            @PathVariable("staffId") Long staffId) {
        return ResponseEntity.ok(service.getAppointmentsByStaff(staffId));
    }
}
