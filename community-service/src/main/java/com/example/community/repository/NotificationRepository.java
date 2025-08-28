package com.example.community.repository;

import com.example.community.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByRecipientIdAndIsDeletedFalseOrderByCreatedAtDesc(String recipientId);
    List<Notification> findByRecipientIdAndIsReadFalseAndIsDeletedFalse(String recipientId);
    int countByRecipientIdAndIsReadFalseAndIsDeletedFalse(String recipientId);
}