package com.sam.notification.dto;

import com.sam.notification.model.Operation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationRequest(
        @NotBlank(message = "Email must not be empty")
        @Email(message = "Email should be valid")
        String email,

        @NotNull(message = "Operation must not be empty")
        Operation operation
) {
}
