package com.example.community.repository;

import com.example.community.entity.Activity;
import com.example.common.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends MongoRepository<Activity, String> {

    // Find activities by user
    List<Activity> findByUserOrderByCreatedAtDesc(UserEntity user);

    // Find activities by multiple users (for friends feed)
    @Query("{'user.$id': {$in: ?0}}")
    List<Activity> findByUserIdInOrderByCreatedAtDesc(List<String> userIds);

    // Find activities by user and type
    List<Activity> findByUserAndTypeOrderByCreatedAtDesc(UserEntity user, String type);

    // Find recent activities (last 30 days)
    @Query("{'createdAt': {$gte: ?0}}")
    List<Activity> findRecentActivities(LocalDateTime since);

    // Find activities with pagination
    Page<Activity> findByUserInOrderByCreatedAtDesc(List<UserEntity> users, Pageable pageable);

    // Delete old activities (cleanup)
    void deleteByCreatedAtBefore(LocalDateTime before);
}