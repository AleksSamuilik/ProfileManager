package com.sam.notification;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.ServerSocket;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = AbstractMailIntegrationTest.TOPIC,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public abstract class AbstractMailIntegrationTest {

    protected static final String TOPIC = "user-events";
    protected static final long MAIL_TIMEOUT_MS = 15_000;

    protected static final GreenMail greenMail;
    private static final int smtpPort = freePort();

    static {
        greenMail = new GreenMail(new ServerSetup(smtpPort, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        greenMail.start();
        Runtime.getRuntime().addShutdownHook(new Thread(greenMail::stop));
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> smtpPort);
        registry.add("notification.site-name", () -> "ваш сайт");
        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.producer.properties.spring.json.add.type.headers", () -> "false");
    }

    @BeforeEach
    void clearMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    protected static MimeMessage awaitSingleMail() {
        boolean received = greenMail.waitForIncomingEmail(MAIL_TIMEOUT_MS, 1);
        if (!received) {
            throw new AssertionError("Email was not received within " + MAIL_TIMEOUT_MS + " ms");
        }
        MimeMessage[] messages = greenMail.getReceivedMessages();
        if (messages.length != 1) {
            throw new AssertionError("Expected exactly 1 email, but got " + messages.length);
        }
        return messages[0];
    }

    protected static String bodyOf(MimeMessage message) throws Exception {
        return ((String) message.getContent()).trim();
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("No free port for test SMTP server", e);
        }
    }
}
