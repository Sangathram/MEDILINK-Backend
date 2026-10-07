package com.medilink.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medilink.entity.Medicine;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    List<Medicine> findByNameContainingIgnoreCase(String name);

}