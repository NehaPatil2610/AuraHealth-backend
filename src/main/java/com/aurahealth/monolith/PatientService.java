package com.aurahealth.monolith;

import com.aurahealth.monolith.entity.Patient;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    // Clean constructor injection
    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient addPatient(Patient patient) {
        return patientRepository.save(patient);
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Patient getPatientById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + id));
    }

    // Update patient clinical history
    public Patient updateMedicalHistory(Long id, String medicalHistory) {
        Patient patient = getPatientById(id);
        patient.setMedicalHistory(medicalHistory);
        return patientRepository.save(patient);
    }
}