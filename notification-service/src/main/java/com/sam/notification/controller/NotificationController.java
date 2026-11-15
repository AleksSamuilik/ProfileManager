package com.sam.notification.controller;

import com.sam.notification.dto.NotificationRequest;
import com.sam.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public Map<String, String> send(@Valid @RequestBody NotificationRequest request) {
        notificationService.notifyUser(request.operation(), request.email());
        return Map.of(
                "status", "sent",
                "email", request.email(),
                "operation", request.operation().name());
    }
}
