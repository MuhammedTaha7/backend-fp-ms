package com.example.edusphere.service;

import com.example.edusphere.dto.request.GenerateReportRequest;
import com.example.edusphere.dto.response.GenerateReportResponse;
import com.example.edusphere.entity.Report;

import java.util.List;
import java.util.Map;

public interface ReportService {
    GenerateReportResponse generateReport(GenerateReportRequest request);
    List<Report> getRecentReports();
    void deleteReport(String id);
    List<Map<String, Object>> getReportData(String id);
}