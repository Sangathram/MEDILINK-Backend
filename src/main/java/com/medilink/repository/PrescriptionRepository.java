package com.medilink.repository;

import com.medilink.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository
        extends JpaRepository<Prescription, Long> {

    List<Prescription> findByPatientId(Long patientId);

    List<Prescription> findByStatus(String status);
}