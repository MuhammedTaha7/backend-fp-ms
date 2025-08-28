package com.example.edusphere.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
public class Message {

    @Id
    private String id;

    private String subject;
    private String content;

    // Sender Info
    private String senderId;
    private String senderName;

    // Recipient Info
    private String recipientId;
    private String recipientName;

    private String priority; // e.g., "high", "medium", "low"
    private String status; // e.g., "pending", "viewed", "replied"

    @CreatedDate
    private LocalDateTime createdAt;

    private String replyContent;
    private LocalDateTime repliedAt;
}