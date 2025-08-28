package com.example.community.repository;

import com.example.community.entity.JobPost;
import com.example.community.entity.SavedJob;
import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SavedJobRepository extends MongoRepository<SavedJob, String> {

    // Find saved jobs by user
    List<SavedJob> findByUserOrderBySavedAtDesc(UserEntity user);

    // Check if job is saved by user
    Optional<SavedJob> findByUserAndJobPost(UserEntity user, JobPost jobPost);

    // Delete saved job
    void deleteByUserAndJobPost(UserEntity user, JobPost jobPost);

    // Count saved jobs by user
    long countByUser(UserEntity user);

    // Find users who saved a job
    List<SavedJob> findByJobPost(JobPost jobPost);
}