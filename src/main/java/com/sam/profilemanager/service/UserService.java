package com.sam.profilemanager.service;

import com.sam.profilemanager.dto.UserRequest;
import com.sam.profilemanager.dto.UserResponse;
import com.sam.profilemanager.event.UserEvent;
import com.sam.profilemanager.model.User;
import com.sam.profilemanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class UserService {

    private final UserRepository repository;
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private final String topic;

    public UserService(UserRepository repository,
                       KafkaTemplate<String, UserEvent> kafkaTemplate,
                       @Value("${spring.kafka.topic.user-events}") String topic) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use: " + request.email());
        }
        User user = new User(request.name(), request.email(), request.age());
        User saved = repository.save(user);
        kafkaTemplate.send(topic, new UserEvent(UserEvent.CREATE, saved.getEmail()));
        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        repository.findByEmail(request.email())
                .filter(existing -> existing.getId() == null || !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email already in use: " + request.email());
                });

        user.setName(request.name());
        user.setEmail(request.email());
        user.setAge(request.age());
        return toResponse(repository.saveAndFlush(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        String email = user.getEmail();
        repository.deleteById(id);
        kafkaTemplate.send(topic, new UserEvent(UserEvent.DELETE, email));
        repository.deleteById(id);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getCreatedAt(),
                user.getVersion()
        );
    }
}