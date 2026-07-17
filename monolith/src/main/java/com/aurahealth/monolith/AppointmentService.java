package com.aurahealth.monolith;

import com.aurahealth.monolith.entity.Appointment;
import com.aurahealth.monolith.entity.Doctor;
import com.aurahealth.monolith.entity.Patient;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    public Appointment createAppointment(Long patientId, Long doctorId, LocalDateTime appointmentTime, String symptoms) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + patientId));

        // 1. Check if the doctor is available
        if (!doctor.isAvailable()) {
            throw new IllegalStateException("Doctor " + doctor.getName() + " is currently not accepting appointments.");
        }

        // 2. Prevent double-booking using your clean ID-based repository method
        boolean isDoubleBooked = appointmentRepository.existsByDoctorIdAndAppointmentTime(doctorId, appointmentTime);
        if (isDoubleBooked) {
            throw new IllegalStateException("Doctor " + doctor.getName() + " already has an appointment scheduled at " + appointmentTime);
        }

        // Direct instantiation bypassing Lombok builder issues
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus("PENDING");
        appointment.setSymptoms(symptoms);

        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public Appointment updateStatus(Long appointmentId, String status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        appointment.setStatus(status.toUpperCase());
        return appointmentRepository.save(appointment);
    }
}
