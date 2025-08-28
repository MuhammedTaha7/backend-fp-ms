package com.example.edusphere.repository;

import com.example.edusphere.entity.Announcement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends MongoRepository<Announcement, String> {

    List<Announcement> findByCreatorIdOrderByCreatedAtDesc(String creatorId);

    List<Announcement> findByTargetUserId(String targetUserId);

    List<Announcement> findByTargetAudienceType(String targetAudienceType);

    List<Announcement> findByTargetAudienceTypeAndTargetCourseIdIn(String targetAudienceType, List<String> targetCourseIds);
}