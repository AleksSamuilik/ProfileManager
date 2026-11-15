package com.sam.notification.service;

import com.sam.notification.model.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Единая точка отправки уведомлений: ею пользуются и Kafka-слушатель, и REST API,
 * поэтому текст письма для операции одинаков в обоих случаях.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailService emailService;
    private final String siteName;

    public NotificationService(EmailService emailService,
                               @Value("${notification.site-name}") String siteName) {
        this.emailService = emailService;
        this.siteName = siteName;
    }

    public void notifyUser(Operation operation, String email) {
        emailService.send(email, operation.subject(), operation.body(siteName));
        log.info("Notification {} sent to {}", operation, email);
    }
}
