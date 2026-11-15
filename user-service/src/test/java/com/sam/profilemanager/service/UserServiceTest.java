package com.sam.profilemanager.service;

import com.sam.profilemanager.dto.UserRequest;
import com.sam.profilemanager.dto.UserResponse;
import com.sam.profilemanager.event.UserEvent;
import com.sam.profilemanager.exception.EmailAlreadyInUseException;
import com.sam.profilemanager.exception.UserNotFoundException;
import com.sam.profilemanager.model.User;
import com.sam.profilemanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private UserService service;

    @Test
    void findById_ReturnsUserResponse() {
        User user = new User("Alex", "alex@example.com", 25);
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = service.findById(1L);

        assertEquals("Alex", response.name());
        assertEquals("alex@example.com", response.email());
        assertEquals(25, response.age());
        verify(repository).findById(1L);
    }

    @Test
    void findById_ThrowsExceptionWhenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> service.findById(1L));

        assertEquals("User not found: 1", ex.getMessage());
    }

    @Test
    void findAll_ReturnsListOfResponses() {
        List<User> users = List.of(
                new User("Alex", "alex@example.com", 25),
                new User("Maria", "maria@example.com", 30));
        when(repository.findAll()).thenReturn(users);

        List<UserResponse> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals("Alex", result.get(0).name());
        assertEquals("Maria", result.get(1).name());
    }

    @Test
    void create_SavesAndPublishesCreateEvent() {
        UserRequest request = new UserRequest("Alex", "alex@example.com", 25);
        User savedUser = new User("Alex", "alex@example.com", 25);

        when(repository.existsByEmail(request.email())).thenReturn(false);
        when(repository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = service.create(request);

        assertNotNull(response);
        assertEquals("Alex", response.name());
        verify(repository).save(any(User.class));
        verify(events).publishEvent(new UserEvent(UserEvent.CREATE, "alex@example.com"));
    }

    @Test
    void create_ThrowsExceptionWhenEmailExists() {
        UserRequest request = new UserRequest("Alex", "alex@example.com", 25);
        when(repository.existsByEmail(request.email())).thenReturn(true);

        EmailAlreadyInUseException ex = assertThrows(EmailAlreadyInUseException.class,
                () -> service.create(request));

        assertEquals("Email already in use: alex@example.com", ex.getMessage());
        verify(repository, never()).save(any(User.class));
        verifyNoInteractions(events);
    }

    @Test
    void update_UpdatesExistingUser() {
        UserRequest request = new UserRequest("Alexander", "alex@example.com", 26);
        User existingUser = new User("Alex", "alex@example.com", 25);

        when(repository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(repository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = service.update(1L, request);

        assertEquals("Alexander", response.name());
        assertEquals(26, response.age());
        verify(repository).saveAndFlush(existingUser);
    }

    @Test
    void update_ThrowsExceptionWhenEmailTakenByAnotherUser() {
        UserRequest request = new UserRequest("Maria", "alex@example.com", 30);
        User existingUser = new User("Maria", "maria@example.com", 30);
        User conflictingUser = new User("Alex", "alex@example.com", 25);

        when(repository.findById(2L)).thenReturn(Optional.of(existingUser));
        when(repository.findByEmail(request.email())).thenReturn(Optional.of(conflictingUser));

        EmailAlreadyInUseException ex = assertThrows(EmailAlreadyInUseException.class,
                () -> service.update(2L, request));

        assertEquals("Email already in use: alex@example.com", ex.getMessage());
        verify(repository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void delete_DeletesUserAndPublishesDeleteEvent() {
        User user = new User("Alex", "alex@example.com", 25);
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        service.delete(1L);

        verify(repository).delete(user);
        verify(events).publishEvent(new UserEvent(UserEvent.DELETE, "alex@example.com"));
    }

    @Test
    void delete_ThrowsExceptionWhenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> service.delete(1L));

        assertEquals("User not found: 1", ex.getMessage());
        verify(repository, never()).delete(any(User.class));
        verifyNoInteractions(events);
    }
}
