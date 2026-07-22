package com.aurahealth.monolith;

import com.aurahealth.monolith.entity.MedicalRecord;
import com.aurahealth.monolith.entity.Patient;
import com.aurahealth.monolith.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
public class MedicalRecordController {

    private final MedicalRecordRepository records;
    private final UserRepository users;
    private final PatientRepository patients;

    public MedicalRecordController(MedicalRecordRepository records,
                                   UserRepository users,
                                   PatientRepository patients) {
        this.records = records;
        this.users = users;
        this.patients = patients;
    }

    /**
     * GET /api/records/mine — returns the authenticated patient's medical records.
     */
    @GetMapping("/mine")
    public ResponseEntity<List<Map<String, Object>>> mine(Authentication authentication) {
        Patient patient = resolvePatient(authentication);
        List<MedicalRecord> result = records.findByPatientIdOrderByRecordDateDesc(patient.getId());
        return ResponseEntity.ok(result.stream().map(this::toView).toList());
    }

    /**
     * GET /api/records/{id} — returns a single record, only if it belongs to the
     * authenticated patient. Returns 404 if the record doesn't exist or belongs
     * to another patient (no information leakage).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id,
                                                       Authentication authentication) {
        Patient patient = resolvePatient(authentication);
        return records.findByIdAndPatientId(id, patient.getId())
                .map(r -> ResponseEntity.ok(toView(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/records/patient/{patientId} — returns a patient's medical records for doctors.
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getByPatientId(@PathVariable Long patientId, Authentication authentication) {
        User user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        if (user.getRole() != User.Role.DOCTOR) return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("message", "Only doctors can perform this action."));
        List<MedicalRecord> result = records.findByPatientIdOrderByRecordDateDesc(patientId);
        return ResponseEntity.ok(result.stream().map(this::toView).toList());
    }

    // ── Helpers ────────────────────────────────────────────────────

    private Patient resolvePatient(Authentication authentication) {
        User user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return patients.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("No patient profile found for user"));
    }

    private Map<String, Object> toView(MedicalRecord r) {
        return Map.of(
                "id", r.getId(),
                "title", r.getTitle(),
                "recordType", r.getRecordType(),
                "content", r.getContent(),
                "recordDate", r.getRecordDate().toString(),
                "createdAt", r.getCreatedAt().toString()
        );
    }
}
