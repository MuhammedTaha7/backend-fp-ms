package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationDto {
    private String id;
    private String type;
    private String title;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String relatedEntityId;
    private String relatedEntityType;
    private String senderName;
    private String senderProfilePic;
}