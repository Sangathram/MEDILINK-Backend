package com.medilink.service;

import com.medilink.entity.Notification;
import com.medilink.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository) {

        this.notificationRepository = notificationRepository;
    }

    public Notification createNotification(
            Long userId,
            Long orderId,
            String title,
            String message,
            String type) {

        Notification notification = new Notification();

        notification.setUserId(userId);
        notification.setOrderId(orderId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReadStatus(false);

        return notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository
                .countByUserIdAndReadStatusFalse(userId);
    }

    public void markAsRead(Long id) {

        Notification notification =
                notificationRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"));

        notification.setReadStatus(true);

        notificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {

        List<Notification> notifications =
                notificationRepository
                        .findByUserIdOrderByCreatedAtDesc(userId);

        for (Notification notification : notifications) {
            notification.setReadStatus(true);
        }

        notificationRepository.saveAll(notifications);
    }
}