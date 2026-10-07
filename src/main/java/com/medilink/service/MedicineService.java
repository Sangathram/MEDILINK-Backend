package com.medilink.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.medilink.entity.Medicine;
import com.medilink.repository.MedicineRepository;

@Service
public class MedicineService {

    private final MedicineRepository medicineRepository;

    public MedicineService(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    // Get all medicines
    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    // Get medicine by ID
    public Medicine getMedicineById(Long id) {
        return medicineRepository.findById(id).orElse(null);
    }

    // Add new medicine
    public Medicine addMedicine(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    // Update medicine
    public Medicine updateMedicine(Long id, Medicine medicine) {

        Medicine existing = medicineRepository.findById(id).orElse(null);

        if (existing == null) {
            return null;
        }

        existing.setName(medicine.getName());
        existing.setManufacturer(medicine.getManufacturer());
        existing.setDescription(medicine.getDescription());
        existing.setPrice(medicine.getPrice());
        existing.setStock(medicine.getStock());

        return medicineRepository.save(existing);
    }

    // Delete medicine
    public void deleteMedicine(Long id) {
        medicineRepository.deleteById(id);
    }

    // Search medicines by name
    public List<Medicine> searchMedicines(String name) {
        return medicineRepository.findByNameContainingIgnoreCase(name);
    }
}