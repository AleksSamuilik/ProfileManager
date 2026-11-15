package com.sam.notification.controller;

import com.sam.notification.dto.ErrorResponse;
import com.sam.notification.dto.NotificationRequest;
import com.sam.notification.dto.NotificationResponse;
import com.sam.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications", description = "Прямая отправка уведомлений на почту (без Kafka)")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Отправить уведомление на почту",
            description = "Отправляет письмо о создании или удалении аккаунта — тот же текст, что и при получении события из Kafka.")
    @ApiResponse(responseCode = "200", description = "Письмо отправлено")
    @ApiResponse(responseCode = "400", description = "Невалидный email или неизвестная операция",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "502", description = "SMTP-сервер недоступен",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public NotificationResponse send(@Valid @RequestBody NotificationRequest request) {
        notificationService.notifyUser(request.operation(), request.email());
        return new NotificationResponse("sent", request.email(), request.operation().name());
    }
}
