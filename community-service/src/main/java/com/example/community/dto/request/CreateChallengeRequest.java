package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChallengeRequest {
    private String title;
    private String description;
    private String category;
    private String type; // "external", "interactive"
    private String difficulty; // "Easy", "Medium", "Hard"
    private Integer points;
}