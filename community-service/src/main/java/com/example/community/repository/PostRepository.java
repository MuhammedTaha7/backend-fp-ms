package com.example.community.repository;

import com.example.community.entity.Post;
import com.example.common.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {

    // Find posts by user
    List<Post> findByUserOrderByCreatedAtDesc(UserEntity user);

    // Find posts by multiple user IDs (for @DBRef) - CORRECTED QUERY
    @Query(value = "{'user.$id': {$in: ?0}}", sort = "{'createdAt': -1}")
    List<Post> findByUserIdInOrderByCreatedAtDesc(List<String> userIds);

    // Alternative method using ObjectId (if the above doesn't work)
    @Query(value = "{'user.$id': {$in: ?#{#userIds.![T(org.bson.types.ObjectId).new(#this)]}}}", sort = "{'createdAt': -1}")
    List<Post> findByUserObjectIdInOrderByCreatedAtDesc(List<String> userIds);

    // Find posts by UserEntity objects directly
    @Query(value = "{'user': {$in: ?0}}", sort = "{'createdAt': -1}")
    List<Post> findByUserInOrderByCreatedAtDesc(List<UserEntity> users);

    // Find community posts (not group posts)
    @Query(value = "{'groupId': {$exists: false}}", sort = "{'createdAt': -1}")
    List<Post> findCommunityPostsOrderByCreatedAtDesc();

    // Find group posts
    @Query(value = "{'groupId': ?0}", sort = "{'createdAt': -1}")
    List<Post> findByGroupIdOrderByCreatedAtDesc(String groupId);

    // Find posts by multiple groups
    @Query(value = "{'groupId': {$in: ?0}}", sort = "{'createdAt': -1}")
    List<Post> findByGroupIdInOrderByCreatedAtDesc(List<String> groupIds);

    // Find posts with pagination
    @Query(value = "{'user': {$in: ?0}}", sort = "{'createdAt': -1}")
    Page<Post> findByUserInOrderByCreatedAtDesc(List<UserEntity> users, Pageable pageable);

    // Find posts user liked
    @Query(value = "{'likes': ?0}", sort = "{'createdAt': -1}")
    List<Post> findPostsLikedByUser(String userId);

    // Search posts by content
    @Query(value = "{'desc': {$regex: ?0, $options: 'i'}}", sort = "{'createdAt': -1}")
    List<Post> searchByContent(String searchTerm);

    // Find posts with images
    @Query(value = "{'img': {$exists: true, $ne: null}}", sort = "{'createdAt': -1}")
    List<Post> findPostsWithImages();

    // Count posts by user
    long countByUser(UserEntity user);

    // Count posts in group
    long countByGroupId(String groupId);

}