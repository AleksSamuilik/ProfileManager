package com.sam.profilemanager.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Отправляет события о пользователях в Kafka только после успешного коммита транзакции:
 * если транзакция откатилась, сообщение в топик не попадёт.
 */
@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private final String topic;

    public UserEventPublisher(KafkaTemplate<String, UserEvent> kafkaTemplate,
                              @Value("${spring.kafka.topic.user-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserEvent(UserEvent event) {
        kafkaTemplate.send(topic, event.email(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send {} event for {} to Kafka", event.operation(), event.email(), ex);
                    } else {
                        log.debug("Sent {} event for {} to Kafka", event.operation(), event.email());
                    }
                });
    }
}
