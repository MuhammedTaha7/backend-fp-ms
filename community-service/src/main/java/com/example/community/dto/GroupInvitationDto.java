package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class GroupInvitationDto {
    private String id;
    private GroupDto group;
    private UserDto inviter;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private long daysUntilExpiry; // Calculated field for frontend

    // Calculate days until expiry
    public long getDaysUntilExpiry() {
        if (expiresAt != null) {
            return java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
        }
        return 0;
    }
}