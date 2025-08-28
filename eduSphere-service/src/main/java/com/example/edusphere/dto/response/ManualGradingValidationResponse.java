package com.example.edusphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualGradingValidationResponse {

    private Boolean isValid;
    private List<String> errors;
    private List<String> warnings;

    // Calculated totals
    private Integer totalScore;
    private Integer maxScore;
    private Double percentage;

    // Question validation details
    private List<QuestionValidation> questionValidations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionValidation {
        private String questionId;
        private Integer assignedScore;
        private Integer maxPoints;
        private Boolean isValid;
        private String errorMessage;
    }
}