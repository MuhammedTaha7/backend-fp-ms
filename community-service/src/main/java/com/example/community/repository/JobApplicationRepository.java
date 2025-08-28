package com.example.community.repository;

import com.example.community.entity.JobApplication;
import com.example.community.entity.JobPost;
import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {

    // Find applications by job
    List<JobApplication> findByJobPostOrderByAppliedAtDesc(JobPost jobPost);

    // Find applications by user
    List<JobApplication> findByApplicantOrderByAppliedAtDesc(UserEntity applicant);

    // Check if user applied to job
    Optional<JobApplication> findByJobPostAndApplicant(JobPost jobPost, UserEntity applicant);

    // Find applications by status
    List<JobApplication> findByJobPostAndStatus(JobPost jobPost, String status);

    // Count applications by user
    long countByApplicant(UserEntity applicant);

    // Find applications by multiple jobs (for job poster dashboard)
    List<JobApplication> findByJobPostInOrderByAppliedAtDesc(List<JobPost> jobPosts);

    boolean existsByJobPostIdAndApplicantId(String jobPostId, String applicantId);

    /**
     * Find all applications for a specific job
     */
    List<JobApplication> findByJobPost(JobPost jobPost);

    /**
     * Check if an applicant has applied to any job posted by a specific employer
     * This is used for security checks when downloading CVs
     */
    @Query("{'applicant.id': ?0, 'jobPost.postedBy.id': ?1}")
    boolean existsByApplicantIdAndJobPostPostedById(String applicantId, String employerId);

    /**
     * Find all applications by a specific applicant to jobs posted by a specific employer
     * Useful for additional security verification
     */
    @Query("{'applicant.id': ?0, 'jobPost.postedBy.id': ?1}")
    List<JobApplication> findByApplicantIdAndJobPostPostedById(String applicantId, String employerId);

    /**
     * Find all applications for jobs posted by a specific employer
     * Useful for employer dashboard
     */
    @Query("{'jobPost.postedBy.id': ?0}")
    List<JobApplication> findByJobPostPostedById(String employerId);

    /**
     * Count applications for a specific job
     */
    long countByJobPost(JobPost jobPost);

    /**
     * Find applications by status for a specific job
     */
    @Query("{'jobPost.id': ?0, 'status': ?1}")
    List<JobApplication> findByJobPostIdAndStatus(String jobPostId, String status);

    /**
     * Find applications by applicant
     */
    List<JobApplication> findByApplicant(UserEntity applicant);

    /**
     * Check if user has applied to a specific job
     */
    boolean existsByJobPostAndApplicant(JobPost jobPost, UserEntity applicant);


    /**
     * Find applications by job post ID
     */
    @Query("{'jobPost.id': ?0}")
    List<JobApplication> findByJobPostId(String jobPostId);

    /**
     * Find applications by status
     */
    List<JobApplication> findByStatus(String status);

}