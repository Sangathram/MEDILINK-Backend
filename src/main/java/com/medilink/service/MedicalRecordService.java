package com.medilink.service;

import com.medilink.entity.MedicalRecord;
import com.medilink.repository.MedicalRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    private final Path uploadDirectory =
            Paths.get("uploads/medical-records");

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
    }

    public MedicalRecord uploadRecord(
            Long patientId,
            MultipartFile file,
            String recordType
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Please select a medical record file.");
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            throw new RuntimeException("Invalid file name.");
        }

        String lowerName = originalFileName.toLowerCase();

        if (!lowerName.endsWith(".pdf")
                && !lowerName.endsWith(".jpg")
                && !lowerName.endsWith(".jpeg")
                && !lowerName.endsWith(".png")) {

            throw new RuntimeException(
                    "Only PDF, JPG, JPEG and PNG files are allowed."
            );
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException(
                    "File size must be less than 10 MB."
            );
        }

        Files.createDirectories(uploadDirectory);

        String safeFileName =
                System.currentTimeMillis() + "_" +
                originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");

        Path targetPath = uploadDirectory.resolve(safeFileName);

        Files.copy(
                file.getInputStream(),
                targetPath
        );

        MedicalRecord record = new MedicalRecord();

        record.setPatientId(patientId);
        record.setFileName(originalFileName);
        record.setFilePath(targetPath.toString());
        record.setRecordType(
                recordType == null || recordType.isBlank()
                        ? "MEDICAL_RECORD"
                        : recordType
        );
        record.setUploadedAt(LocalDateTime.now());

        return medicalRecordRepository.save(record);
    }

    public List<MedicalRecord> getPatientRecords(Long patientId) {
        return medicalRecordRepository.findByPatientId(patientId);
    }

    public MedicalRecord getRecord(Long id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Medical record not found.")
                );
    }
}