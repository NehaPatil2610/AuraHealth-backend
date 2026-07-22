package com.aurahealth.monolith;

import com.aurahealth.monolith.entity.Appointment;
import com.aurahealth.monolith.entity.Patient;
import com.aurahealth.monolith.model.User;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointments; private final UserRepository users; private final PatientRepository patients; private final DoctorRepository doctors;
    public AppointmentController(AppointmentService appointments, UserRepository users, PatientRepository patients, DoctorRepository doctors) { this.appointments = appointments; this.users = users; this.patients = patients; this.doctors = doctors; }

    @PostMapping("/book")
    public ResponseEntity<?> book(@RequestParam Long doctorId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime appointmentTime, @RequestParam(required = false) String symptoms, Authentication authentication) {
        User user = current(authentication);
        if (user.getRole() != User.Role.PATIENT) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only patients can book appointments."));
        Patient patient = patients.findByEmail(user.getEmail()).orElseThrow();
        Appointment appointment = appointments.createAppointment(patient.getId(), doctorId, appointmentTime, symptoms);
        return ResponseEntity.status(HttpStatus.CREATED).body(view(appointment, user));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Map<String, Object>>> mine(Authentication authentication) {
        User user = current(authentication);
        List<Appointment> result = user.getRole() == User.Role.DOCTOR
                ? doctors.findAll().stream().filter(d -> d.getUserId().equals(user.getId())).findFirst().map(d -> appointments.getAppointmentsByDoctor(d.getId())).orElse(List.of())
                : patients.findByEmail(user.getEmail()).map(p -> appointments.getAppointmentsByPatient(p.getId())).orElse(List.of());
        return ResponseEntity.ok(result.stream().map(item -> view(item, user)).toList());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(appointments.updateStatus(id, status));
    }

    @PutMapping("/{id}/time")
    public ResponseEntity<?> updateTime(@PathVariable Long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newTime) {
        return ResponseEntity.ok(appointments.updateTime(id, newTime));
    }

    @PutMapping("/today/complete")
    public ResponseEntity<?> markDayComplete(Authentication authentication) {
        User user = current(authentication);
        if (user.getRole() != User.Role.DOCTOR) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only doctors can perform this action."));
        com.aurahealth.monolith.entity.Doctor doctor = doctors.findAll().stream().filter(d -> d.getUserId().equals(user.getId())).findFirst().orElseThrow();
        appointments.markDayComplete(doctor.getId());
        return ResponseEntity.ok(Map.of("message", "Day marked as complete."));
    }

    private User current(Authentication authentication) { return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow(); }

    private Map<String, Object> view(Appointment appointment, User viewer) {
        boolean doctorViewer = viewer.getRole() == User.Role.DOCTOR;
        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        map.put("id", appointment.getId());
        map.put("appointmentTime", appointment.getAppointmentTime());
        map.put("status", appointment.getStatus());
        map.put("priority", appointment.getPatient().getSubscriptionTier());
        map.put("symptoms", appointment.getSymptoms());
        if (doctorViewer) {
            map.put("patientId", appointment.getPatient().getId());
            map.put("patientName", appointment.getPatient().getName());
        } else {
            map.put("doctorId", appointment.getDoctor().getId());
            map.put("doctorName", appointment.getDoctor().getName());
            map.put("doctorSpecialty", appointment.getDoctor().getSpecialization());
        }
        return map;
    }
}
