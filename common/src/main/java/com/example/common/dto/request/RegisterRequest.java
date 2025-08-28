package com.example.common.dto.request;

import lombok.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    String username;
    String email;
    String password;
}
