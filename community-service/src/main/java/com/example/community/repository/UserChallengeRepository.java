package com.example.community.repository;

import com.example.community.entity.Challenge;
import com.example.community.entity.UserChallenge;
import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserChallengeRepository extends MongoRepository<UserChallenge, String> {

    // Find user's challenges
    List<UserChallenge> findByUserOrderByStartedAtDesc(UserEntity user);

    // Find user's challenges by status
    List<UserChallenge> findByUserAndStatusOrderByStartedAtDesc(UserEntity user, String status);

    // Check if user started challenge
    Optional<UserChallenge> findByUserAndChallenge(UserEntity user, Challenge challenge);

    // Find completed challenges by user
    List<UserChallenge> findByUserAndStatusOrderByCompletedAtDesc(UserEntity user, String status);

    // Count challenges by user and status
    long countByUserAndStatus(UserEntity user, String status);

    // Find users who completed a challenge
    List<UserChallenge> findByChallengeAndStatus(Challenge challenge, String status);

    // Calculate total points for user
    // Note: This would need to be implemented in service layer with aggregation
}