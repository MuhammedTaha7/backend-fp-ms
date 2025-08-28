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
@Document(collection = "friend_requests")
public class FriendRequest {

    @Id
    private String id;

    @DBRef
    private UserEntity sender;

    @DBRef
    private UserEntity receiver;

    private String status; // "PENDING", "ACCEPTED", "REJECTED"
    private String message; // Optional message with friend request

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;
}