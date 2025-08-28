package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequest {
    private String userId;
    private String username;
    private String text;
}