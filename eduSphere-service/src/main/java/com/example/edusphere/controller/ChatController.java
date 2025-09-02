package com.example.edusphere.controller;

import com.example.common.dto.request.ChatMessage;
import com.example.common.dto.request.ChatRequest;
import com.example.common.dto.response.ChatResponse;
import com.example.common.entity.ChatMessageEntity;
import com.example.common.repository.ChatMessageRepository;
import com.example.edusphere.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final ChatService chatService;

    /**
     * Chatbot endpoint
     */
    @PostMapping
    public ResponseEntity<ChatResponse> getBotResponse(@RequestBody ChatRequest request) {
        String botResponse = chatService.getBotResponse(request.getMessage());
        return ResponseEntity.ok(ChatResponse.builder().response(botResponse).build());
    }

    /**
     * Test endpoint
     */
    @GetMapping("/test")
    public Map<String, Object> test() {
        return Map.of(
                "status", "success",
                "message", "EduSphere Chat API is working",
                "timestamp", LocalDateTime.now().toString()
        );
    }

    /**
     * Handle EduSphere direct messages
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        Object user = headerAccessor.getUser();
        if (user == null) {
            System.err.println("⚠️ Unauthorized EduSphere message attempt");
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
                "eduSphere"
        );

        chatMessageRepository.save(entity);

        ChatMessage response = new ChatMessage(
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent(),
                "eduSphere"
        );
        response.setTimestamp(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        String topic = "/topic/messages/eduSphere/" + message.getReceiverId();
        messagingTemplate.convertAndSend(topic, response);
    }

    /**
     * Get EduSphere chat history
     */
    @GetMapping("/{user1}/{user2}")
    public List<ChatMessageEntity> getEduSphereChat(
            @PathVariable String user1,
            @PathVariable String user2
    ) {
        return chatMessageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdAndContext(
                user1, user2, "eduSphere"
        );
    }

}