package com.sam.notification.kafka;

import com.sam.notification.event.UserEvent;
import com.sam.notification.model.Operation;
import com.sam.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    private final NotificationService notificationService;

    public UserEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${spring.kafka.topic.user-events}")
    public void onUserEvent(UserEvent event) {
        log.debug("Received event: {}", event);
        if (event == null || event.email() == null || event.email().isBlank()) {
            throw new IllegalArgumentException("Event without email: " + event);
        }
        notificationService.notifyUser(Operation.from(event.operation()), event.email());
    }
}
