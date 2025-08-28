package com.example.community.repository;

import com.example.community.entity.Challenge;
import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChallengeRepository extends MongoRepository<Challenge, String> {

    // Find challenges by category
    List<Challenge> findByCategoryOrderByCreatedAtDesc(String category);

    // Find challenges by difficulty
    List<Challenge> findByDifficultyOrderByPointsDesc(String difficulty);

    // Find challenges by type
    List<Challenge> findByTypeOrderByCreatedAtDesc(String type);

    // Find challenges by creator
    List<Challenge> findByCreatorOrderByCreatedAtDesc(UserEntity creator);

    // Search challenges by title
    @Query("{'title': {$regex: ?0, $options: 'i'}}")
    List<Challenge> searchByTitle(String title);

    // Find challenges with filters
    @Query("{'$and': [" +
            "{'$or': [{category: ?0}, {?0: {$exists: false}}]}," +
            "{'$or': [{difficulty: ?1}, {?1: {$exists: false}}]}," +
            "{'$or': [{type: ?2}, {?2: {$exists: false}}]}" +
            "]}")
    List<Challenge> findWithFilters(String category, String difficulty, String type);

    // Count challenges by category
    long countByCategory(String category);
}