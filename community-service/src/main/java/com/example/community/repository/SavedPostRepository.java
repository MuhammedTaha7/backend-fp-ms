package com.example.community.repository;

import com.example.community.entity.Post;
import com.example.community.entity.SavedPost;
import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPostRepository extends MongoRepository<SavedPost, String> {

    // Find saved posts by user
    List<SavedPost> findByUserOrderBySavedAtDesc(UserEntity user);

    // Check if post is saved by user
    Optional<SavedPost> findByUserAndPost(UserEntity user, Post post);

    // Delete saved post
    void deleteByUserAndPost(UserEntity user, Post post);

    // Count saved posts by user
    long countByUser(UserEntity user);

    // Find users who saved a post
    List<SavedPost> findByPost(Post post);
}