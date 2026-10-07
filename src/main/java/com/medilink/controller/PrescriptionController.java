package com.medilink.controller;

import com.medilink.entity.Prescription;
import com.medilink.service.PrescriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@CrossOrigin(origins = "http://localhost:5173")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    // PATIENT: Upload prescription
    @PostMapping("/upload")
    public ResponseEntity<Prescription> uploadPrescription(
            @RequestParam("file") MultipartFile file) {

        Prescription prescription =
                prescriptionService.uploadPrescription(file);

        return ResponseEntity.ok(prescription);
    }

    // PATIENT: View own prescriptions
    @GetMapping("/my")
    public ResponseEntity<List<Prescription>> getMyPrescriptions() {

        return ResponseEntity.ok(
                prescriptionService.getMyPrescriptions()
        );
    }

    // ADMIN / PHARMACY: View all prescriptions
    @GetMapping
    public ResponseEntity<List<Prescription>> getAllPrescriptions() {

        return ResponseEntity.ok(
                prescriptionService.getAllPrescriptions()
        );
    }

    // ADMIN / PHARMACY: Approve or reject prescription
    @PutMapping("/{id}/status")
    public ResponseEntity<Prescription> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        Prescription prescription =
                prescriptionService.updatePrescriptionStatus(id, status);

        return ResponseEntity.ok(prescription);
    }
}