package com.medilink.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.medilink.entity.HealthcareProduct;
import com.medilink.entity.Medicine;
import com.medilink.entity.Order;
import com.medilink.entity.User;
import com.medilink.repository.HealthcareProductRepository;
import com.medilink.repository.MedicineRepository;
import com.medilink.repository.OrderRepository;
import com.medilink.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MedicineRepository medicineRepository;
    private final HealthcareProductRepository healthcareProductRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // =========================================================
    // DELIVERY CHARGE SETTINGS
    // =========================================================

    private static final double FREE_DELIVERY_LIMIT = 99.0;
    private static final double DELIVERY_CHARGE = 30.0;

    // =========================================================
    // DELIVERY TRACKING SETTINGS
    // =========================================================

    private static final DateTimeFormatter ESTIMATED_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");


    public OrderService(
            OrderRepository orderRepository,
            MedicineRepository medicineRepository,
            HealthcareProductRepository healthcareProductRepository,
            UserRepository userRepository,
            NotificationService notificationService) {

        this.orderRepository = orderRepository;
        this.medicineRepository = medicineRepository;
        this.healthcareProductRepository = healthcareProductRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
                    "User is not authenticated"
            );
        }

        String email = authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Logged-in user not found"
                        )
                );
    }


    // =========================================================
    // GENERATE UNIQUE TRACKING ID
    // =========================================================

    private String generateTrackingId() {

        String shortId = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();

        return "ML" + shortId;
    }


    // =========================================================
    // SET INITIAL TRACKING INFORMATION
    // =========================================================

    private void initializeTracking(Order order) {

        order.setTrackingId(generateTrackingId());

        order.setDeliveryStatus("ORDER_PLACED");

        order.setDeliveryPartner(null);

        order.setDeliveryPhone(null);

        LocalDateTime estimatedTime =
                LocalDateTime.now().plusMinutes(10);

        order.setEstimatedDelivery(
                estimatedTime.format(ESTIMATED_DATE_FORMAT)
        );

        order.setPackedAt(null);

        order.setDispatchedAt(null);

        order.setDeliveredAt(null);
    }


    // =========================================================
    // PLACE SINGLE ORDER
    // =========================================================

    @Transactional
    public Order placeOrder(Order order) {

        User loggedInUser = getLoggedInUser();

        // -----------------------------------------------------
        // Security check
        // -----------------------------------------------------

        if (!loggedInUser.getId().equals(order.getUserId())) {

            throw new RuntimeException(
                    "You can place an order only for your own account"
            );
        }

        // -----------------------------------------------------
        // Validate quantity
        // -----------------------------------------------------

        if (order.getQuantity() == null ||
                order.getQuantity() <= 0) {

            throw new RuntimeException(
                    "Quantity must be greater than 0"
            );
        }

        // -----------------------------------------------------
        // HEALTHCARE PRODUCT ORDER
        // -----------------------------------------------------

        if ("HEALTHCARE".equalsIgnoreCase(order.getProductType())
                || order.getHealthcareProductId() != null) {

            if (order.getHealthcareProductId() == null) {
                throw new RuntimeException(
                        "Healthcare product ID is required"
                );
            }

            HealthcareProduct product = healthcareProductRepository
                    .findById(order.getHealthcareProductId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Healthcare product not found"
                            )
                    );

            if (product.getStock() == null ||
                    product.getStock() < order.getQuantity()) {

                throw new RuntimeException(
                        "Insufficient stock for " + product.getName()
                );
            }

            if (product.getPrice() == null) {
                throw new RuntimeException(
                        "Healthcare product price is not available for "
                                + product.getName()
                );
            }

            double productSubtotal =
                    product.getPrice() * order.getQuantity();

            double deliveryCharge =
                    productSubtotal >= FREE_DELIVERY_LIMIT
                            ? 0.0
                            : DELIVERY_CHARGE;

            order.setMedicineId(null);
            order.setProductType("HEALTHCARE");
            order.setTotalPrice(productSubtotal + deliveryCharge);
            order.setStatus("PLACED");

            initializeTracking(order);

            product.setStock(
                    product.getStock() - order.getQuantity()
            );

            healthcareProductRepository.save(product);

            Order savedOrder = orderRepository.save(order);

            notificationService.createNotification(
                    loggedInUser.getId(),
                    savedOrder.getId(),
                    "Order Placed",
                    "Your healthcare product order #" +
                            savedOrder.getId() +
                            " has been placed successfully.",
                    "ORDER_PLACED"
            );

            return savedOrder;
        }

        // -----------------------------------------------------
        // MEDICINE ORDER
        // -----------------------------------------------------

        if (order.getMedicineId() == null) {
            throw new RuntimeException(
                    "Medicine ID is required"
            );
        }

        Medicine medicine = medicineRepository
                .findById(order.getMedicineId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Medicine not found"
                        )
                );

        if (medicine.getStock() == null ||
                medicine.getStock() < order.getQuantity()) {

            throw new RuntimeException(
                    "Insufficient stock"
            );
        }

        if (medicine.getPrice() == null) {

            throw new RuntimeException(
                    "Medicine price is not available"
            );
        }

        double medicineSubtotal =
                medicine.getPrice() * order.getQuantity();

        double deliveryCharge =
                medicineSubtotal >= FREE_DELIVERY_LIMIT
                        ? 0.0
                        : DELIVERY_CHARGE;

        order.setHealthcareProductId(null);
        order.setProductType("MEDICINE");
        order.setTotalPrice(
                medicineSubtotal + deliveryCharge
        );
        order.setStatus("PLACED");

        initializeTracking(order);

        medicine.setStock(
                medicine.getStock() - order.getQuantity()
        );

        medicineRepository.save(medicine);

        Order savedOrder = orderRepository.save(order);

        notificationService.createNotification(
                loggedInUser.getId(),
                savedOrder.getId(),
                "Order Placed",
                "Your medicine order #" +
                        savedOrder.getId() +
                        " has been placed successfully.",
                "ORDER_PLACED"
        );

        return savedOrder;
    }

    // =========================================================
    // PLACE CART ORDERS
    //
    // Delivery charge is applied ONLY ONCE to the whole cart.
    // =========================================================

    @Transactional
    public List<Order> placeCartOrders(List<Order> cartOrders) {

        // -----------------------------------------------------
        // Validate cart
        // -----------------------------------------------------

        if (cartOrders == null ||
                cartOrders.isEmpty()) {

            throw new RuntimeException(
                    "Cart cannot be empty"
            );
        }

        User loggedInUser = getLoggedInUser();

        // -----------------------------------------------------
        // Store validated items
        // -----------------------------------------------------

        List<Medicine> medicines = new ArrayList<>();
        List<HealthcareProduct> healthcareProducts = new ArrayList<>();

        double cartSubtotal = 0.0;

        // =====================================================
        // FIRST PASS
        // Validate everything before changing stock
        // =====================================================

        for (Order order : cartOrders) {

            if (!loggedInUser.getId().equals(order.getUserId())) {

                throw new RuntimeException(
                        "You can place orders only for your own account"
                );
            }

            if (order.getQuantity() == null ||
                    order.getQuantity() <= 0) {

                throw new RuntimeException(
                        "Quantity must be greater than 0"
                );
            }

            boolean isHealthcare =
                    "HEALTHCARE".equalsIgnoreCase(order.getProductType())
                    || order.getHealthcareProductId() != null;

            if (isHealthcare) {

                if (order.getHealthcareProductId() == null) {
                    throw new RuntimeException(
                            "Healthcare product ID is required"
                    );
                }

                HealthcareProduct product =
                        healthcareProductRepository
                                .findById(order.getHealthcareProductId())
                                .orElseThrow(() ->
                                        new RuntimeException(
                                                "Healthcare product not found: "
                                                        + order.getHealthcareProductId()
                                        )
                                );

                if (product.getStock() == null ||
                        product.getStock() < order.getQuantity()) {

                    throw new RuntimeException(
                            "Insufficient stock for " +
                                    product.getName()
                    );
                }

                if (product.getPrice() == null) {
                    throw new RuntimeException(
                            "Healthcare product price is not available for " +
                                    product.getName()
                    );
                }

                cartSubtotal +=
                        product.getPrice() * order.getQuantity();

                healthcareProducts.add(product);
                medicines.add(null);

            } else {

                if (order.getMedicineId() == null) {
                    throw new RuntimeException(
                            "Medicine ID is required"
                    );
                }

                Medicine medicine = medicineRepository
                        .findById(order.getMedicineId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Medicine not found: " +
                                                order.getMedicineId()
                                )
                        );

                if (medicine.getStock() == null ||
                        medicine.getStock() < order.getQuantity()) {

                    throw new RuntimeException(
                            "Insufficient stock for " +
                                    medicine.getName()
                    );
                }

                if (medicine.getPrice() == null) {
                    throw new RuntimeException(
                            "Medicine price is not available for " +
                                    medicine.getName()
                    );
                }

                cartSubtotal +=
                        medicine.getPrice() * order.getQuantity();

                medicines.add(medicine);
                healthcareProducts.add(null);
            }
        }

        // =====================================================
        // DELIVERY CHARGE ONCE FOR WHOLE CART
        // =====================================================

        double deliveryCharge =
                cartSubtotal >= FREE_DELIVERY_LIMIT
                        ? 0.0
                        : DELIVERY_CHARGE;

        // =====================================================
        // SECOND PASS
        // Create all orders
        // =====================================================

        List<Order> savedOrders = new ArrayList<>();

        for (int i = 0; i < cartOrders.size(); i++) {

            Order order = cartOrders.get(i);

            Medicine medicine = medicines.get(i);
            HealthcareProduct healthcareProduct =
                    healthcareProducts.get(i);

            double itemSubtotal;

            if (healthcareProduct != null) {

                itemSubtotal =
                        healthcareProduct.getPrice() *
                                order.getQuantity();

                order.setMedicineId(null);
                order.setHealthcareProductId(
                        healthcareProduct.getId()
                );
                order.setProductType("HEALTHCARE");

            } else {

                itemSubtotal =
                        medicine.getPrice() *
                                order.getQuantity();

                order.setHealthcareProductId(null);
                order.setMedicineId(medicine.getId());
                order.setProductType("MEDICINE");
            }

            // Apply delivery charge only once to first order
            double orderTotal = itemSubtotal;

            if (i == 0) {
                orderTotal += deliveryCharge;
            }

            order.setTotalPrice(orderTotal);
            order.setStatus("PLACED");

            initializeTracking(order);

            // Reduce correct stock
            if (healthcareProduct != null) {

                healthcareProduct.setStock(
                        healthcareProduct.getStock() -
                                order.getQuantity()
                );

                healthcareProductRepository.save(
                        healthcareProduct
                );

            } else {

                medicine.setStock(
                        medicine.getStock() -
                                order.getQuantity()
                );

                medicineRepository.save(medicine);
            }

            Order savedOrder =
                    orderRepository.save(order);

            savedOrders.add(savedOrder);
        }

        // =====================================================
        // CREATE ORDER-PLACED NOTIFICATIONS
        // =====================================================

        for (Order savedOrder : savedOrders) {

            String productType =
                    "HEALTHCARE".equalsIgnoreCase(
                            savedOrder.getProductType())
                            ? "healthcare product"
                            : "medicine";

            notificationService.createNotification(
                    loggedInUser.getId(),
                    savedOrder.getId(),
                    "Order Placed",
                    "Your " + productType +
                            " order #" +
                            savedOrder.getId() +
                            " has been placed successfully.",
                    "ORDER_PLACED"
            );
        }

        return savedOrders;
    }

    // =========================================================
    // GET ALL ORDERS
    // =========================================================

    public List<Order> getAllOrders() {

        return orderRepository.findAll();
    }


    // =========================================================
    // GET ORDERS BY USER
    // =========================================================

    public List<Order> getOrdersByUser(Long userId) {

        return orderRepository.findByUserId(userId);
    }


    // =========================================================
    // GET ORDER BY ID
    // =========================================================

    public Order getOrderById(Long id) {

        return orderRepository
                .findById(id)
                .orElse(null);
    }


    // =========================================================
    // UPDATE ORDER STATUS
    // =========================================================

    @Transactional
    public Order updateOrderStatus(
            Long id,
            String newStatus) {

        // -----------------------------------------------------
        // Find order
        // -----------------------------------------------------

        Order order = orderRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found"
                        )
                );


        // -----------------------------------------------------
        // Check status input
        // -----------------------------------------------------

        if (newStatus == null ||
                newStatus.isBlank()) {

            throw new RuntimeException(
                    "Order status cannot be empty"
            );
        }


        // Convert status to uppercase
        newStatus = newStatus.toUpperCase();


        // Current status
        String currentStatus = order.getStatus();


        // =====================================================
        // VALIDATE STATUS TRANSITION
        // =====================================================

        if (currentStatus.equals("PLACED")) {

            if (!newStatus.equals("PROCESSING")) {

                throw new RuntimeException(
                        "PLACED order can only move to PROCESSING"
                );
            }
        }

        else if (currentStatus.equals("PROCESSING")) {

            if (!newStatus.equals("DISPATCHED")) {

                throw new RuntimeException(
                        "PROCESSING order can only move to DISPATCHED"
                );
            }
        }

        else if (currentStatus.equals("DISPATCHED")) {

            if (!newStatus.equals("DELIVERED")) {

                throw new RuntimeException(
                        "DISPATCHED order can only move to DELIVERED"
                );
            }
        }

        else if (currentStatus.equals("DELIVERED")) {

            throw new RuntimeException(
                    "DELIVERED order cannot be changed"
            );
        }

        else {

            throw new RuntimeException(
                    "Invalid current order status"
            );
        }


        // =====================================================
        // UPDATE MAIN ORDER STATUS
        // =====================================================

        order.setStatus(newStatus);


        // =====================================================
        // UPDATE DELIVERY TRACKING
        // =====================================================

        if (newStatus.equals("PROCESSING")) {

            order.setDeliveryStatus("PROCESSING");

        }

        else if (newStatus.equals("DISPATCHED")) {

            order.setDeliveryStatus("OUT_FOR_DELIVERY");

            order.setDispatchedAt(
                    LocalDateTime.now()
            );

            // Demo delivery partner information.
            // Later we can create a separate delivery-partner
            // module and assign real delivery partners.

            if (order.getDeliveryPartner() == null ||
                    order.getDeliveryPartner().isBlank()) {

                order.setDeliveryPartner(
                        "MEDILINK Delivery Partner"
                );
            }

            if (order.getDeliveryPhone() == null ||
                    order.getDeliveryPhone().isBlank()) {

                order.setDeliveryPhone(
                        "Delivery contact available"
                );
            }
        }

        else if (newStatus.equals("DELIVERED")) {

            order.setDeliveryStatus("DELIVERED");

            order.setDeliveredAt(
                    LocalDateTime.now()
            );
        }


        // -----------------------------------------------------
        // Save order
        // -----------------------------------------------------

        Order savedOrder = orderRepository.save(order);

        // =====================================================
        // CREATE STATUS NOTIFICATION
        // =====================================================

        String title;
        String message;

        if (newStatus.equals("PROCESSING")) {

            title = "Order Processing";
            message = "Your order #" +
                    savedOrder.getId() +
                    " is now being processed.";

        } else if (newStatus.equals("DISPATCHED")) {

            title = "Order Dispatched";
            message = "Your order #" +
                    savedOrder.getId() +
                    " has been dispatched and is out for delivery.";

        } else if (newStatus.equals("DELIVERED")) {

            title = "Order Delivered";
            message = "Your order #" +
                    savedOrder.getId() +
                    " has been delivered successfully.";

        } else {

            title = "Order Updated";
            message = "Your order #" +
                    savedOrder.getId() +
                    " status is now " +
                    newStatus + ".";
        }

        notificationService.createNotification(
                savedOrder.getUserId(),
                savedOrder.getId(),
                title,
                message,
                "ORDER_" + newStatus
        );

        return savedOrder;
    }
}