package com.aurahealth.monolith;

import com.aurahealth.monolith.entity.Doctor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    // Matches gateway rule: /api/doctors/add (ADMIN only at gateway level)
    @PostMapping("/add")
    public ResponseEntity<Doctor> addDoctor(@RequestBody Doctor doctor) {
        Doctor savedDoctor = doctorService.addDoctor(doctor);
        return new ResponseEntity<>(savedDoctor, HttpStatus.CREATED);
    }

    // Matches gateway rule: /api/doctors (Accessible by ADMIN, DOCTOR, PATIENT)
    @GetMapping
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<List<Doctor>> getDoctorsBySpecialization(@PathVariable String specialization) {
        return ResponseEntity.ok(doctorService.getDoctorsBySpecialization(specialization));
    }

    // Toggles clinical availability status
    @PutMapping("/{id}/availability")
    public ResponseEntity<Doctor> updateAvailability(
            @PathVariable Long id,
            @RequestParam boolean isAvailable) {
        return ResponseEntity.ok(doctorService.updateAvailability(id, isAvailable));
    }
}