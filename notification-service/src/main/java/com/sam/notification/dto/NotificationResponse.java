package com.sam.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Результат отправки уведомления")
public record NotificationResponse(
        @Schema(description = "Статус отправки", example = "sent")
        String status,

        @Schema(description = "Адрес получателя", example = "alex@example.com")
        String email,

        @Schema(description = "Операция, о которой отправлено уведомление", example = "CREATE")
        String operation
) {
}
