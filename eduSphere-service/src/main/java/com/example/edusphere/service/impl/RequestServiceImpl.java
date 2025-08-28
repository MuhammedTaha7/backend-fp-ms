package com.example.edusphere.service.impl;

import com.example.edusphere.dto.request.RequestRequestDto;
import com.example.edusphere.dto.response.RequestResponseDto;
import com.example.edusphere.entity.StudentRequest;
import com.example.common.entity.UserEntity;
import com.example.edusphere.repository.StudentRequestRepository;
import com.example.common.repository.UserRepository;
import com.example.edusphere.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final StudentRequestRepository requestRepository;
    private final UserRepository userRepository;

    @Override
    public List<RequestResponseDto> getRequestsByLecturerId(String lecturerId) {
        List<StudentRequest> requests = requestRepository.findByReceiverId(lecturerId);
        return requests.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public RequestResponseDto createRequest(RequestRequestDto requestDto) {
        StudentRequest request = new StudentRequest();
        request.setSenderId(requestDto.getSenderId());
        request.setReceiverId(requestDto.getReceiverId());
        request.setSubject(requestDto.getSubject());
        request.setMessage(requestDto.getMessage());
        request.setType(requestDto.getType());
        request.setPriority(requestDto.getPriority());
        request.setStatus("pending");
        request.setDate(LocalDateTime.now());

        StudentRequest savedRequest = requestRepository.save(request);
        return mapToDto(savedRequest);
    }

    @Override
    public RequestResponseDto updateRequestStatus(String requestId, String status) {
        StudentRequest existingRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (status == null || (!status.equals("approved") && !status.equals("rejected") && !status.equals("pending"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status provided");
        }

        existingRequest.setStatus(status);

        StudentRequest updatedRequest = requestRepository.save(existingRequest);
        return mapToDto(updatedRequest);
    }

    @Override
    public RequestResponseDto submitResponse(String requestId, String response) {
        StudentRequest existingRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        existingRequest.setResponse(response);
        existingRequest.setResponseDate(LocalDateTime.now());
        existingRequest.setStatus("responded");

        StudentRequest updatedRequest = requestRepository.save(existingRequest);
        return mapToDto(updatedRequest);
    }

    private RequestResponseDto mapToDto(StudentRequest request) {
        RequestResponseDto dto = new RequestResponseDto();
        dto.setId(request.getId());
        dto.setSenderId(request.getSenderId());

        // Fetch sender's name
        UserEntity sender = userRepository.findById(request.getSenderId()).orElse(null);
        dto.setSenderName(sender != null ? sender.getName() : "Unknown Student");

        dto.setReceiverId(request.getReceiverId());
        dto.setSubject(request.getSubject());
        dto.setMessage(request.getMessage());
        dto.setType(request.getType());
        dto.setPriority(request.getPriority());
        dto.setStatus(request.getStatus());
        dto.setResponse(request.getResponse());
        dto.setDate(request.getDate());
        dto.setResponseDate(request.getResponseDate());
        return dto;
    }
}