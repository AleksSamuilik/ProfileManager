package com.sam.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Описание ошибки")
public record ErrorResponse(
        @Schema(description = "Сообщение об ошибке", example = "Failed to send email")
        String error
) {
}
