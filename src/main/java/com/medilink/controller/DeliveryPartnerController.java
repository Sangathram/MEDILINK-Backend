package com.medilink.controller;

import com.medilink.entity.DeliveryPartner;
import com.medilink.service.DeliveryPartnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-partners")
@CrossOrigin
public class DeliveryPartnerController {

    private final DeliveryPartnerService service;

    public DeliveryPartnerController(
            DeliveryPartnerService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DeliveryPartner> createPartner(
            @RequestBody DeliveryPartner partner) {

        return ResponseEntity.ok(
                service.createPartner(partner));
    }

    @GetMapping
    public ResponseEntity<List<DeliveryPartner>> getAllPartners() {

        return ResponseEntity.ok(
                service.getAllPartners());
    }

    @GetMapping("/available")
    public ResponseEntity<List<DeliveryPartner>>
    getAvailablePartners() {

        return ResponseEntity.ok(
                service.getAvailablePartners());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryPartner> getPartner(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.getPartner(id));
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<DeliveryPartner>
    updateAvailability(
            @PathVariable Long id,
            @RequestParam boolean available) {

        return ResponseEntity.ok(
                service.updateAvailability(
                        id, available));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePartner(
            @PathVariable Long id) {

        service.deletePartner(id);

        return ResponseEntity.ok(
                "Delivery partner deleted successfully");
    }
}