package com.example.community.repository;

import com.example.community.entity.FriendRequest;
import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends MongoRepository<FriendRequest, String> {

    // Find requests by receiver ID and status
    @Query("{'receiver.$id': ?0, 'status': ?1}")
    List<FriendRequest> findByReceiverIdAndStatus(String receiverId, String status);

    // Original method using UserEntity
    List<FriendRequest> findByReceiverAndStatus(UserEntity receiver, String status);

    // Find ALL requests by receiver ID (any status)
    @Query("{'receiver.$id': ?0}")
    List<FriendRequest> findByReceiverId(String receiverId);

    // Original method using UserEntity
    List<FriendRequest> findByReceiver(UserEntity receiver);

    // Find requests by sender ID (all statuses)
    @Query("{'sender.$id': ?0}")
    List<FriendRequest> findBySenderId(String senderId);

    // Original method using UserEntity
    List<FriendRequest> findBySender(UserEntity sender);

    // Find requests by sender ID and status
    @Query("{'sender.$id': ?0, 'status': ?1}")
    List<FriendRequest> findBySenderIdAndStatus(String senderId, String status);

    // Original method using UserEntity
    List<FriendRequest> findBySenderAndStatus(UserEntity sender, String status);

    // Check if request exists between two users using String IDs (any direction)
    @Query("{'$or': [{'sender.$id': ?0, 'receiver.$id': ?1}, {'sender.$id': ?1, 'receiver.$id': ?0}]}")
    Optional<FriendRequest> findBetweenUserIds(String userId1, String userId2);

    // Original method using UserEntity
    @Query("{'$or': [{'sender': ?0, 'receiver': ?1}, {'sender': ?1, 'receiver': ?0}]}")
    Optional<FriendRequest> findBetweenUsers(UserEntity user1, UserEntity user2);

    // Find request between two users with specific status using String IDs
    @Query("{'$or': [{'sender.$id': ?0, 'receiver.$id': ?1}, {'sender.$id': ?1, 'receiver.$id': ?0}], 'status': ?2}")
    Optional<FriendRequest> findBetweenUserIdsWithStatus(String userId1, String userId2, String status);

    // Original method using UserEntity
    @Query("{'$or': [{'sender': ?0, 'receiver': ?1}, {'sender': ?1, 'receiver': ?0}], 'status': ?2}")
    Optional<FriendRequest> findBetweenUsersWithStatus(UserEntity user1, UserEntity user2, String status);

    // Count pending requests for a user using String ID
    @Query(value = "{'receiver.$id': ?0, 'status': 'PENDING'}", count = true)
    long countPendingRequestsByReceiverId(String receiverId);

    // Original method using UserEntity
    @Query(value = "{'receiver': ?0, 'status': 'PENDING'}", count = true)
    long countPendingRequestsByReceiver(UserEntity receiver);

    // Find all requests involving specific user IDs (for cleanup operations)
    @Query("{'$or': [{'sender.$id': {$in: ?0}}, {'receiver.$id': {$in: ?0}}]}")
    List<FriendRequest> findAllInvolvingUserIds(List<String> userIds);

    // Find all requests between two users (any status, any direction)
    @Query("{'$or': [{'sender.$id': ?0, 'receiver.$id': ?1}, {'sender.$id': ?1, 'receiver.$id': ?0}]}")
    List<FriendRequest> findAllBetweenUserIds(String userId1, String userId2);

    // REMOVED ALL DELETE QUERIES - We'll handle deletion in service layer using deleteAll() and deleteById()
}