package com.sam.profilemanager;

import com.sam.profilemanager.event.UserEvent;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    private static final boolean USE_H2 = System.getProperty("h2") != null;

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = USE_H2 ? null : new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        if (POSTGRES != null) {
            POSTGRES.start();
        }
    }

    @MockBean
    protected KafkaTemplate<String, UserEvent> kafkaTemplate;

    @BeforeEach
    void stubKafka() {
        when(kafkaTemplate.send(anyString(), anyString(), any(UserEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        if (USE_H2) {
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
        } else {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
    }
}
