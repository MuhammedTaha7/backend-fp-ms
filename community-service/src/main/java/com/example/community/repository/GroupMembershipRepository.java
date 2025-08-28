package com.example.community.repository;

import com.example.community.entity.Group;
import com.example.community.entity.GroupMembership;
import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMembershipRepository extends MongoRepository<GroupMembership, String> {

    // Find user's group memberships
    List<GroupMembership> findByUserOrderByJoinDateDesc(UserEntity user);

    // Find group members
    List<GroupMembership> findByGroupOrderByJoinDateAsc(Group group);

    // Check if user is member of group
    Optional<GroupMembership> findByGroupAndUser(Group group, UserEntity user);

    // Find groups user is member of
    @Query("{'user': ?0}")
    List<GroupMembership> findGroupsByUser(UserEntity user);

    // Count members in group
    long countByGroup(Group group);

    // Find members by role
    List<GroupMembership> findByGroupAndRole(Group group, String role);

    // Find founders and co-founders
    @Query("{'group': ?0, 'role': {$in: ['Founder', 'Co-founder']}}")
    List<GroupMembership> findGroupAdmins(Group group);

    // Delete membership
    void deleteByGroupAndUser(Group group, UserEntity user);
}