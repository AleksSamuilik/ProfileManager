package com.sam.notification.kafka;

import com.sam.notification.AbstractMailIntegrationTest;
import com.sam.notification.event.UserEvent;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Полный путь: сообщение в Kafka -> слушатель -> SMTP -> письмо в ящике получателя. */
class UserEventListenerIntegrationTest extends AbstractMailIntegrationTest {

    @Autowired
    private KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Test
    void createEvent_SendsAccountCreatedEmail() throws Exception {
        kafkaTemplate.send(TOPIC, "alex@example.com", new UserEvent("CREATE", "alex@example.com"));

        MimeMessage mail = awaitSingleMail();

        assertThat(mail.getAllRecipients()).hasSize(1);
        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo("alex@example.com");
        assertThat(mail.getFrom()[0].toString()).isEqualTo("no-reply@profilemanager.local");
        assertThat(mail.getSubject()).isEqualTo("Аккаунт создан");
        assertThat(bodyOf(mail))
                .isEqualTo("Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан.");
    }

    @Test
    void deleteEvent_SendsAccountDeletedEmail() throws Exception {
        kafkaTemplate.send(TOPIC, "maria@example.com", new UserEvent("DELETE", "maria@example.com"));

        MimeMessage mail = awaitSingleMail();

        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo("maria@example.com");
        assertThat(mail.getSubject()).isEqualTo("Аккаунт удалён");
        assertThat(bodyOf(mail)).isEqualTo("Здравствуйте! Ваш аккаунт был удалён.");
    }

    @Test
    void invalidEvents_AreSkippedAndDoNotBlockNextMessages() throws Exception {
        kafkaTemplate.send(TOPIC, "bad@example.com", new UserEvent("UNKNOWN", "bad@example.com"));
        kafkaTemplate.send(TOPIC, "noemail", new UserEvent("CREATE", null));
        kafkaTemplate.send(TOPIC, "good@example.com", new UserEvent("CREATE", "good@example.com"));

        // Сообщения в одной партиции обрабатываются по порядку: если пришло письмо для good@,
        // значит, два некорректных события уже обработаны (пропущены) и писем по ним нет.
        MimeMessage mail = awaitSingleMail();

        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo("good@example.com");
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }
}
