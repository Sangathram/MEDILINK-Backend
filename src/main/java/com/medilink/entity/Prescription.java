package com.medilink.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Prescription() {
    }


    public Prescription(
            Long patientId,
            String fileName,
            String filePath,
            String status,
            LocalDateTime uploadedAt) {

        this.patientId = patientId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.status = status;
        this.uploadedAt = uploadedAt;
    }


    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}