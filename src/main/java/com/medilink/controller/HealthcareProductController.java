package com.medilink.controller;

import com.medilink.entity.HealthcareProduct;
import com.medilink.service.HealthcareProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/healthcare-products")
@CrossOrigin(origins = "http://localhost:5173")
public class HealthcareProductController {

    private final HealthcareProductService healthcareProductService;

    public HealthcareProductController(HealthcareProductService healthcareProductService) {
        this.healthcareProductService = healthcareProductService;
    }

    // GET all healthcare products
    @GetMapping
    public ResponseEntity<List<HealthcareProduct>> getAllProducts() {
        return ResponseEntity.ok(healthcareProductService.getAllProducts());
    }

    // GET healthcare product by ID
    @GetMapping("/{id}")
    public ResponseEntity<HealthcareProduct> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(healthcareProductService.getProductById(id));
    }

    // GET products by category
    @GetMapping("/category/{category}")
    public ResponseEntity<List<HealthcareProduct>> getProductsByCategory(
            @PathVariable String category) {

        return ResponseEntity.ok(
                healthcareProductService.getProductsByCategory(category)
        );
    }

    // ADMIN / PHARMACY - Add product
    @PostMapping
    public ResponseEntity<HealthcareProduct> addProduct(
            @RequestBody HealthcareProduct product) {

        return ResponseEntity.ok(
                healthcareProductService.addProduct(product)
        );
    }

    // ADMIN / PHARMACY - Update product
    @PutMapping("/{id}")
    public ResponseEntity<HealthcareProduct> updateProduct(
            @PathVariable Long id,
            @RequestBody HealthcareProduct product) {

        return ResponseEntity.ok(
                healthcareProductService.updateProduct(id, product)
        );
    }

    // ADMIN / PHARMACY - Delete product
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {

        healthcareProductService.deleteProduct(id);

        return ResponseEntity.ok("Healthcare product deleted successfully");
    }
}