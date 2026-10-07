package com.medilink.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    // Existing medicine order
    private Long medicineId;

    // New healthcare/wellness product order
    private Long healthcareProductId;

    // MEDICINE or HEALTHCARE
    private String productType;

    private Integer quantity;
    private Double totalPrice;
    private String status;

    // DELIVERY TRACKING
    private String trackingId;
    private String deliveryStatus;
    private String deliveryPartner;
    private String deliveryPhone;

    // Assigned delivery partner ID
    private Long deliveryPartnerId;

    private String estimatedDelivery;
    private LocalDateTime packedAt;
    private LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;

    public Order() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Long medicineId) {
        this.medicineId = medicineId;
    }

    public Long getHealthcareProductId() {
        return healthcareProductId;
    }

    public void setHealthcareProductId(Long healthcareProductId) {
        this.healthcareProductId = healthcareProductId;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getDeliveryPartner() {
        return deliveryPartner;
    }

    public void setDeliveryPartner(String deliveryPartner) {
        this.deliveryPartner = deliveryPartner;
    }

    public String getDeliveryPhone() {
        return deliveryPhone;
    }

    public void setDeliveryPhone(String deliveryPhone) {
        this.deliveryPhone = deliveryPhone;
    }

    public Long getDeliveryPartnerId() {
        return deliveryPartnerId;
    }

    public void setDeliveryPartnerId(Long deliveryPartnerId) {
        this.deliveryPartnerId = deliveryPartnerId;
    }

    public String getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public void setEstimatedDelivery(String estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }

    public LocalDateTime getPackedAt() {
        return packedAt;
    }

    public void setPackedAt(LocalDateTime packedAt) {
        this.packedAt = packedAt;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(LocalDateTime dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
