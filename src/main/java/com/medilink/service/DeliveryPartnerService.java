package com.medilink.service;

import com.medilink.entity.DeliveryPartner;
import com.medilink.repository.DeliveryPartnerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeliveryPartnerService {

    private final DeliveryPartnerRepository repository;

    public DeliveryPartnerService(
            DeliveryPartnerRepository repository) {
        this.repository = repository;
    }

    public DeliveryPartner createPartner(
            DeliveryPartner partner) {

        if (partner.getEmail() == null ||
                partner.getEmail().isBlank()) {
            throw new RuntimeException(
                    "Delivery partner email is required");
        }

        if (repository.findByEmail(partner.getEmail())
                .isPresent()) {
            throw new RuntimeException(
                    "Delivery partner email already exists");
        }

        partner.setAvailable(true);

        return repository.save(partner);
    }

    public List<DeliveryPartner> getAllPartners() {
        return repository.findAll();
    }

    public List<DeliveryPartner> getAvailablePartners() {
        return repository.findByAvailableTrue();
    }

    public DeliveryPartner getPartner(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Delivery partner not found"));
    }

    public DeliveryPartner updateAvailability(
            Long id,
            boolean available) {

        DeliveryPartner partner = getPartner(id);

        partner.setAvailable(available);

        return repository.save(partner);
    }

    public void deletePartner(Long id) {

        if (!repository.existsById(id)) {
            throw new RuntimeException(
                    "Delivery partner not found");
        }

        repository.deleteById(id);
    }
}