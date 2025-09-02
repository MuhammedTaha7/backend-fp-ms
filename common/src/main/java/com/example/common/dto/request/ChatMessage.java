package com.example.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    @NotBlank(message = "Sender ID cannot be blank")
    private String senderId; // ID of the message sender

    @NotBlank(message = "Receiver ID cannot be blank")
    private String receiverId; // ID of the message receiver

    @NotBlank(message = "Message content cannot be blank")
    private String content; // Message content

    // Context for distinguishing between eduSphere and community
    private String context = "eduSphere"; // Default to eduSphere for backward compatibility

    // Timestamp field for frontend (ISO string format)
    private String timestamp;

    // Constructor without context and timestamp (for backward compatibility)
    public ChatMessage(String senderId, String receiverId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.context = "eduSphere";
    }

    // Constructor with context but without timestamp
    public ChatMessage(String senderId, String receiverId, String content, String context) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.context = context;
    }
}