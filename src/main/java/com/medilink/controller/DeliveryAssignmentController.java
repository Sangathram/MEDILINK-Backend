package com.medilink.controller;

import com.medilink.entity.DeliveryPartner;
import com.medilink.entity.Order;
import com.medilink.repository.DeliveryPartnerRepository;
import com.medilink.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery")
@CrossOrigin
public class DeliveryAssignmentController {

    private final OrderRepository orderRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;

    public DeliveryAssignmentController(
            OrderRepository orderRepository,
            DeliveryPartnerRepository deliveryPartnerRepository) {

        this.orderRepository = orderRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
    }

    @PutMapping("/assign/{orderId}/{partnerId}")
    public ResponseEntity<Order> assignPartner(
            @PathVariable Long orderId,
            @PathVariable Long partnerId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        DeliveryPartner partner =
                deliveryPartnerRepository.findById(partnerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Delivery partner not found"));

        order.setDeliveryPartnerId(partner.getId());
        order.setDeliveryPartner(partner.getName());
        order.setDeliveryPhone(partner.getPhone());

        partner.setAvailable(false);
        deliveryPartnerRepository.save(partner);

        return ResponseEntity.ok(
                orderRepository.save(order));
    }
}