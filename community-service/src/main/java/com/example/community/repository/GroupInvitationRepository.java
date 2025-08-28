package com.example.community.repository;

import com.example.community.entity.Group;
import com.example.community.entity.GroupInvitation;
import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupInvitationRepository extends MongoRepository<GroupInvitation, String> {

    // Find invitations by invitee (received invitations)
    List<GroupInvitation> findByInviteeAndStatusOrderByCreatedAtDesc(UserEntity invitee, String status);

    // Find invitations by inviter (sent invitations)
    List<GroupInvitation> findByInviterAndStatusOrderByCreatedAtDesc(UserEntity inviter, String status);

    // Find invitations for a specific group
    List<GroupInvitation> findByGroupAndStatusOrderByCreatedAtDesc(Group group, String status);

    // Check if invitation already exists
    Optional<GroupInvitation> findByGroupAndInviterAndInviteeAndStatus(Group group, UserEntity inviter, UserEntity invitee, String status);

    // Find all pending invitations for a user
    List<GroupInvitation> findByInviteeAndStatus(UserEntity invitee, String status);

    // Count pending invitations for a user
    long countByInviteeAndStatus(UserEntity invitee, String status);

    // Find expired invitations
    @Query("{'status': 'PENDING', 'expiresAt': {$lt: ?0}}")
    List<GroupInvitation> findExpiredInvitations(LocalDateTime now);

    // Delete invitations between users for a group
    void deleteByGroupAndInviterAndInvitee(Group group, UserEntity inviter, UserEntity invitee);
}