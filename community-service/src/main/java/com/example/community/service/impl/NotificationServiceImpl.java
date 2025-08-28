package com.example.community.service.impl;

import com.example.community.entity.Notification;
import com.example.community.repository.NotificationRepository;
import com.example.community.service.NotificationService;
import com.example.community.dto.NotificationDto;
import com.example.community.dto.request.SendNotificationRequest;
import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<NotificationDto> getUserNotifications(String userId) {
        List<Notification> notifications = notificationRepository
                .findByRecipientIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        return notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void sendNotification(SendNotificationRequest request, String senderId) {
        UserEntity recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        UserEntity sender = senderId != null ?
                userRepository.findById(senderId).orElse(null) : null;

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setSender(sender);
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setRelatedEntityId(request.getJobId());
        notification.setRelatedEntityType("JOB");
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }

    @Override
    public void markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public int getUnreadCount(String userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalseAndIsDeletedFalse(userId);
    }

    @Override
    public void markAllAsRead(String userId) {
        List<Notification> notifications = notificationRepository
                .findByRecipientIdAndIsReadFalseAndIsDeletedFalse(userId);

        notifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        });

        notificationRepository.saveAll(notifications);
    }

    @Override
    public void sendJobApplicationNotification(String recipientId, String type, String title, String message, String jobId) {
        UserEntity recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityId(jobId);
        notification.setRelatedEntityType("JOB");
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }

    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setRelatedEntityId(notification.getRelatedEntityId());
        dto.setRelatedEntityType(notification.getRelatedEntityType());

        if (notification.getSender() != null) {
            dto.setSenderName(notification.getSender().getName());
            dto.setSenderProfilePic(notification.getSender().getProfilePic());
        }

        return dto;
    }
}