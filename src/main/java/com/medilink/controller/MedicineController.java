package com.medilink.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.medilink.entity.Medicine;
import com.medilink.service.MedicineService;

@RestController
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    // Get all medicines
    @GetMapping("/api/medicines")
    public List<Medicine> getAllMedicines() {
        return medicineService.getAllMedicines();
    }

    // Get medicine by ID
    @GetMapping("/api/medicines/{id}")
    public Medicine getMedicineById(@PathVariable Long id) {
        return medicineService.getMedicineById(id);
    }

    // Search medicines by name
    @GetMapping("/api/medicines/search")
    public List<Medicine> searchMedicines(@RequestParam String name) {
        return medicineService.searchMedicines(name);
    }

    // Add new medicine
    @PostMapping("/api/medicines")
    public Medicine addMedicine(@RequestBody Medicine medicine) {
        return medicineService.addMedicine(medicine);
    }

    // Update medicine
    @PutMapping("/api/medicines/{id}")
    public Medicine updateMedicine(
            @PathVariable Long id,
            @RequestBody Medicine medicine) {

        return medicineService.updateMedicine(id, medicine);
    }

    // Delete medicine
    @DeleteMapping("/api/medicines/{id}")
    public String deleteMedicine(@PathVariable Long id) {
        medicineService.deleteMedicine(id);
        return "Medicine deleted successfully";
    }
}