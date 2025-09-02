package com.example.community.controller;

import com.example.community.entity.Notification;
import com.example.community.service.NotificationService;
import com.example.common.service.UserService;
import com.example.community.dto.NotificationDto;
import com.example.community.dto.request.SendNotificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        List<NotificationDto> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/send")
    public ResponseEntity<Void> sendNotification(
            @RequestBody SendNotificationRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String senderId = userService.getUserByUsername(username).getId();
        notificationService.sendNotification(request, senderId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String notificationId,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getUnreadCount(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        int count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}