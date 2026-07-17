package com.aurahealth.monolith.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "patients")
public class Patient {
    public enum SubscriptionTier { FREE, EARLY_ASSISTANCE, PERSONAL_ASSISTANCE, COMPREHENSIVE_PRIORITY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    private String name;
    private String email;

    @Column(name = "medical_history")
    private String medicalHistory;

    @Column(name = "current_vitals")
    private String currentVitals;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false, length = 32)
    private SubscriptionTier subscriptionTier = SubscriptionTier.FREE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }

    public String getMedicalHistorySummary() { return medicalHistory; }
    public void setMedicalHistorySummary(String summary) { this.medicalHistory = summary; }

    public String getCurrentVitals() { return currentVitals; }
    public void setCurrentVitals(String currentVitals) { this.currentVitals = currentVitals; }

    public String getSubscriptionTier() { return subscriptionTier.name(); }
    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = SubscriptionTier.valueOf(subscriptionTier.toUpperCase());
    }

    public boolean isPremiumTier() {
        return subscriptionTier != SubscriptionTier.FREE;
    }
}
