package com.medilink.repository;

import com.medilink.entity.DeliveryPartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryPartnerRepository
        extends JpaRepository<DeliveryPartner, Long> {

    Optional<DeliveryPartner> findByEmail(String email);

    List<DeliveryPartner> findByAvailableTrue();
}