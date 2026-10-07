package com.medilink.service;

import com.medilink.entity.HealthcareProduct;
import com.medilink.repository.HealthcareProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HealthcareProductService {

    private final HealthcareProductRepository healthcareProductRepository;

    public HealthcareProductService(HealthcareProductRepository healthcareProductRepository) {
        this.healthcareProductRepository = healthcareProductRepository;
    }

    // Get all healthcare products
    public List<HealthcareProduct> getAllProducts() {
        return healthcareProductRepository.findAll();
    }

    // Get product by ID
    public HealthcareProduct getProductById(Long id) {
        return healthcareProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Healthcare product not found"));
    }

    // Get products by category
    public List<HealthcareProduct> getProductsByCategory(String category) {
        return healthcareProductRepository.findByCategoryIgnoreCase(category);
    }

    // Add new healthcare product
    public HealthcareProduct addProduct(HealthcareProduct product) {

        if (product.getAvailable() == null) {
            product.setAvailable(true);
        }

        return healthcareProductRepository.save(product);
    }

    // Update healthcare product
    public HealthcareProduct updateProduct(Long id, HealthcareProduct updatedProduct) {

        HealthcareProduct existingProduct = getProductById(id);

        existingProduct.setName(updatedProduct.getName());
        existingProduct.setCategory(updatedProduct.getCategory());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setStock(updatedProduct.getStock());
        existingProduct.setImage(updatedProduct.getImage());
        existingProduct.setAvailable(updatedProduct.getAvailable());

        return healthcareProductRepository.save(existingProduct);
    }

    // Delete healthcare product
    public void deleteProduct(Long id) {

        HealthcareProduct existingProduct = getProductById(id);

        healthcareProductRepository.delete(existingProduct);
    }
}