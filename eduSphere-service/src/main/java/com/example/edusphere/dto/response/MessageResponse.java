package com.example.edusphere.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private String id;
    private String subject;
    private String content;
    private String senderId;
    private String senderName;
    private String recipientId;
    private String recipientName;
    private String priority;
    private String status;
    private LocalDateTime createdAt;
    private String replyContent;
    private LocalDateTime repliedAt;

}