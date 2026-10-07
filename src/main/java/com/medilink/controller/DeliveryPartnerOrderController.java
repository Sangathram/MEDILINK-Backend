package com.medilink.controller;

import com.medilink.entity.Order;
import com.medilink.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-partner/orders")
@CrossOrigin
public class DeliveryPartnerOrderController {

    private final OrderRepository orderRepository;

    public DeliveryPartnerOrderController(
            OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/{partnerId}")
    public ResponseEntity<List<Order>> getAssignedOrders(
            @PathVariable Long partnerId) {

        return ResponseEntity.ok(
                orderRepository
                        .findByDeliveryPartnerIdOrderByIdDesc(partnerId)
        );
    }
}