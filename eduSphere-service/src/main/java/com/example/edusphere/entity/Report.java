package com.example.edusphere.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    @Id
    private String id;
    private String query;
    private LocalDateTime generatedDate;
    private String collectionName;
    private int recordCount;
    private String reportData; // Stores the CSV content as a string
}