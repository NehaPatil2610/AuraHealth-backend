package com.aurahealth.monolith;

import com.aurahealth.monolith.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientIdOrderByRecordDateDesc(Long patientId);
    Optional<MedicalRecord> findByIdAndPatientId(Long id, Long patientId);
}
