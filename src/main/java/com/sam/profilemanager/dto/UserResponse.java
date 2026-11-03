package com.sam.profilemanager.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        int age,
        LocalDateTime createdAt,
        Long version
) {
}