package com.example.community.repository;

import com.example.community.entity.JobPost;
import com.example.common.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface JobPostRepository extends MongoRepository<JobPost, String> {

    // Find jobs by poster
    List<JobPost> findByPosterOrderByCreatedAtDesc(UserEntity poster);

    // Find active jobs
    List<JobPost> findByStatusOrderByCreatedAtDesc(String status);

    // Find jobs by type
    List<JobPost> findByTypeAndStatusOrderByCreatedAtDesc(String type, String status);

    // Search jobs by title
    @Query("{'title': {$regex: ?0, $options: 'i'}, 'status': 'Active'}")
    List<JobPost> searchByTitle(String title);

    // Find jobs by location
    @Query("{'location': {$regex: ?0, $options: 'i'}, 'status': 'Active'}")
    List<JobPost> findByLocationContaining(String location);

    // Find jobs with specific tags/skills
    @Query("{'tags': {$in: ?0}, 'status': 'Active'}")
    List<JobPost> findByTagsIn(List<String> tags);

    // Find jobs with filters
    @Query("{'status': 'Active', $and: [" +
            "{'$or': [{title: {$regex: ?0, $options: 'i'}}, {?0: {$exists: false}}]}," +
            "{'$or': [{type: ?1}, {?1: {$exists: false}}]}," +
            "{'$or': [{location: {$regex: ?2, $options: 'i'}}, {?2: {$exists: false}}]}" +
            "]}")
    Page<JobPost> findWithFilters(String searchTerm, String type, String location, Pageable pageable);

    // Find jobs by remote option
    List<JobPost> findByRemoteAndStatusOrderByCreatedAtDesc(String remote, String status);

    // Find expiring jobs
    @Query("{'deadline': {$lte: ?0}, 'status': 'Active'}")
    List<JobPost> findExpiringJobs(LocalDate date);

    // Count jobs by poster
    long countByPosterAndStatus(UserEntity poster, String status);
}