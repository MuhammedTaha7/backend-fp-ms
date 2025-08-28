package com.example.edusphere.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RequestResponseDto {
    private String id;
    private String senderId;
    private String senderName; // Student's name
    private String receiverId;
    private String subject;
    private String message;
    private String type;
    private String priority;
    private String status; // 'pending', 'approved', 'rejected', 'responded'
    private String response;
    private LocalDateTime date;
    private LocalDateTime responseDate;
}