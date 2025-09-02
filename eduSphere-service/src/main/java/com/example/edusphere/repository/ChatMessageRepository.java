package com.example.edusphere.repository;

import com.example.edusphere.entity.ChatMessageEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessageEntity, String> {

    // Original method (for backward compatibility)
    List<ChatMessageEntity> findBySenderIdAndReceiverIdOrReceiverIdAndSenderId(
            String senderId1, String receiverId1, String senderId2, String receiverId2
    );

    //  Context-aware method for specific chat context
    @Query("{ '$and': [ " +
            "  { '$or': [ " +
            "    { 'senderId': ?0, 'receiverId': ?1 }, " +
            "    { 'senderId': ?1, 'receiverId': ?0 } " +
            "  ] }, " +
            "  { 'context': ?2 } " +
            "] }")
    List<ChatMessageEntity> findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdAndContext(
            String userId1, String userId2, String context
    );

    //  Get all messages for a user in a specific context
    @Query("{ '$and': [ " +
            "  { '$or': [ " +
            "    { 'senderId': ?0 }, " +
            "    { 'receiverId': ?0 } " +
            "  ] }, " +
            "  { 'context': ?1 } " +
            "] }")
    List<ChatMessageEntity> findAllByUserIdAndContext(String userId, String context);

    //  Get unread messages for a user in a specific context
    @Query("{ '$and': [ " +
            "  { 'receiverId': ?0 }, " +
            "  { 'read': false }, " +
            "  { 'context': ?1 } " +
            "] }")
    List<ChatMessageEntity> findUnreadMessagesByUserIdAndContext(String userId, String context);

    //  Count unread messages for a user in a specific context
    @Query(value = "{ '$and': [ " +
            "  { 'receiverId': ?0 }, " +
            "  { 'read': false }, " +
            "  { 'context': ?1 } " +
            "] }",
            count = true)
    long countUnreadMessagesByUserIdAndContext(String userId, String context);

    //  Get recent conversations for a user in a specific context
    @Query(value = "{ '$and': [ " +
            "  { '$or': [ " +
            "    { 'senderId': ?0 }, " +
            "    { 'receiverId': ?0 } " +
            "  ] }, " +
            "  { 'context': ?1 } " +
            "] }",
            sort = "{ 'timestamp': -1 }")
    List<ChatMessageEntity> findRecentMessagesByUserIdAndContext(String userId, String context);

    //  Mark messages as read between two users in a specific context
    @Query("{ '$and': [ " +
            "  { 'senderId': ?1, 'receiverId': ?0 }, " +
            "  { 'context': ?2 } " +
            "] }")
    List<ChatMessageEntity> findUnreadMessagesFromSenderInContext(String receiverId, String senderId, String context);
}