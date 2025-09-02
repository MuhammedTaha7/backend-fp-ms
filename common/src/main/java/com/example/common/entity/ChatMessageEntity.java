package com.example.common.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessageEntity {

    @Id
    private String id; // Unique identifier for the message

    @NotBlank(message = "Sender ID cannot be blank")
    private String senderId; // ID of the message sender

    @NotBlank(message = "Receiver ID cannot be blank")
    private String receiverId; // ID of the message receiver

    @NotBlank(message = "Message content cannot be blank")
    private String content; // Message text

    @NotNull(message = "Timestamp cannot be null")
    private LocalDateTime timestamp; // When the message was sent

    @NotNull
    private boolean read = false; // Indicates if the message was read (default is false)

    // NEW FIELD: Context to distinguish between eduSphere and community chats
    @NotBlank(message = "Context cannot be blank")
    private String context; // "eduSphere" or "community"

    // Constructor without context (for backward compatibility)
    public ChatMessageEntity(String id, String senderId, String receiverId, String content, LocalDateTime timestamp, boolean read) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp;
        this.read = read;
        this.context = "eduSphere"; // Default to eduSphere for existing messages
    }
}