package com.example.edusphere.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Jacksonized
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateReportResponse {
    private List<Map<String, Object>> data;
}
