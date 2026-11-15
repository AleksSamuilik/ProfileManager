package com.sam.profilemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные для создания или обновления пользователя")
public record UserRequest(
        @Schema(description = "Имя", example = "Alex", maxLength = 255)
        @NotBlank(message = "Name must not be empty")
        @Size(max = 255, message = "Name is too long")
        String name,

        @Schema(description = "Email (уникальный)", example = "alex@example.com", maxLength = 255)
        @NotBlank(message = "Email must not be empty")
        @Email(message = "Email should be valid")
        @Size(max = 255, message = "Email is too long")
        String email,

        @Schema(description = "Возраст", example = "25", minimum = "0", maximum = "150")
        @Min(value = 0, message = "Age must be at least 0")
        @Max(value = 150, message = "Age must be at most 150")
        int age
) {
}
