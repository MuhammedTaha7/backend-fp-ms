package com.example.edusphere.controller;

import com.example.common.exceptions.BadRequestException;
import com.example.edusphere.service.ResetPasswordService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.edusphere.dto.request.ResetPasswordRequest;
import com.example.edusphere.dto.response.ResetPasswordResponse;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class PasswordController {

    @Autowired
    private ResetPasswordService resetPasswordService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ResetPasswordResponse> forgotPassword(@RequestBody ResetPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new BadRequestException("Email is required.");
        }
        String message = resetPasswordService.sendResetCode(request.getEmail());
        ResetPasswordResponse response = new ResetPasswordResponse();
        response.setMessage(message);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
    //public ResponseEntity<ResetPasswordResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request.getCode() == null || request.getCode().isEmpty()) {
            throw new BadRequestException("Token is required.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            throw new BadRequestException("New password is required.");
        }
        String message = resetPasswordService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(message);

//        String message = resetPasswordService.resetPassword(request.getEmail(), request.getToken(), request.getNewPassword());
//        ResetPasswordResponse response = new ResetPasswordResponse();
//        response.setMessage(message);
//        return ResponseEntity.ok(response);
    }
}