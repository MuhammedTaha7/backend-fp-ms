package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendNotificationRequest {
    private String recipientId;
    private String type;
    private String title;
    private String message;
    private String jobId;
}