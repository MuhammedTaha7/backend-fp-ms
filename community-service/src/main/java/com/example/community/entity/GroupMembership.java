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
@Document(collection = "group_memberships")
public class GroupMembership {

    @Id
    private String id;

    @DBRef
    private Group group;

    @DBRef
    private UserEntity user;

    private String role; // "Founder", "Co-founder", "Member"

    @CreatedDate
    private LocalDateTime joinDate;
}