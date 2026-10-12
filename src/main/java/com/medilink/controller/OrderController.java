
package com.medilink.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medilink.entity.DeliveryPartner;
import com.medilink.entity.Order;
import com.medilink.entity.User;
import com.medilink.repository.DeliveryPartnerRepository;
import com.medilink.repository.OrderRepository;
import com.medilink.repository.UserRepository;
import com.medilink.service.OrderService;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final OrderRepository orderRepository;

    public OrderController(
            OrderService orderService,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            OrderRepository orderRepository) {

        this.orderService = orderService;
        this.userRepository = userRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.orderRepository = orderRepository;
    }

    // PLACE SINGLE ORDER
    @PostMapping
    public Order placeOrder(@RequestBody Order order) {
        return orderService.placeOrder(order);
    }

    // PLACE CART ORDERS
    @PostMapping("/cart")
    public List<Order> placeCartOrders(@RequestBody List<Order> orders) {
        return orderService.placeCartOrders(orders);
    }


    // GET AVAILABLE ORDERS FOR DELIVERY PARTNERS
    @GetMapping("/available")
    public List<Order> getAvailableOrders() {

        User user = getLoggedInUser();

        if (!"DELIVERY_PARTNER".equals(user.getRole())) {
            throw new RuntimeException(
                    "Only delivery partners can view available orders");
        }

        DeliveryPartner partner = getDeliveryPartner(user);

        boolean isAvailable = deliveryPartnerRepository
                .findByAvailableTrue()
                .stream()
                .anyMatch(p -> p.getId().equals(partner.getId()));

        if (!isAvailable) {
            throw new RuntimeException(
                    "You must be available to accept orders");
        }

        return orderRepository.findAll()
                .stream()
                .filter(order ->
                        "PLACED".equalsIgnoreCase(order.getStatus())
                        && order.getDeliveryPartnerId() == null)
                .collect(java.util.stream.Collectors.toList());
    }


    // GET ALL ORDERS: ADMIN / PHARMACY
    @GetMapping
    public List<Order> getAllOrders() {
        User user = getLoggedInUser();

        if (!"ADMIN".equals(user.getRole())
                && !"PHARMACY".equals(user.getRole())) {
            throw new RuntimeException(
                    "You are not allowed to view all orders");
        }

        return orderService.getAllOrders();
    }

    // GET ORDER BY ID
    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        User user = getLoggedInUser();
        Order order = orderService.getOrderById(id);

        if (order == null) {
            throw new RuntimeException("Order not found");
        }

        if ("ADMIN".equals(user.getRole())
                || "PHARMACY".equals(user.getRole())) {
            return order;
        }

        if ("DELIVERY_PARTNER".equals(user.getRole())) {
            DeliveryPartner partner = getDeliveryPartner(user);

            if (order.getDeliveryPartnerId() == null
                    || !order.getDeliveryPartnerId().equals(partner.getId())) {
                throw new RuntimeException(
                        "You can view only orders assigned to you");
            }

            return order;
        }

        if (!user.getId().equals(order.getUserId())) {
            throw new RuntimeException(
                    "You can view only your own orders");
        }

        return order;
    }

    // GET ORDERS BY USER
    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUser(@PathVariable Long userId) {
        User user = getLoggedInUser();

        if ("ADMIN".equals(user.getRole())
                || "PHARMACY".equals(user.getRole())) {
            return orderService.getOrdersByUser(userId);
        }

        if ("DELIVERY_PARTNER".equals(user.getRole())) {
            throw new RuntimeException(
                    "Delivery partners must use assigned orders");
        }

        if (!user.getId().equals(userId)) {
            throw new RuntimeException(
                    "You can view only your own orders");
        }

        return orderService.getOrdersByUser(userId);
    }

    // TRACK ORDER
    @GetMapping("/track/{trackingId}")
    public Order trackOrder(@PathVariable String trackingId) {
        User user = getLoggedInUser();

        Order matchingOrder = null;

        for (Order order : orderService.getAllOrders()) {
            if (order.getTrackingId() != null
                    && order.getTrackingId().equalsIgnoreCase(trackingId)) {
                matchingOrder = order;
                break;
            }
        }

        if (matchingOrder == null) {
            throw new RuntimeException("Tracking ID not found");
        }

        if ("ADMIN".equals(user.getRole())
                || "PHARMACY".equals(user.getRole())) {
            return matchingOrder;
        }

        if ("DELIVERY_PARTNER".equals(user.getRole())) {
            DeliveryPartner partner = getDeliveryPartner(user);

            if (matchingOrder.getDeliveryPartnerId() == null
                    || !matchingOrder.getDeliveryPartnerId()
                            .equals(partner.getId())) {
                throw new RuntimeException(
                        "You can track only orders assigned to you");
            }

            return matchingOrder;
        }

        if (!user.getId().equals(matchingOrder.getUserId())) {
            throw new RuntimeException(
                    "You can track only your own order");
        }

        return matchingOrder;
    }

    // ACCEPT OR UPDATE ORDER
    @PutMapping("/{id}/status")
    @Transactional
    public Order updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        User user = getLoggedInUser();
        String role = user.getRole();

        if (status == null || status.isBlank()) {
            throw new RuntimeException("Order status cannot be empty");
        }

        status = status.trim().toUpperCase();

        if (!"ADMIN".equals(role)
                && !"PHARMACY".equals(role)
                && !"DELIVERY_PARTNER".equals(role)) {
            throw new RuntimeException(
                    "You are not allowed to update this order");
        }

        // The repository must provide findByIdForUpdate().
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        boolean isAdmin = "ADMIN".equals(role) || "PHARMACY".equals(role);
        DeliveryPartner partner = null;

        // ACCEPT: PLACED -> PROCESSING
        if ("PROCESSING".equals(status)) {

            if (!"PLACED".equalsIgnoreCase(order.getStatus())) {
                throw new RuntimeException(
                        "Only a PLACED order can be accepted");
            }

            if (order.getDeliveryPartnerId() != null) {
                throw new RuntimeException(
                        "This order has already been assigned");
            }

            final DeliveryPartner acceptingPartner;

            if ("DELIVERY_PARTNER".equals(role)) {
                acceptingPartner = getDeliveryPartner(user);

                // Check availability using the repository,
                // avoiding a missing DeliveryPartner.getAvailable() method.
                final Long acceptingPartnerId = acceptingPartner.getId();

                boolean isAvailable = deliveryPartnerRepository
                        .findByAvailableTrue()
                        .stream()
                        .anyMatch(p -> p.getId().equals(acceptingPartnerId));

                if (!isAvailable) {
                    throw new RuntimeException(
                            "You are not currently available for deliveries");
                }

            } else {
                acceptingPartner = deliveryPartnerRepository
                        .findByAvailableTrue()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(
                                "No delivery partner is currently available"));
            }

            order.setDeliveryPartnerId(acceptingPartner.getId());
            order.setDeliveryPartner(acceptingPartner.getName());
            order.setDeliveryPhone(acceptingPartner.getPhone());

            acceptingPartner.setAvailable(false);
            deliveryPartnerRepository.save(acceptingPartner);
            orderRepository.save(order);

            return orderService.updateOrderStatus(id, status);
        }

        // LATER STATUS CHANGES
        if ("DELIVERY_PARTNER".equals(role)) {
            partner = getDeliveryPartner(user);

            if (order.getDeliveryPartnerId() == null
                    || !order.getDeliveryPartnerId().equals(partner.getId())) {
                throw new RuntimeException(
                        "This order is not assigned to you");
            }

            if (!"DISPATCHED".equals(status)
                    && !"DELIVERED".equals(status)) {
                throw new RuntimeException("Invalid delivery status");
            }

        } else {
            if (!"DISPATCHED".equals(status)
                    && !"DELIVERED".equals(status)) {
                throw new RuntimeException("Invalid order status");
            }

            // Assign an available partner when dispatching an older
            // order that has not yet been assigned.
            if ("DISPATCHED".equals(status)
                    && order.getDeliveryPartnerId() == null) {

                DeliveryPartner dispatchPartner = deliveryPartnerRepository
                        .findByAvailableTrue()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(
                                "No delivery partner is currently available"));

                order.setDeliveryPartnerId(dispatchPartner.getId());
                order.setDeliveryPartner(dispatchPartner.getName());
                order.setDeliveryPhone(dispatchPartner.getPhone());

                dispatchPartner.setAvailable(false);
                deliveryPartnerRepository.save(dispatchPartner);
                orderRepository.save(order);
            }
        }

        Order updatedOrder = orderService.updateOrderStatus(id, status);

        if ("DELIVERED".equals(status)
                && updatedOrder.getDeliveryPartnerId() != null) {

            DeliveryPartner assignedPartner =
                    deliveryPartnerRepository
                            .findById(updatedOrder.getDeliveryPartnerId())
                            .orElse(null);

            if (assignedPartner != null) {
                assignedPartner.setAvailable(true);
                deliveryPartnerRepository.save(assignedPartner);
            }
        }

        return updatedOrder;
    }

    // GET LOGGED-IN USER
    private User getLoggedInUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Authentication required");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // GET DELIVERY PARTNER PROFILE
    private DeliveryPartner getDeliveryPartner(User user) {
        return deliveryPartnerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "Delivery partner profile not found"));
    }
}
