package com.example.community.repository;

import com.example.community.entity.Comment;
import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    // Find comments by post ID
    List<Comment> findByPostIdOrderByCreatedAtAsc(String postId);

    // Find comments by user
    List<Comment> findByUserOrderByCreatedAtDesc(UserEntity user);

    // Count comments for a post
    long countByPostId(String postId);

    // Delete all comments for a post
    void deleteByPostId(String postId);

    // Find comments by multiple post IDs
    List<Comment> findByPostIdIn(List<String> postIds);
}