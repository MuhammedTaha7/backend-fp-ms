package com.example.community.repository;

import com.example.community.entity.Friendship;
import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends MongoRepository<Friendship, String> {

    // Find all friendships for a user (any direction) - using String IDs for better reliability
    @Query("{'$or': [{'user1.$id': ?0}, {'user2.$id': ?0}]}")
    List<Friendship> findAllByUserId(String userId);

    // BACKUP: Original method using UserEntity (keep for compatibility)
    @Query("{'$or': [{'user1': ?0}, {'user2': ?0}]}")
    List<Friendship> findAllByUser(UserEntity user);

    // Check if friendship exists between two users using String IDs
    @Query("{'$or': [{'user1.$id': ?0, 'user2.$id': ?1}, {'user1.$id': ?1, 'user2.$id': ?0}]}")
    Optional<Friendship> findBetweenUserIds(String userId1, String userId2);

    // BACKUP: Original method using UserEntity (keep for compatibility)
    @Query("{'$or': [{'user1': ?0, 'user2': ?1}, {'user1': ?1, 'user2': ?0}]}")
    Optional<Friendship> findBetweenUsers(UserEntity user1, UserEntity user2);

    // Count friends for a user using String ID
    @Query(value = "{'$or': [{'user1.$id': ?0}, {'user2.$id': ?0}]}", count = true)
    long countFriendsByUserId(String userId);

    // Original count method using UserEntity
    @Query(value = "{'$or': [{'user1': ?0}, {'user2': ?0}]}", count = true)
    long countFriendsByUser(UserEntity user);

    // Find mutual friends using String IDs
    @Query("{'$or': [{'user1.$id': {$in: ?0}}, {'user2.$id': {$in: ?0}}]}")
    List<Friendship> findMutualFriendsByIds(List<String> friendIds);

    // Find mutual friends using UserEntity list
    @Query("{'$or': [{'user1': {$in: ?0}}, {'user2': {$in: ?0}}]}")
    List<Friendship> findMutualFriends(List<UserEntity> friendsOfUser1);

    // Find all friendships involving specific user IDs (for cleanup operations)
    @Query("{'$or': [{'user1.$id': {$in: ?0}}, {'user2.$id': {$in: ?0}}]}")
    List<Friendship> findAllInvolvingUserIds(List<String> userIds);

    // REMOVED ALL DELETE QUERIES - We'll handle deletion in service layer using deleteAll() and deleteById()
}