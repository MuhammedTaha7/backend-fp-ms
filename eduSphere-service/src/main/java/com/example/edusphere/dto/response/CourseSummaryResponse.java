package com.example.edusphere.dto.response;

import com.example.edusphere.entity.YearlyEnrollment;
import lombok.Data;
import java.util.List;

@Data
public class CourseSummaryResponse {
    private String id;
    private String name;
    private String code;
    private String imageUrl;
    private String department;
    private String lecturerName;
    private int credits;
    private String academicYear;
    private String semester;
    private Integer year;
    private Boolean selectable;
    private List<YearlyEnrollment> enrollments;
}