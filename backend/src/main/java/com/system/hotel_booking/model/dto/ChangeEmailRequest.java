package com.system.hotel_booking.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeEmailRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String newEmail;
    
    @NotBlank(message = "Password is required for verification")
    private String password;
}
