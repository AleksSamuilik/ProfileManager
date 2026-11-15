package com.sam.profilemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sam.profilemanager.AbstractIntegrationTest;
import com.sam.profilemanager.dto.UserRequest;
import com.sam.profilemanager.model.User;
import com.sam.profilemanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserControllerIntegrationTest extends AbstractIntegrationTest {

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
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Alex"))
                .andExpect(jsonPath("$[1].name").value("Maria"));
    }

    @Test
    void getUserById_ReturnsUser() throws Exception {
        User saved = userRepository.save(new User("Alex", "alex@example.com", 25));

        mockMvc.perform(get("/api/users/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Alex"))
                .andExpect(jsonPath("$.email").value("alex@example.com"));
    }

    @Test
    void getUserById_ReturnsBadRequestWhenNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 9999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found: 9999"));
    }

    @Test
    void createUser_ReturnsCreatedUserAndDto() throws Exception {
        UserRequest request = new UserRequest("Alex", "alex@example.com", 25);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Alex"))
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void createUser_ReturnsBadRequestWhenValidationFails() throws Exception {
        UserRequest invalidRequest = new UserRequest("", "bad-email", -5);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createUser_ReturnsBadRequestWhenEmailAlreadyExists() throws Exception {
        userRepository.save(new User("Alex", "alex@example.com", 25));
        UserRequest request = new UserRequest("Maria", "alex@example.com", 30);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already in use: alex@example.com"));
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
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void updateUser_ReturnsBadRequestWhenChangingToExistingEmail() throws Exception {
        userRepository.save(new User("Alex", "alex@example.com", 25));
        User maria = userRepository.save(new User("Maria", "maria@example.com", 30));
        UserRequest request = new UserRequest("Maria", "alex@example.com", 30);

        mockMvc.perform(put("/api/users/{id}", maria.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already in use: alex@example.com"));
    }

    @Test
    void deleteUser_ReturnsNoContentAndRemovesUser() throws Exception {
        User saved = userRepository.save(new User("Alex", "alex@example.com", 25));

        mockMvc.perform(delete("/api/users/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", saved.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_ReturnsBadRequestWhenNotFound() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", 9999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found: 9999"));
    }
}