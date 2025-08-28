package com.example.edusphere.service;

import com.example.edusphere.entity.Message;
import com.example.edusphere.dto.request.MessageRequest;
import com.example.edusphere.dto.request.MessageReplyRequest;
import com.example.edusphere.dto.response.MessageResponse;

import java.util.List;

public interface MessageService {

    List<MessageResponse> getReceivedMessages(String userId);

    List<MessageResponse> getSentMessages(String userId);

    MessageResponse getMessageById(String messageId, String userId);

    MessageResponse createMessage(MessageRequest messageRequest, String senderId, String senderName);

    MessageResponse replyToMessage(String messageId, MessageReplyRequest replyRequest, String replierId);

    List<MessageResponse> getAllMessages();
}