package com.example.edusphere.repository;

import com.example.edusphere.entity.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    // Custom query to find all messages where the user is the recipient
    List<Message> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    // Custom query to find all messages where the user is the sender
    List<Message> findBySenderIdOrderByCreatedAtDesc(String senderId);

    // Custom query to find all messages for a specific sender and recipient
    List<Message> findBySenderIdAndRecipientIdOrderByCreatedAtDesc(String senderId, String recipientId);
}