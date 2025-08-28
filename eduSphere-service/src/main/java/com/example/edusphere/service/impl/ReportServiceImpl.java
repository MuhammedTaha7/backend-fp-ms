package com.example.edusphere.service.impl;

import com.example.edusphere.dto.request.GenerateReportRequest;
import com.example.edusphere.dto.response.GenerateReportResponse;
import com.example.edusphere.entity.Report;
import com.example.edusphere.service.OpenAiService;
import com.example.edusphere.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final MongoTemplate mongoTemplate;
    private final OpenAiService openAiService;

    @Override
    public GenerateReportResponse generateReport(GenerateReportRequest request) {
        // ... (existing generateReport logic)
        String userQuery = request.getQuery();

        try {
            String gptResponse = openAiService.generateMongoQuery(userQuery);
            Document queryDoc = Document.parse(gptResponse.trim());
            String collectionName = queryDoc.getString("collection");
            if (collectionName == null || collectionName.isEmpty()) {
                return GenerateReportResponse.builder().data(List.of()).build();
            }

            Document filter = queryDoc.get("filter", Document.class);
            if (filter == null) {
                filter = new Document();
            }
            Query mongoQuery = new BasicQuery(filter);

            if (queryDoc.containsKey("sort")) {
                Document sortDoc = queryDoc.get("sort", Document.class);
                if (sortDoc != null) {
                    sortDoc.forEach((field, direction) -> {
                        int dir = direction instanceof Integer ? (Integer) direction : 1;
                        mongoQuery.with(Sort.by(
                                dir == -1 ? Sort.Direction.DESC : Sort.Direction.ASC,
                                field
                        ));
                    });
                }
            }

            if (queryDoc.containsKey("limit")) {
                Integer limit = queryDoc.getInteger("limit");
                if (limit != null && limit > 0) {
                    mongoQuery.limit(limit);
                }
            }

            if (queryDoc.containsKey("fields")) {
                List<?> fieldsObject = queryDoc.get("fields", List.class);
                if (fieldsObject != null && !fieldsObject.isEmpty()) {
                    List<String> fields = fieldsObject.stream()
                            .map(Object::toString)
                            .collect(Collectors.toList());
                    for (String field : fields) {
                        mongoQuery.fields().include(field);
                    }
                }
            }

            List<Map> results = mongoTemplate.find(mongoQuery, Map.class, collectionName);

            if ("users".equals(collectionName)) {
                results.forEach(result -> result.remove("password"));
            }

            String csvData = convertToCsvString(results);
            Report report = Report.builder()
                    .query(userQuery)
                    .generatedDate(LocalDateTime.now())
                    .collectionName(collectionName)
                    .recordCount(results.size())
                    .reportData(csvData)
                    .build();

            mongoTemplate.save(report);

            return GenerateReportResponse.builder()
                    .data((List<Map<String, Object>>)(List<?>) results)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return GenerateReportResponse.builder().data(List.of()).build();
        }
    }

    @Override
    public List<Report> getRecentReports() {
        Query query = new Query().limit(10).with(Sort.by(Sort.Direction.DESC, "generatedDate"));
        return mongoTemplate.find(query, Report.class);
    }

    @Override
    public void deleteReport(String id) {
        mongoTemplate.remove(Query.query(Criteria.where("id").is(id)), Report.class);
    }

    @Override
    public List<Map<String, Object>> getReportData(String id) {
        Report report = mongoTemplate.findById(id, Report.class);
        if (report == null || report.getReportData() == null) {
            return null;
        }
        return convertCsvToMaps(report.getReportData());
    }

    private String convertToCsvString(List<Map> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        List<String> headers = (List<String>) data.get(0).keySet().stream().map(Object::toString).collect(Collectors.toList());
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(String.join(",", headers)).append("\n");
        for (Map<String, Object> row : data) {
            String rowString = headers.stream()
                    .map(key -> {
                        Object value = row.get(key);
                        String stringValue = (value == null) ? "" : value.toString();
                        if (stringValue.contains(",") || stringValue.contains("\"")) {
                            return "\"" + stringValue.replace("\"", "\"\"") + "\"";
                        }
                        return stringValue;
                    })
                    .collect(Collectors.joining(","));
            csvBuilder.append(rowString).append("\n");
        }
        return csvBuilder.toString();
    }

    private List<Map<String, Object>> convertCsvToMaps(String csvData) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvData))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return result;
            }
            String[] headers = headerLine.split(",");
            String dataLine;
            while ((dataLine = reader.readLine()) != null) {
                String[] values = dataLine.split(",");
                Map<String, Object> row = new HashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    row.put(headers[i], values[i]);
                }
                result.add(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}