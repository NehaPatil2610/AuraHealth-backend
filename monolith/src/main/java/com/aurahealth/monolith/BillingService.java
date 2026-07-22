package com.aurahealth.monolith;

import com.aurahealth.monolith.entity.Billing;
import com.aurahealth.monolith.entity.Appointment;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BillingService {

    private final BillingRepository billingRepository;
    private final AppointmentRepository appointmentRepository;

    public BillingService(BillingRepository billingRepository, AppointmentRepository appointmentRepository) {
        this.billingRepository = billingRepository;
        this.appointmentRepository = appointmentRepository;
    }

    // Generate a fresh invoice for a completed appointment
    public Billing createInvoice(Long appointmentId, double amount, String description) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        // Direct instantiation bypassing Lombok builder issues
        Billing billing = new Billing();
        billing.setAppointment(appointment);
        billing.setAmount(amount);
        billing.setStatus("UNPAID");
        billing.setDescription(description);

        return billingRepository.save(billing);
    }

    public List<Billing> getAllInvoices() {
        return billingRepository.findAll();
    }

    // Process a payment update
    public Billing updatePaymentStatus(Long billingId, String status) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new RuntimeException("Invoice record not found with ID: " + billingId));
        billing.setStatus(status.toUpperCase());
        return billingRepository.save(billing);
    }
}