package com.medilink.controller;

import com.medilink.entity.MedicalRecord;
import com.medilink.entity.User;
import com.medilink.repository.UserRepository;
import com.medilink.service.MedicalRecordService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/medical-records")
@CrossOrigin(origins = "http://localhost:5173")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final UserRepository userRepository;

    public MedicalRecordController(
            MedicalRecordService medicalRecordService,
            UserRepository userRepository
    ) {
        this.medicalRecordService = medicalRecordService;
        this.userRepository = userRepository;
    }

    // Upload medical record
    @PostMapping("/upload")
    public ResponseEntity<?> uploadRecord(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "recordType", required = false)
            String recordType,
            Authentication authentication
    ) {

        try {

            String email = authentication.getName();

            Optional<User> optionalUser =
                    userRepository.findByEmail(email);

            if (optionalUser.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("User not found.");
            }

            User user = optionalUser.get();

            if (!"PATIENT".equals(user.getRole())) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body("Only patients can upload medical records.");
            }

            MedicalRecord record =
                    medicalRecordService.uploadRecord(
                            user.getId(),
                            file,
                            recordType
                    );

            return ResponseEntity.ok(record);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // Get logged-in patient's medical records
    @GetMapping("/my")
    public ResponseEntity<?> getMyRecords(
            Authentication authentication
    ) {

        String email = authentication.getName();

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User not found.");
        }

        User user = optionalUser.get();

        List<MedicalRecord> records =
                medicalRecordService.getPatientRecords(
                        user.getId()
                );

        return ResponseEntity.ok(records);
    }
}