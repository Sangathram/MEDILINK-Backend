package com.medilink.controller;

import com.medilink.entity.Notification;
import com.medilink.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {

        this.notificationService = notificationService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getNotifications(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                notificationService.getUserNotifications(userId));
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable Long id) {

        notificationService.markAsRead(id);

        return ResponseEntity.ok(
                "Notification marked as read");
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<String> markAllAsRead(
            @PathVariable Long userId) {

        notificationService.markAllAsRead(userId);

        return ResponseEntity.ok(
                "All notifications marked as read");
    }
}