package com.aurahealth.monolith;

import com.aurahealth.monolith.entity.Billing;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/invoice")
    public ResponseEntity<Billing> createInvoice(@RequestParam Long appointmentId, @RequestParam double amount) {
        Billing billing = billingService.createInvoice(appointmentId, amount);
        return new ResponseEntity<>(billing, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Billing>> getAllInvoices() {
        return ResponseEntity.ok(billingService.getAllInvoices());
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<Billing> processPayment(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(billingService.updatePaymentStatus(id, status));
    }
}