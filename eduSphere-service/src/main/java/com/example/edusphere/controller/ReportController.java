package com.example.edusphere.controller;

import com.example.edusphere.dto.request.GenerateReportRequest;
import com.example.edusphere.dto.response.GenerateReportResponse;
import com.example.edusphere.entity.Report;
import com.example.edusphere.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;
    private final MongoTemplate mongoTemplate;

    @PostMapping("/generate")
    public ResponseEntity<GenerateReportResponse> generateReport(@RequestBody GenerateReportRequest request) {
        try {
            System.out.println("🎯 Report generation request received: " + request.getQuery());
            GenerateReportResponse response = reportService.generateReport(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("❌ Error in report controller: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(GenerateReportResponse.builder().data(List.of()).build());
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Report>> getRecentReports() {
        List<Report> recentReports = reportService.getRecentReports();
        return ResponseEntity.ok(recentReports);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String id) {
        Report report = mongoTemplate.findById(id, Report.class);

        if (report == null || report.getReportData() == null) {
            return ResponseEntity.notFound().build();
        }

        String filename = "report_" + id + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(report.getReportData().getBytes());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable String id) {
        try {
            reportService.deleteReport(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            System.out.println("❌ Error deleting report: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<GenerateReportResponse> viewReport(@PathVariable String id) {
        try {
            List<Map<String, Object>> reportData = reportService.getReportData(id);
            if (reportData == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(GenerateReportResponse.builder().data(reportData).build());
        } catch (Exception e) {
            System.out.println("❌ Error viewing report: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(GenerateReportResponse.builder().data(List.of()).build());
        }
    }
}