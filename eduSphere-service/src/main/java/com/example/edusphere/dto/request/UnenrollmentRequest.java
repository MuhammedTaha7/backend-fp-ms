package com.example.edusphere.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class UnenrollmentRequest {
    private List<String> studentIds;
}