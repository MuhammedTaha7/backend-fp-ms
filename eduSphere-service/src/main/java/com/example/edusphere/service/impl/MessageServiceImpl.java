package com.example.edusphere.service.impl;

import com.example.edusphere.entity.Message;
import com.example.common.entity.UserEntity;
import com.example.edusphere.repository.MessageRepository;
import com.example.common.repository.UserRepository;
import com.example.edusphere.service.MessageService;
import com.example.edusphere.dto.request.MessageRequest;
import com.example.edusphere.dto.request.MessageReplyRequest;
import com.example.edusphere.dto.response.MessageResponse;
import com.example.common.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<MessageResponse> getReceivedMessages(String userId) {
        List<Message> messages = messageRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        return messages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageResponse> getSentMessages(String userId) {
        List<Message> messages = messageRepository.findBySenderIdOrderByCreatedAtDesc(userId);
        return messages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MessageResponse getMessageById(String messageId, String userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSenderId().equals(userId) && !message.getRecipientId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied");
        }

        if (message.getRecipientId().equals(userId) && "pending".equals(message.getStatus())) {
            message.setStatus("viewed");
            messageRepository.save(message);
        }

        return mapToResponse(message);
    }

    @Override
    public MessageResponse createMessage(MessageRequest messageRequest, String senderId, String senderName) {
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        UserEntity recipient = userRepository.findById(messageRequest.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

        // ---  Role-based message creation rules ---
        // Admin (1100) cannot send new requests
        if ("1100".equals(sender.getRole())) {
            throw new org.springframework.security.access.AccessDeniedException("Admins cannot create new requests.");
        }
        // Student (1300) can send to Admin or Lecturer
        else if ("1300".equals(sender.getRole())) {
            if (!("1100".equals(recipient.getRole()) || "1200".equals(recipient.getRole()))) {
                throw new org.springframework.security.access.AccessDeniedException("Students can only send messages to Admins or Lecturers.");
            }
        }
        // Lecturer (1200) can send to Admin or Lecturer
        else if ("1200".equals(sender.getRole())) {
            if (!("1100".equals(recipient.getRole()) || "1200".equals(recipient.getRole()))) {
                throw new org.springframework.security.access.AccessDeniedException("Lecturers can only send messages to other Lecturers or Admins.");
            }
        }

        Message newMessage = new Message();
        newMessage.setSubject(messageRequest.getSubject());
        newMessage.setContent(messageRequest.getContent());
        newMessage.setSenderId(senderId);
        newMessage.setSenderName(senderName);
        newMessage.setRecipientId(recipient.getId());
        newMessage.setRecipientName(recipient.getName());
        newMessage.setPriority(messageRequest.getPriority());
        newMessage.setStatus("pending");

        Message savedMessage = messageRepository.save(newMessage);
        return mapToResponse(savedMessage);
    }

    @Override
    public MessageResponse replyToMessage(String messageId, MessageReplyRequest replyRequest, String replierId) {
        Message originalMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        // Security check: only the original recipient can reply
        if (!originalMessage.getRecipientId().equals(replierId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the recipient can reply to this message");
        }

        // Get the replier's role
        UserEntity replier = userRepository.findById(replierId)
                .orElseThrow(() -> new ResourceNotFoundException("Replier not found"));

        // ---  Role-based reply rule ---
        // Only Admins and Lecturers can reply
        if (!("1100".equals(replier.getRole()) || "1200".equals(replier.getRole()))) {
            throw new org.springframework.security.access.AccessDeniedException("Students are not allowed to reply to messages.");
        }

        if ("replied".equals(originalMessage.getStatus())) {
            throw new IllegalStateException("This message has already been replied to");
        }

        originalMessage.setReplyContent(replyRequest.getReplyContent());
        originalMessage.setRepliedAt(LocalDateTime.now());
        originalMessage.setStatus("replied");

        Message updatedMessage = messageRepository.save(originalMessage);
        return mapToResponse(updatedMessage);
    }

    @Override
    public List<MessageResponse> getAllMessages() {
        List<Message> messages = messageRepository.findAll();
        return messages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private MessageResponse mapToResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSubject(),
                message.getContent(),
                message.getSenderId(),
                message.getSenderName(),
                message.getRecipientId(),
                message.getRecipientName(),
                message.getPriority(),
                message.getStatus(),
                message.getCreatedAt(),
                message.getReplyContent(),
                message.getRepliedAt()
        );
    }
}