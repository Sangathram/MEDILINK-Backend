package com.medilink.controller;

import com.medilink.entity.PatientProfile;
import com.medilink.entity.User;
import com.medilink.repository.UserRepository;
import com.medilink.service.PatientProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/patient-profile")
@CrossOrigin(origins = "http://localhost:5173")
public class PatientProfileController {

    private final PatientProfileService patientProfileService;
    private final UserRepository userRepository;

    public PatientProfileController(
            PatientProfileService patientProfileService,
            UserRepository userRepository) {

        this.patientProfileService = patientProfileService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<PatientProfile> getProfile(
            Authentication authentication) {

        String email = authentication.getName();

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        User user = optionalUser.get();

        PatientProfile profile =
                patientProfileService.getProfile(user.getId());

        if (profile == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<PatientProfile> saveProfile(
            Authentication authentication,
            @RequestBody PatientProfile profileData) {

        String email = authentication.getName();

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        User user = optionalUser.get();

        PatientProfile savedProfile =
                patientProfileService.saveOrUpdateProfile(
                        user.getId(),
                        profileData
                );

        return ResponseEntity.ok(savedProfile);
    }
}