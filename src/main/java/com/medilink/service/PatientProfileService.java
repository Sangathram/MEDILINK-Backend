package com.medilink.service;

import com.medilink.entity.PatientProfile;
import com.medilink.repository.PatientProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PatientProfileService {

    private final PatientProfileRepository patientProfileRepository;

    public PatientProfileService(PatientProfileRepository patientProfileRepository) {
        this.patientProfileRepository = patientProfileRepository;
    }

    public PatientProfile getProfile(Long userId) {
        Optional<PatientProfile> profile =
                patientProfileRepository.findByUserId(userId);

        return profile.orElse(null);
    }

    public PatientProfile saveOrUpdateProfile(
            Long userId,
            PatientProfile profileData) {

        PatientProfile profile =
                patientProfileRepository.findByUserId(userId)
                        .orElse(new PatientProfile());

        profile.setUserId(userId);

        profile.setName(profileData.getName());
        profile.setAge(profileData.getAge());
        profile.setGender(profileData.getGender());
        profile.setPhone(profileData.getPhone());
        profile.setEmail(profileData.getEmail());
        profile.setAddress(profileData.getAddress());
        profile.setCity(profileData.getCity());
        profile.setState(profileData.getState());
        profile.setPinCode(profileData.getPinCode());
        profile.setEmergencyContact(profileData.getEmergencyContact());
        profile.setPreferredLanguage(profileData.getPreferredLanguage());
        profile.setBloodGroup(profileData.getBloodGroup());
        profile.setHealthConditions(profileData.getHealthConditions());
        profile.setAllergies(profileData.getAllergies());
        profile.setCurrentMedications(profileData.getCurrentMedications());

        profile.setUpdatedAt(LocalDateTime.now());

        return patientProfileRepository.save(profile);
    }
}