package com.sam.profilemanager.service;

import com.sam.profilemanager.dto.UserRequest;
import com.sam.profilemanager.dto.UserResponse;
import com.sam.profilemanager.model.User;
import com.sam.profilemanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return toResponse(user);
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new RuntimeException("User with this email already exists");
        }
        User user = new User(request.name(), request.email(), request.age());
        User saved = repository.save(user);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        repository.findByEmail(request.email())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException("User with this email already exists");
                    }
                });

        user.setName(request.name());
        user.setEmail(request.email());
        user.setAge(request.age());
        User updated = repository.save(user);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
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