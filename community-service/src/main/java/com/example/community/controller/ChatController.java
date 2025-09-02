// community-service/src/main/java/com/example/community/controller/ChatController.java
package com.example.community.controller;

import com.example.common.dto.request.ChatMessage;
import com.example.common.entity.ChatMessageEntity;
import com.example.common.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // TEST ENDPOINT
    @GetMapping("/test")
    public Map<String, Object> testConnection() {
        return Map.of(
                "status", "success",
                "message", "Community Chat API is working",
                "timestamp", LocalDateTime.now().toString()
        );
    }

    // Handle community messages
    @MessageMapping("/community.sendMessage")
    public void sendCommunityMessage(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        Object user = headerAccessor.getUser();
        if (user == null) {
            System.err.println("⚠️ Unauthorized message attempt: " + message);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        ChatMessageEntity entity = new ChatMessageEntity(
                null,
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent(),
                now,
                false,
                "community"
        );

        chatMessageRepository.save(entity);

        // Add timestamp for frontend
        ChatMessage response = new ChatMessage(
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent(),
                "community"
        );
        response.setTimestamp(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        String topic = "/topic/messages/community/" + message.getReceiverId();
        messagingTemplate.convertAndSend(topic, response);
    }

    // Get community chat history
    @GetMapping("/community/{user1}/{user2}")
    public List<ChatMessageEntity> getCommunityChat(
            @PathVariable String user1,
            @PathVariable String user2
    ) {
        return chatMessageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdAndContext(
                user1, user2, "community"
        );
    }

    // Unread count
    @GetMapping("/unread/{userId}")
    public Map<String, Object> getUnreadCount(@PathVariable String userId) {
        long count = chatMessageRepository.countUnreadMessagesByUserIdAndContext(userId, "community");
        return Map.of("userId", userId, "context", "community", "unreadCount", count);
    }

    // Mark as read (optional)
    @PutMapping("/mark-read")
    public Map<String, Object> markAsRead(@RequestBody Map<String, String> req) {
        String receiverId = req.get("receiverId");
        String senderId = req.get("senderId");

        List<ChatMessageEntity> msgs = chatMessageRepository.findUnreadMessagesFromSenderInContext(
                receiverId, senderId, "community"
        );

        msgs.forEach(m -> {
            m.setRead(true);
            chatMessageRepository.save(m);
        });

        return Map.of("success", true, "marked", msgs.size());
    }
}