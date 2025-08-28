package com.example.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvitationResponseRequest {

    @NotBlank(message = "Response is required")
    @Pattern(regexp = "ACCEPTED|REJECTED", message = "Response must be either ACCEPTED or REJECTED")
    private String response;
}