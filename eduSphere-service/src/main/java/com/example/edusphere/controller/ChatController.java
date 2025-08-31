package com.example.edusphere.controller;

import com.example.edusphere.dto.request.ChatMessage;
import com.example.edusphere.entity.ChatMessageEntity;
import com.example.edusphere.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.example.edusphere.dto.request.ChatRequest;
import com.example.edusphere.dto.response.ChatResponse;
import com.example.edusphere.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
@RequiredArgsConstructor
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final ChatService chatService;

    /**
     * New method for the chatbot API. Handles requests from the chat UI.
     * @param request The user's message.
     * @return A ResponseEntity containing the bot's response.
     */
    @PostMapping
    public ResponseEntity<ChatResponse> getBotResponse(@RequestBody ChatRequest request) {
        String botResponse = chatService.getBotResponse(request.getMessage());
        return ResponseEntity.ok(ChatResponse.builder().response(botResponse).build());
    }

    // TEST ENDPOINT
    @GetMapping("/test")
    public Map<String, Object> testConnection() {
        return Map.of(
                "status", "success",
                "message", "Chat API is working",
                "timestamp", LocalDateTime.now().toString()
        );
    }

    // EXISTING METHOD: EduSphere chat (backward compatibility)
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage message) {

        LocalDateTime now = LocalDateTime.now();
        ChatMessageEntity chatMessageEntity = new ChatMessageEntity(
                null,
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent(),
                now,
                false,
                "eduSphere"
        );

        ChatMessageEntity savedMessage = chatMessageRepository.save(chatMessageEntity);

        // Create response message with proper timestamp format
        ChatMessage responseMessage = new ChatMessage(
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent(),
                "eduSphere"
        );
        // Add timestamp in ISO format for frontend
        responseMessage.setTimestamp(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Send ONLY to receiver (not to sender to avoid duplicates)
        String receiverTopic = "/topic/messages/eduSphere/" + message.getReceiverId();
        messagingTemplate.convertAndSend(receiverTopic, responseMessage);
    }

    // NEW METHOD: Community chat
    @MessageMapping("/community.sendMessage")
    public void sendCommunityMessage(@Payload ChatMessage message) {

        LocalDateTime now = LocalDateTime.now();
        ChatMessageEntity chatMessageEntity = new ChatMessageEntity(
                null,
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent(),
                now,
                false,
                "community"
        );

        ChatMessageEntity savedMessage = chatMessageRepository.save(chatMessageEntity);

        // Create response message with proper timestamp format
        ChatMessage responseMessage = new ChatMessage(
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent(),
                "community"
        );
        // Add timestamp in ISO format for frontend
        responseMessage.setTimestamp(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Send ONLY to receiver (not to sender to avoid duplicates)
        String receiverTopic = "/topic/messages/community/" + message.getReceiverId();
        messagingTemplate.convertAndSend(receiverTopic, responseMessage);
    }

    // EXISTING METHOD: EduSphere chat history (backward compatibility)
    @GetMapping("/{user1}/{user2}")
    public List<ChatMessageEntity> getChatMessagesBetweenUsers(
            @PathVariable String user1,
            @PathVariable String user2
    ) {
        List<ChatMessageEntity> messages = chatMessageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdAndContext(
                user1, user2, "eduSphere"
        );
        return messages;
    }

    // NEW METHOD: Community chat history
    @GetMapping("/community/{user1}/{user2}")
    public List<ChatMessageEntity> getCommunityChatMessagesBetweenUsers(
            @PathVariable String user1,
            @PathVariable String user2
    ) {
        List<ChatMessageEntity> messages = chatMessageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdAndContext(
                user1, user2, "community"
        );
        return messages;
    }

    // NEW METHOD: Get chat conversations for a user in specific context
    @GetMapping("/conversations/{userId}")
    public List<ChatMessageEntity> getUserConversations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "eduSphere") String context
    ) {
        List<ChatMessageEntity> messages = chatMessageRepository.findRecentMessagesByUserIdAndContext(userId, context);
        return messages;
    }

    // NEW METHOD: Get unread message count
    @GetMapping("/unread/{userId}")
    public Map<String, Object> getUnreadCount(
            @PathVariable String userId,
            @RequestParam(defaultValue = "eduSphere") String context
    ) {
        long unreadCount = chatMessageRepository.countUnreadMessagesByUserIdAndContext(userId, context);

        return Map.of(
                "userId", userId,
                "context", context,
                "unreadCount", unreadCount
        );
    }

    // NEW METHOD: Mark messages as read
    @PutMapping("/mark-read")
    public Map<String, Object> markMessagesAsRead(
            @RequestBody Map<String, String> request
    ) {
        String receiverId = request.get("receiverId");
        String senderId = request.get("senderId");
        String context = request.getOrDefault("context", "eduSphere");

        List<ChatMessageEntity> unreadMessages = chatMessageRepository
                .findUnreadMessagesFromSenderInContext(receiverId, senderId, context);

        unreadMessages.forEach(message -> {
            message.setRead(true);
            chatMessageRepository.save(message);
        });

        return Map.of(
                "success", true,
                "markedAsRead", unreadMessages.size()
        );
    }

    // NEW METHOD: Get all contexts a user has chats in
    @GetMapping("/contexts/{userId}")
    public Map<String, Object> getUserChatContexts(@PathVariable String userId) {

        List<ChatMessageEntity> eduSphereMessages = chatMessageRepository
                .findAllByUserIdAndContext(userId, "eduSphere");
        List<ChatMessageEntity> communityMessages = chatMessageRepository
                .findAllByUserIdAndContext(userId, "community");

        return Map.of(
                "eduSphere", eduSphereMessages.size() > 0,
                "community", communityMessages.size() > 0,
                "eduSphereCount", eduSphereMessages.size(),
                "communityCount", communityMessages.size()
        );
    }
}