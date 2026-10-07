package com.medilink.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.medilink.entity.Prescription;
import com.medilink.entity.User;
import com.medilink.repository.PrescriptionRepository;
import com.medilink.repository.UserRepository;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;

    // Folder where prescriptions will be stored
    private final Path uploadDirectory =
            Paths.get("uploads", "prescriptions");


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public PrescriptionService(
            PrescriptionRepository prescriptionRepository,
            UserRepository userRepository) {

        this.prescriptionRepository = prescriptionRepository;
        this.userRepository = userRepository;

        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not create prescription upload folder"
            );
        }
    }


    // =========================================================
    // GET LOGGED-IN USER
    // =========================================================

    private User getLoggedInUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }

        String email = authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Logged-in user not found"
                        )
                );
    }


    // =========================================================
    // UPLOAD PRESCRIPTION
    // =========================================================

    public Prescription uploadPrescription(
            MultipartFile file) {

        // -----------------------------------------------------
        // Get logged-in user
        // -----------------------------------------------------

        User loggedInUser = getLoggedInUser();


        // -----------------------------------------------------
        // Only PATIENT can upload
        // -----------------------------------------------------

        if (!"PATIENT".equals(loggedInUser.getRole())) {

            throw new RuntimeException(
                    "Only patients can upload prescriptions"
            );
        }


        // -----------------------------------------------------
        // Check file
        // -----------------------------------------------------

        if (file == null ||
                file.isEmpty()) {

            throw new RuntimeException(
                    "Please select a prescription file"
            );
        }


        // -----------------------------------------------------
        // Check file size
        // Maximum: 5 MB
        // -----------------------------------------------------

        if (file.getSize() > 5 * 1024 * 1024) {

            throw new RuntimeException(
                    "Prescription file must be smaller than 5 MB"
            );
        }


        // -----------------------------------------------------
        // Get original file name
        // -----------------------------------------------------

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null ||
                originalFileName.isBlank()) {

            throw new RuntimeException(
                    "Invalid file name"
            );
        }


        // -----------------------------------------------------
        // Validate extension
        // -----------------------------------------------------

        String lowerFileName =
                originalFileName.toLowerCase();

        boolean validType =
                lowerFileName.endsWith(".pdf") ||
                lowerFileName.endsWith(".jpg") ||
                lowerFileName.endsWith(".jpeg") ||
                lowerFileName.endsWith(".png");

        if (!validType) {

            throw new RuntimeException(
                    "Only PDF, JPG, JPEG and PNG files are allowed"
            );
        }


        // -----------------------------------------------------
        // Create safe unique file name
        // -----------------------------------------------------

        String extension = "";

        int dotIndex =
                originalFileName.lastIndexOf(".");

        if (dotIndex >= 0) {
            extension =
                    originalFileName.substring(dotIndex);
        }

        String storedFileName =
                UUID.randomUUID() + extension;


        // -----------------------------------------------------
        // Save file
        // -----------------------------------------------------

        Path targetPath =
                uploadDirectory.resolve(storedFileName);

        try {

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to save prescription file"
            );
        }


        // -----------------------------------------------------
        // Create database record
        // -----------------------------------------------------

        Prescription prescription =
                new Prescription();

        prescription.setPatientId(
                loggedInUser.getId()
        );

        prescription.setFileName(
                originalFileName
        );

        prescription.setFilePath(
                targetPath.toString()
        );

        prescription.setStatus(
                "PENDING"
        );

        prescription.setUploadedAt(
                LocalDateTime.now()
        );


        // -----------------------------------------------------
        // Save database record
        // -----------------------------------------------------

        return prescriptionRepository.save(
                prescription
        );
    }


    // =========================================================
    // GET PATIENT PRESCRIPTIONS
    // =========================================================

    public List<Prescription> getMyPrescriptions() {

        User loggedInUser = getLoggedInUser();

        return prescriptionRepository
                .findByPatientId(loggedInUser.getId());
    }


    // =========================================================
    // GET ALL PRESCRIPTIONS
    // PHARMACY + ADMIN
    // =========================================================

    public List<Prescription> getAllPrescriptions() {

        User loggedInUser = getLoggedInUser();

        if (!"ADMIN".equals(loggedInUser.getRole()) &&
                !"PHARMACY".equals(loggedInUser.getRole())) {

            throw new RuntimeException(
                    "Only ADMIN and PHARMACY can view prescriptions"
            );
        }

        return prescriptionRepository.findAll();
    }


    // =========================================================
    // UPDATE PRESCRIPTION STATUS
    // PHARMACY + ADMIN
    // =========================================================

    public Prescription updatePrescriptionStatus(
            Long id,
            String newStatus) {

        User loggedInUser = getLoggedInUser();

        if (!"ADMIN".equals(loggedInUser.getRole()) &&
                !"PHARMACY".equals(loggedInUser.getRole())) {

            throw new RuntimeException(
                    "Only ADMIN and PHARMACY can update prescriptions"
            );
        }


        if (newStatus == null ||
                newStatus.isBlank()) {

            throw new RuntimeException(
                    "Prescription status cannot be empty"
            );
        }


        newStatus =
                newStatus.toUpperCase();


        if (!newStatus.equals("APPROVED") &&
                !newStatus.equals("REJECTED")) {

            throw new RuntimeException(
                    "Status must be APPROVED or REJECTED"
            );
        }


        Prescription prescription =
                prescriptionRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Prescription not found"
                                )
                        );


        if (!prescription.getStatus()
                .equals("PENDING")) {

            throw new RuntimeException(
                    "This prescription has already been reviewed"
            );
        }


        prescription.setStatus(
                newStatus
        );


        return prescriptionRepository.save(
                prescription
        );
    }
}