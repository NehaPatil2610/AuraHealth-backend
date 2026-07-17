package com.aurahealth.monolith.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    private String name;
    private String email;

    @Column(name = "specialty")
    private String specialization;

    @Column(name = "license_id")
    private String licenseNumber;

    @Column(name = "is_approved", nullable = false)
    private boolean approvalStatus = false;

    private boolean available = true;

    private String city = "Mumbai";

    @Column(name = "consultation_fee")
    private double consultationFee = 500;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public boolean isApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(boolean approvalStatus) { this.approvalStatus = approvalStatus; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(double consultationFee) { this.consultationFee = consultationFee; }
}
