package com.medilink.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.medilink.entity.DeliveryLocation;
import com.medilink.entity.DeliveryPartner;
import com.medilink.entity.Order;
import com.medilink.entity.User;
import com.medilink.repository.DeliveryLocationRepository;
import com.medilink.repository.DeliveryPartnerRepository;
import com.medilink.repository.UserRepository;
import com.medilink.service.OrderService;

@RestController
@RequestMapping("/api/delivery")
@CrossOrigin
public class DeliveryLocationController {

    private final DeliveryLocationRepository locationRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    public DeliveryLocationController(
            DeliveryLocationRepository locationRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            UserRepository userRepository,
            OrderService orderService) {

        this.locationRepository = locationRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    // =========================================================
    // DELIVERY PARTNER -> SEND CURRENT GPS LOCATION
    // =========================================================

    @PostMapping("/location/{orderId}")
    public ResponseEntity<DeliveryLocation> updateLocation(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        User user = getUser(authentication);

        if (!"DELIVERY_PARTNER".equals(user.getRole())) {
            return ResponseEntity.status(403).build();
        }

        DeliveryPartner partner =
                deliveryPartnerRepository
                        .findByEmail(user.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Delivery partner profile not found"));

        Order order = orderService.getOrderById(orderId);

        if (order == null) {
            throw new RuntimeException("Order not found");
        }

        if (order.getDeliveryPartnerId() == null ||
                !order.getDeliveryPartnerId().equals(partner.getId())) {

            return ResponseEntity.status(403).build();
        }

        if (order.getStatus().equals("DELIVERED")) {
            throw new RuntimeException(
                    "Location sharing is stopped because the order is delivered");
        }

        Double latitude = toDouble(request.get("latitude"));
        Double longitude = toDouble(request.get("longitude"));
        Double accuracy = toDouble(request.get("accuracy"));

        if (latitude == null || longitude == null) {
            throw new RuntimeException(
                    "Latitude and longitude are required");
        }

        if (latitude < -90 || latitude > 90 ||
                longitude < -180 || longitude > 180) {

            throw new RuntimeException("Invalid GPS coordinates");
        }

        DeliveryLocation location =
                locationRepository
                        .findByOrderId(orderId)
                        .orElseGet(DeliveryLocation::new);

        location.setOrderId(orderId);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);
        location.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(
                locationRepository.save(location));
    }

    // =========================================================
    // PATIENT / AUTHORIZED USER -> GET CURRENT DELIVERY LOCATION
    // =========================================================

    @GetMapping("/location/{orderId}")
    public ResponseEntity<DeliveryLocation> getLocation(
            @PathVariable Long orderId,
            Authentication authentication) {

        User user = getUser(authentication);

        Order order = orderService.getOrderById(orderId);

        if (order == null) {
            throw new RuntimeException("Order not found");
        }

        String role = user.getRole();

        // ADMIN / PHARMACY can view any delivery location
        if ("ADMIN".equals(role) ||
                "PHARMACY".equals(role)) {

            return locationRepository.findByOrderId(orderId)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.noContent().build());
        }

        // PATIENT can view only their own order
        if ("PATIENT".equals(role)) {

            if (!user.getId().equals(order.getUserId())) {
                return ResponseEntity.status(403).build();
            }

            return locationRepository.findByOrderId(orderId)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.noContent().build());
        }

        // DELIVERY PARTNER can view only their assigned order
        if ("DELIVERY_PARTNER".equals(role)) {

            DeliveryPartner partner =
                    deliveryPartnerRepository
                            .findByEmail(user.getEmail())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Delivery partner profile not found"));

            if (order.getDeliveryPartnerId() == null ||
                    !order.getDeliveryPartnerId()
                            .equals(partner.getId())) {

                return ResponseEntity.status(403).build();
            }

            return locationRepository.findByOrderId(orderId)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.noContent().build());
        }

        return ResponseEntity.status(403).build();
    }

    private User getUser(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException("Authentication required");
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    private Double toDouble(Object value) {

        if (value == null) {
            return null;
        }

        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
