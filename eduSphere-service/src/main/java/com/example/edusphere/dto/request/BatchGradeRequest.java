package com.example.edusphere.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchGradeRequest {

    @NotEmpty(message = "Response IDs are required")
    private List<String> responseIds;

    private String instructorFeedback;

    private Boolean flagForReview = false;

    private String batchGradeType; // "feedback", "flag", "score"

    private Integer uniformScore; // For uniform scoring
}