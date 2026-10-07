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

    // =========================================================
    // PLACE SINGLE ORDER
    // =========================================================

    @PostMapping
    public Order placeOrder(@RequestBody Order order) {
        return orderService.placeOrder(order);
    }

    // =========================================================
    // PLACE CART ORDERS
    // =========================================================

    @PostMapping("/cart")
    public List<Order> placeCartOrders(
            @RequestBody List<Order> orders) {

        return orderService.placeCartOrders(orders);
    }

    // =========================================================
    // GET ALL ORDERS
    // ADMIN + PHARMACY ONLY
    // =========================================================

    @GetMapping
    public List<Order> getAllOrders() {

        User user = getLoggedInUser();

        if (!user.getRole().equals("ADMIN") &&
                !user.getRole().equals("PHARMACY")) {

            throw new RuntimeException(
                    "You are not allowed to view all orders");
        }

        return orderService.getAllOrders();
    }

    // =========================================================
    // GET ORDER BY ID
    // =========================================================

    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {

        User loggedInUser = getLoggedInUser();

        Order order = orderService.getOrderById(id);

        if (order == null) {
            throw new RuntimeException("Order not found");
        }

        // ADMIN / PHARMACY
        if (loggedInUser.getRole().equals("ADMIN") ||
                loggedInUser.getRole().equals("PHARMACY")) {

            return order;
        }

        // DELIVERY PARTNER
        if (loggedInUser.getRole().equals("DELIVERY_PARTNER")) {

            DeliveryPartner partner =
                    getDeliveryPartner(loggedInUser);

            if (order.getDeliveryPartnerId() == null ||
                    !order.getDeliveryPartnerId()
                            .equals(partner.getId())) {

                throw new RuntimeException(
                        "You can view only orders assigned to you");
            }

            return order;
        }

        // PATIENT
        if (!loggedInUser.getId()
                .equals(order.getUserId())) {

            throw new RuntimeException(
                    "You can view only your own orders");
        }

        return order;
    }

    // =========================================================
    // GET ORDERS BY USER
    // =========================================================

    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUser(
            @PathVariable Long userId) {

        User loggedInUser = getLoggedInUser();

        // ADMIN / PHARMACY
        if (loggedInUser.getRole().equals("ADMIN") ||
                loggedInUser.getRole().equals("PHARMACY")) {

            return orderService.getOrdersByUser(userId);
        }

        // DELIVERY PARTNER
        if (loggedInUser.getRole().equals("DELIVERY_PARTNER")) {

            throw new RuntimeException(
                    "Delivery partners must use assigned orders");
        }

        // PATIENT
        if (!loggedInUser.getId().equals(userId)) {

            throw new RuntimeException(
                    "You can view only your own orders");
        }

        return orderService.getOrdersByUser(userId);
    }

    // =========================================================
    // TRACK ORDER BY TRACKING ID
    // =========================================================

    @GetMapping("/track/{trackingId}")
    public Order trackOrder(
            @PathVariable String trackingId) {

        User loggedInUser = getLoggedInUser();

        List<Order> allOrders =
                orderService.getAllOrders();

        Order matchingOrder = null;

        for (Order order : allOrders) {

            if (order.getTrackingId() != null &&
                    order.getTrackingId()
                            .equalsIgnoreCase(trackingId)) {

                matchingOrder = order;
                break;
            }
        }

        if (matchingOrder == null) {
            throw new RuntimeException(
                    "Tracking ID not found");
        }

        // ADMIN / PHARMACY
        if (loggedInUser.getRole().equals("ADMIN") ||
                loggedInUser.getRole().equals("PHARMACY")) {

            return matchingOrder;
        }

        // DELIVERY PARTNER
        if (loggedInUser.getRole().equals("DELIVERY_PARTNER")) {

            DeliveryPartner partner =
                    getDeliveryPartner(loggedInUser);

            if (matchingOrder.getDeliveryPartnerId() == null ||
                    !matchingOrder.getDeliveryPartnerId()
                            .equals(partner.getId())) {

                throw new RuntimeException(
                        "You can track only orders assigned to you");
            }

            return matchingOrder;
        }

        // PATIENT
        if (!loggedInUser.getId()
                .equals(matchingOrder.getUserId())) {

            throw new RuntimeException(
                    "You can track only your own order");
        }

        return matchingOrder;
    }

    // =========================================================
    // UPDATE ORDER STATUS
    //
    // PATIENT
    //    ↓
    // PLACED
    //    ↓
    // ADMIN / PHARMACY
    //    ↓
    // PROCESSING
    //    ↓
    // DISPATCH
    //    ↓
    // AUTO ASSIGN DELIVERY PARTNER
    //    ↓
    // DELIVERY PARTNER
    //    ↓
    // DELIVERED
    // =========================================================

    @PutMapping("/{id}/status")
    public Order updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        User loggedInUser = getLoggedInUser();

        String role = loggedInUser.getRole();

        // =====================================================
        // ADMIN / PHARMACY
        // =====================================================

        if (role.equals("ADMIN") ||
                role.equals("PHARMACY")) {

            /*
             * When ADMIN/PHARMACY dispatches the order,
             * automatically find an available delivery partner.
             */

            if (status.equalsIgnoreCase("DISPATCHED")) {

                Order order =
                        orderService.getOrderById(id);

                if (order == null) {

                    throw new RuntimeException(
                            "Order not found");
                }

                // Assign only when there is no partner yet.
                if (order.getDeliveryPartnerId() == null) {

                    DeliveryPartner partner =
                            deliveryPartnerRepository
                                    .findByAvailableTrue()
                                    .stream()
                                    .findFirst()
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "No delivery partner is currently available"));

                    // Assign partner
                    order.setDeliveryPartnerId(
                            partner.getId());

                    order.setDeliveryPartner(
                            partner.getName());

                    order.setDeliveryPhone(
                            partner.getPhone());

                    /*
                     * IMPORTANT:
                     * Save assignment BEFORE OrderService
                     * changes the order status.
                     */
                    orderRepository.save(order);

                    /*
                     * Partner is now carrying an active order.
                     */
                    partner.setAvailable(false);

                    deliveryPartnerRepository.save(
                            partner);
                }
            }

            /*
             * Now perform the normal status transition.
             */
            Order updatedOrder =
                    orderService.updateOrderStatus(
                            id,
                            status);

            /*
             * Short ETA for project demonstration.
             * Later this can be calculated from real GPS.
             */
            if (status.equalsIgnoreCase("DISPATCHED")) {

                updatedOrder.setEstimatedDelivery(
                        java.time.LocalDateTime.now()
                                .plusMinutes(10)
                                .toString());

                updatedOrder =
                        orderRepository.save(
                                updatedOrder);
            }

            return updatedOrder;
        }

        // =====================================================
        // DELIVERY PARTNER
        // =====================================================

        if (role.equals("DELIVERY_PARTNER")) {

            DeliveryPartner partner =
                    getDeliveryPartner(loggedInUser);

            Order order =
                    orderService.getOrderById(id);

            if (order == null) {

                throw new RuntimeException(
                        "Order not found");
            }

            /*
             * Delivery partner can only update
             * orders assigned to that partner.
             */

            if (order.getDeliveryPartnerId() == null ||
                    !order.getDeliveryPartnerId()
                            .equals(partner.getId())) {

                throw new RuntimeException(
                        "This order is not assigned to you");
            }

            /*
             * Delivery partner can only use
             * valid delivery statuses.
             */

            if (!status.equals("PROCESSING") &&
                    !status.equals("DISPATCHED") &&
                    !status.equals("DELIVERED")) {

                throw new RuntimeException(
                        "Invalid delivery status");
            }

            Order updatedOrder =
                    orderService.updateOrderStatus(
                            id,
                            status);

            /*
             * When delivered, make the partner available again.
             */

            if (status.equals("DELIVERED")) {

                partner.setAvailable(true);

                deliveryPartnerRepository.save(
                        partner);
            }

            return updatedOrder;
        }

        // =====================================================
        // OTHER ROLES
        // =====================================================

        throw new RuntimeException(
                "Only ADMIN, PHARMACY and assigned DELIVERY PARTNER can update order status");
    }

    // =========================================================
    // GET LOGGED-IN USER
    // =========================================================

    private User getLoggedInUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "Authentication required");
        }

        String email =
                authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"));
    }

    // =========================================================
    // GET DELIVERY PARTNER PROFILE
    // =========================================================

    private DeliveryPartner getDeliveryPartner(
            User user) {

        return deliveryPartnerRepository
                .findByEmail(user.getEmail())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Delivery partner profile not found"));
    }
}