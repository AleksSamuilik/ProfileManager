package sam.com.profilemanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sam.com.profilemanager.dao.UserDao;
import sam.com.profilemanager.model.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao dao;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(dao);
    }

    @Test
    void createPassesValidatedUserToDao() {
        when(dao.create(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.create("  Alex  ", "  alex@example.com  ", 25);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(dao).create(captor.capture());
        User passed = captor.getValue();
        assertEquals("Alex", passed.getName());
        assertEquals("alex@example.com", passed.getEmail());
        assertEquals(25, passed.getAge());
        assertSame(passed, result);
    }

    @Test
    void createDaoReturned() {
        User persisted = new User("Alex", "alex@example.com", 25);
        when(dao.create(any(User.class))).thenReturn(persisted);

        User result = service.create("Alex", "alex@example.com", 25);

        assertSame(persisted, result);
    }

    @Test
    void createEmptyName() {
        assertThrows(ValidationException.class,
                () -> service.create("   ", "alex@example.com", 25));
        verifyNoInteractions(dao);
    }

    @Test
    void createNullName() {
        assertThrows(ValidationException.class,
                () -> service.create(null, "alex@example.com", 25));
        verifyNoInteractions(dao);
    }

    @Test
    void createLongName() {
        String longName = "a".repeat(UserService.MAX_TEXT_LENGTH + 1);
        assertThrows(ValidationException.class,
                () -> service.create(longName, "alex@example.com", 25));
        verifyNoInteractions(dao);
    }

    @Test
    void createEmptyEmail() {
        assertThrows(ValidationException.class,
                () -> service.create("Alex", "", 25));
        verifyNoInteractions(dao);
    }

    @Test
    void createInvalidEmail() {
        assertThrows(ValidationException.class,
                () -> service.create("Alex", "not-an-email", 25));
        verifyNoInteractions(dao);
    }

    @Test
    void createEmailWithDoubleDot() {
        assertThrows(ValidationException.class,
                () -> service.create("Alex", "a@b..com", 25));
        verifyNoInteractions(dao);
    }

    @Test
    void createLongEmail() {
        String longEmail = "a".repeat(UserService.MAX_TEXT_LENGTH) + "@example.com";
        assertThrows(ValidationException.class,
                () -> service.create("Alex", longEmail, 25));
        verifyNoInteractions(dao);
    }

    @Test
    void createNegativeAge() {
        assertThrows(ValidationException.class,
                () -> service.create("Alex", "alex@example.com", -1));
        verifyNoInteractions(dao);
    }

    @Test
    void createLargeAge() {
        assertThrows(ValidationException.class,
                () -> service.create("Alex", "alex@example.com", UserService.MAX_AGE + 1));
        verifyNoInteractions(dao);
    }

    @Test
    void createBoundaryAge() {
        when(dao.create(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.create("Alex", "alex@example.com", UserService.MIN_AGE));
        assertDoesNotThrow(() -> service.create("Alex", "alex@example.com", UserService.MAX_AGE));
    }

    @Test
    void findByIdDelegatesToDao() {
        User user = new User("Alex", "alex@example.com", 25);
        when(dao.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = service.findById(1L);

        assertTrue(result.isPresent());
        assertSame(user, result.get());
        verify(dao).findById(1L);
    }

    @Test
    void findByIdNullId() {
        assertThrows(NullPointerException.class, () -> service.findById(null));
        verifyNoInteractions(dao);
    }

    @Test
    void findByIdDaoReturnsEmpty() {
        when(dao.findById(1L)).thenReturn(Optional.empty());

        assertTrue(service.findById(1L).isEmpty());
    }

    @Test
    void findAllDelegatesToDao() {
        List<User> users = List.of(
                new User("Alex", "alex@example.com", 25),
                new User("Maria", "maria@example.com", 30));
        when(dao.findAll()).thenReturn(users);

        assertSame(users, service.findAll());
        verify(dao).findAll();
    }

    @Test
    void updateArgsAreNull() {
        User existing = new User("Alex", "alex@example.com", 25);
        when(dao.update(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.update(existing, null, null, null);

        assertEquals("Alex", result.getName());
        assertEquals("alex@example.com", result.getEmail());
        assertEquals(25, result.getAge());
        verify(dao).update(existing);
    }

    @Test
    void updateAppliesNewValues() {
        User existing = new User("Alex", "alex@example.com", 25);
        when(dao.update(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.update(existing, "Alexander", "alexander@example.com", 26);

        assertEquals("Alexander", result.getName());
        assertEquals("alexander@example.com", result.getEmail());
        assertEquals(26, result.getAge());
    }

    @Test
    void updateValidatesNewValues() {
        User existing = new User("Alex", "alex@example.com", 25);

        assertThrows(ValidationException.class,
                () -> service.update(existing, "   ", null, null));
        assertThrows(ValidationException.class,
                () -> service.update(existing, null, "bad-email", null));
        assertThrows(ValidationException.class,
                () -> service.update(existing, null, null, -5));

        verify(dao, never()).update(any(User.class));
    }

    @Test
    void updateNullUser() {
        assertThrows(NullPointerException.class,
                () -> service.update(null, "Alex", "alex@example.com", 25));
        verifyNoInteractions(dao);
    }

    @Test
    void deleteDao() {
        when(dao.delete(1L)).thenReturn(true);

        assertTrue(service.delete(1L));
        verify(dao).delete(1L);
    }

    @Test
    void deleteNullId() {
        assertThrows(NullPointerException.class, () -> service.delete(null));
        verifyNoInteractions(dao);
    }

    @Test
    void deleteDaoReturnsFalse() {
        when(dao.delete(1L)).thenReturn(false);

        assertFalse(service.delete(1L));
    }
}