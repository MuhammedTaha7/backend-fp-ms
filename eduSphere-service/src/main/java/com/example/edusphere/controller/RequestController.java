package com.example.edusphere.controller;

import com.example.edusphere.dto.request.RequestRequestDto;
import com.example.edusphere.dto.response.RequestResponseDto;
import com.example.edusphere.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class RequestController {

    private final RequestService requestService;

    @GetMapping("/by-lecturer/{lecturerId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<List<RequestResponseDto>> getRequestsByLecturer(@PathVariable String lecturerId) {
        List<RequestResponseDto> requests = requestService.getRequestsByLecturerId(lecturerId);
        return ResponseEntity.ok(requests);
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<RequestResponseDto> createRequest(@RequestBody RequestRequestDto requestDto) {
        RequestResponseDto newRequest = requestService.createRequest(requestDto);
        return new ResponseEntity<>(newRequest, HttpStatus.CREATED);
    }

    @PatchMapping("/{requestId}/status")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<RequestResponseDto> updateRequestStatus(@PathVariable String requestId, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        RequestResponseDto updatedRequest = requestService.updateRequestStatus(requestId, status);
        return ResponseEntity.ok(updatedRequest);
    }

    @PostMapping("/{requestId}/response")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<RequestResponseDto> submitResponse(@PathVariable String requestId, @RequestBody Map<String, String> body) {
        String response = body.get("response");
        if (response == null) {
            return ResponseEntity.badRequest().build();
        }
        RequestResponseDto updatedRequest = requestService.submitResponse(requestId, response);
        return ResponseEntity.ok(updatedRequest);
    }
}