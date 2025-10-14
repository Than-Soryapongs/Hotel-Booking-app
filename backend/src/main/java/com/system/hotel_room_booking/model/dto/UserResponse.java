package com.system.hotel_room_booking.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

import com.system.hotel_room_booking.model.entity.Gender;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String pendingEmail;
    private String firstName;
    private String lastName;
    private Gender gender;
    private String address;
    private String profileImageUrl;
    private Set<String> roles;
    private Boolean enabled;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}

