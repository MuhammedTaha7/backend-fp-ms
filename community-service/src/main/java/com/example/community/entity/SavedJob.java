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
@Document(collection = "saved_jobs")
public class SavedJob {

    @Id
    private String id;

    @DBRef
    private UserEntity user;

    @DBRef
    private JobPost jobPost;

    @CreatedDate
    private LocalDateTime savedAt;
}