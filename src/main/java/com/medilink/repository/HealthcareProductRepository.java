package com.medilink.repository;

import com.medilink.entity.HealthcareProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthcareProductRepository extends JpaRepository<HealthcareProduct, Long> {

    List<HealthcareProduct> findByCategoryIgnoreCase(String category);
}