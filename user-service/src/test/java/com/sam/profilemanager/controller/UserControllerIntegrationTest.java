package com.sam.profilemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.sam.profilemanager.AbstractIntegrationTest;
import com.sam.profilemanager.dto.UserRequest;
import com.sam.profilemanager.event.UserEvent;
import com.sam.profilemanager.model.User;
import com.sam.profilemanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String TOPIC = "user-events";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void getAllUsers_ReturnsListOfUsers() throws Exception {
        userRepository.save(new User("Alex", "alex@example.com", 25));
        userRepository.save(new User("Maria", "maria@example.com", 30));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.users", hasSize(2)))
                .andExpect(jsonPath("$._embedded.users[0].name").value("Alex"))
                .andExpect(jsonPath("$._embedded.users[1].name").value("Maria"))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/api/users")))
                .andExpect(jsonPath("$._embedded.users[0]._links.self.href").value(containsString("/api/users/")));
    }

    @Test
    void getAllUsers_ReturnsSelfLinkWhenNoUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/api/users")));
    }

    @Test
    void getUserById_ReturnsUser() throws Exception {
        User saved = userRepository.save(new User("Alex", "alex@example.com", 25));

        mockMvc.perform(get("/api/users/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Alex"))
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/api/users/" + saved.getId())))
                .andExpect(jsonPath("$._links.update.href").value(endsWith("/api/users/" + saved.getId())))
                .andExpect(jsonPath("$._links.delete.href").value(endsWith("/api/users/" + saved.getId())))
                .andExpect(jsonPath("$._links.users.href").value(endsWith("/api/users")));
    }

    @Test
    void getUserById_LinksCanBeFollowed() throws Exception {
        User saved = userRepository.save(new User("Alex", "alex@example.com", 25));

        String body = mockMvc.perform(get("/api/users/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Переходим по ссылке "users" из ответа: она должна вести на рабочий список с этим же пользователем
        String usersHref = JsonPath.read(body, "$._links.users.href");
        mockMvc.perform(get(URI.create(usersHref)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.users", hasSize(1)))
                .andExpect(jsonPath("$._embedded.users[0].id").value(saved.getId()));
    }

    @Test
    void getUserById_ReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: 9999"));
    }

    @Test
    void createUser_ReturnsCreatedUserAndSendsKafkaEvent() throws Exception {
        UserRequest request = new UserRequest("Alex", "alex@example.com", 25);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/users/")))
                .andExpect(jsonPath("$._links.self.href").value(containsString("/api/users/")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Alex"))
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.version").value(0));

        verify(kafkaTemplate).send(TOPIC, "alex@example.com",
                new UserEvent(UserEvent.CREATE, "alex@example.com"));
    }

    @Test
    void createUser_ReturnsBadRequestWhenValidationFails() throws Exception {
        UserRequest invalidRequest = new UserRequest("", "bad-email", -5);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(UserEvent.class));
    }

    @Test
    void createUser_ReturnsConflictWhenEmailAlreadyExists() throws Exception {
        userRepository.save(new User("Alex", "alex@example.com", 25));
        UserRequest request = new UserRequest("Maria", "alex@example.com", 30);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already in use: alex@example.com"));

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(UserEvent.class));
    }

    @Test
    void updateUser_ReturnsUpdatedUser() throws Exception {
        User saved = userRepository.save(new User("Alex", "alex@example.com", 25));
        UserRequest request = new UserRequest("Alexander", "alex@example.com", 26);

        mockMvc.perform(put("/api/users/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alexander"))
                .andExpect(jsonPath("$.age").value(26))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/api/users/" + saved.getId())));
    }

    @Test
    void updateUser_ReturnsConflictWhenChangingToExistingEmail() throws Exception {
        userRepository.save(new User("Alex", "alex@example.com", 25));
        User maria = userRepository.save(new User("Maria", "maria@example.com", 30));
        UserRequest request = new UserRequest("Maria", "alex@example.com", 30);

        mockMvc.perform(put("/api/users/{id}", maria.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already in use: alex@example.com"));
    }

    @Test
    void deleteUser_ReturnsNoContentRemovesUserAndSendsKafkaEvent() throws Exception {
        User saved = userRepository.save(new User("Alex", "alex@example.com", 25));

        mockMvc.perform(delete("/api/users/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", saved.getId()))
                .andExpect(status().isNotFound());

        verify(kafkaTemplate).send(eq(TOPIC), eq("alex@example.com"),
                eq(new UserEvent(UserEvent.DELETE, "alex@example.com")));
    }

    @Test
    void deleteUser_ReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: 9999"));

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(UserEvent.class));
    }
}
