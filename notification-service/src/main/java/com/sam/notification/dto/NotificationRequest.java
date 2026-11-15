package com.sam.notification.dto;

import com.sam.notification.model.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на отправку уведомления пользователю")
public record NotificationRequest(
        @Schema(description = "Email получателя", example = "alex@example.com")
        @NotBlank(message = "Email must not be empty")
        @Email(message = "Email should be valid")
        String email,

        @Schema(description = "Операция: CREATE — аккаунт создан, DELETE — аккаунт удалён", example = "CREATE")
        @NotNull(message = "Operation must not be empty")
        Operation operation
) {
}
