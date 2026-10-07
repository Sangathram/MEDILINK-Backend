package com.medilink.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medilink.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByDeliveryPartnerIdOrderByIdDesc(Long deliveryPartnerId);

}
