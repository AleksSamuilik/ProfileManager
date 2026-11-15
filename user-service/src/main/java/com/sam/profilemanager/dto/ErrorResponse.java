package com.sam.profilemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Описание ошибки")
public record ErrorResponse(
        @Schema(description = "Сообщение об ошибке", example = "User not found: 42")
        String error
) {
}
