package com.example.community.repository;

import com.example.community.entity.Group;
import com.example.common.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroupRepository extends MongoRepository<Group, String> {

    // Find groups by founder
    List<Group> findByFounderOrderByCreatedAtDesc(UserEntity founder);

    // Find public groups
    List<Group> findByTypeOrderByMemberCountDesc(String type);

    // Search groups by name
    @Query("{'name': {$regex: ?0, $options: 'i'}}")
    List<Group> findByNameContainingIgnoreCase(String name);

    // Find groups with pagination
    Page<Group> findByTypeOrderByCreatedAtDesc(String type, Pageable pageable);

    // Find groups user is not member of
    @Query("{'_id': {$nin: ?0}, 'type': 'Public'}")
    List<Group> findPublicGroupsUserNotMemberOf(List<String> groupIds);

    // Count groups by type
    long countByType(String type);

    // Find popular groups (by member count)
    List<Group> findTop10ByTypeOrderByMemberCountDesc(String type);

}