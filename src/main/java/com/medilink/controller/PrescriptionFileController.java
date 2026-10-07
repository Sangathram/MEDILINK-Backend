package com.medilink.controller;

import com.medilink.entity.Prescription;
import com.medilink.entity.User;
import com.medilink.repository.PrescriptionRepository;
import com.medilink.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/prescriptions")
@CrossOrigin(origins = "http://localhost:5173")
public class PrescriptionFileController {

    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;

    public PrescriptionFileController(
            PrescriptionRepository prescriptionRepository,
            UserRepository userRepository) {

        this.prescriptionRepository = prescriptionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> viewPrescription(
            @PathVariable Long id) {

        try {
            // Find prescription
            Optional<Prescription> optionalPrescription =
                    prescriptionRepository.findById(id);

            if (optionalPrescription.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Prescription prescription = optionalPrescription.get();

            // Get logged-in user
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null ||
                    authentication.getName() == null) {

                return ResponseEntity.status(401).build();
            }

            String email = authentication.getName();

            Optional<User> optionalUser =
                    userRepository.findByEmail(email);

            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(401).build();
            }

            User user = optionalUser.get();

            // Check role
            boolean isAdminOrPharmacy =
                    user.getRole() != null &&
                    (user.getRole().toString().equalsIgnoreCase("ADMIN")
                            || user.getRole().toString().equalsIgnoreCase("PHARMACY"));

            // Patient can only view their own prescription
            if (!isAdminOrPharmacy &&
                    !prescription.getPatientId().equals(user.getId())) {

                return ResponseEntity.status(403).build();
            }

            // Build file path safely
            Path uploadRoot =
                    Paths.get("uploads", "prescriptions")
                            .toAbsolutePath()
                            .normalize();

            Path filePath =
                    Paths.get(prescription.getFilePath())
                            .toAbsolutePath()
                            .normalize();

            // Prevent path traversal
            if (!filePath.startsWith(uploadRoot)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource =
                    new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType =
                    "application/octet-stream";

            String fileName =
                    prescription.getFileName();

            if (fileName != null) {

                String lowerName =
                        fileName.toLowerCase();

                if (lowerName.endsWith(".pdf")) {
                    contentType = "application/pdf";

                } else if (lowerName.endsWith(".jpg")
                        || lowerName.endsWith(".jpeg")) {

                    contentType = "image/jpeg";

                } else if (lowerName.endsWith(".png")) {

                    contentType = "image/png";
                }
            }

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(contentType))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" +
                                    fileName +
                                    "\"")
                    .body(resource);

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .build();
        }
    }
}