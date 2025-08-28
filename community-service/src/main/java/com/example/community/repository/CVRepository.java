package com.example.community.repository;

import com.example.community.entity.CV;
import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CVRepository extends MongoRepository<CV, String> {

    // Find CV by user entity
    Optional<CV> findByUser(UserEntity user);

    // Find CV by user ID (you'll need to add this method)
    Optional<CV> findByUserId(String userId);

    // Delete CV by user
    void deleteByUser(UserEntity user);

    // Delete CV by user ID
    void deleteByUserId(String userId);

    // Check if user has CV
    boolean existsByUser(UserEntity user);

    // Check if user has CV by ID
    boolean existsByUserId(String userId);
}