package com.example.community.repository;

import com.example.community.entity.Story;
import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends MongoRepository<Story, String> {

    // Find active stories (not expired)
    @Query("{'expiresAt': {$gt: ?0}}")
    List<Story> findActiveStories(LocalDateTime now);

    // Find stories by user
    List<Story> findByUserOrderByCreatedAtDesc(UserEntity user);

    // Find active stories by user - WORKS FINE
    @Query("{'user': ?0, 'expiresAt': {$gt: ?1}}")
    List<Story> findActiveStoriesByUser(UserEntity user, LocalDateTime now);

    // FIXED: Query by user.id field to work with @DBRef - try different syntax
    @Query("{'user.$id': {$in: ?0}, 'expiresAt': {$gt: ?1}}")
    List<Story> findActiveStoriesByUserIds(List<String> userIds, LocalDateTime now);

    // ALTERNATIVE: Use ObjectId conversion
    @Query("{'user': {$in: ?0}, 'expiresAt': {$gt: ?1}}")
    List<Story> findActiveStoriesByUserEntities(List<UserEntity> users, LocalDateTime now);

    // ALTERNATIVE: Get all active stories and filter in service
    @Query("{'expiresAt': {$gt: ?0}}")
    List<Story> findAllActiveStories(LocalDateTime now);

    // Delete expired stories
    @Query(value = "{'expiresAt': {$lt: ?0}}", delete = true)
    void deleteExpiredStories(LocalDateTime now);

    // Find stories by type
    List<Story> findByTypeAndExpiresAtAfterOrderByCreatedAtDesc(String type, LocalDateTime now);

    // Count active stories by user
    @Query(value = "{'user': ?0, 'expiresAt': {$gt: ?1}}", count = true)
    long countActiveStoriesByUser(UserEntity user, LocalDateTime now);
}