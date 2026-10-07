package com.medilink.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medilink.entity.DeliveryLocation;

public interface DeliveryLocationRepository
        extends JpaRepository<DeliveryLocation, Long> {

    Optional<DeliveryLocation> findByOrderId(Long orderId);
}
