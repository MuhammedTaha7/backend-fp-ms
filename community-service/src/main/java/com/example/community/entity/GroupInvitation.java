package com.example.community.entity;

import com.example.common.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "group_invitations")
public class GroupInvitation {

    @Id
    private String id;

    @DBRef
    private Group group;

    @DBRef
    private UserEntity inviter; // Who sent the invitation

    @DBRef
    private UserEntity invitee; // Who received the invitation

    private String status; // "PENDING", "ACCEPTED", "REJECTED", "EXPIRED"
    private String message; // Optional invitation message

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;
    private LocalDateTime expiresAt; // Invitations expire after 30 days
}