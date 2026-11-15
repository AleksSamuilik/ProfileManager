package com.sam.profilemanager.service;

import com.sam.profilemanager.dto.UserRequest;
import com.sam.profilemanager.dto.UserResponse;
import com.sam.profilemanager.event.UserEvent;
import com.sam.profilemanager.exception.EmailAlreadyInUseException;
import com.sam.profilemanager.exception.UserNotFoundException;
import com.sam.profilemanager.model.User;
import com.sam.profilemanager.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * События о создании/удалении публикуются внутри транзакции как Spring-события,
 * а в Kafka их отправляет {@link com.sam.profilemanager.event.UserEventPublisher} только после успешного коммита.
 */
@Service
public class UserService {

    private final UserRepository repository;
    private final ApplicationEventPublisher events;

    public UserService(UserRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
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
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }
        User saved = repository.save(new User(request.name(), request.email(), request.age()));
        events.publishEvent(new UserEvent(UserEvent.CREATE, saved.getEmail()));
        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        repository.findByEmail(request.email())
                .filter(existing -> existing.getId() == null || !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new EmailAlreadyInUseException(request.email());
                });

        user.setName(request.name());
        user.setEmail(request.email());
        user.setAge(request.age());
        return toResponse(repository.saveAndFlush(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        repository.delete(user);
        events.publishEvent(new UserEvent(UserEvent.DELETE, user.getEmail()));
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
