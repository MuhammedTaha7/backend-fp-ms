package com.example.edusphere.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AuthResponse {
    private String id;
    private String username;
    private String email;
    private String role;
    private String name;
    private String profilePic;
}