package com.sam.profilemanager.repository;

import com.sam.profilemanager.AbstractIntegrationTest;
import com.sam.profilemanager.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void saveAndFindById() {
        User user = new User("Alex", "alex@example.com", 25);

        User saved = repository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0);

        Optional<User> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alex@example.com");
    }

    @Test
    void findByEmail() {
        repository.save(new User("Alex", "alex@example.com", 25));

        Optional<User> found = repository.findByEmail("alex@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alex");
    }

    @Test
    void existsByEmail() {
        repository.save(new User("Alex", "alex@example.com", 25));

        assertThat(repository.existsByEmail("alex@example.com")).isTrue();
        assertThat(repository.existsByEmail("other@example.com")).isFalse();
    }

    @Test
    void findAllReturnsAllUsersOrderedById() {
        repository.save(new User("Alex", "alex@example.com", 25));
        repository.save(new User("Maria", "maria@example.com", 30));

        List<User> users = repository.findAll();

        assertThat(users).hasSize(2);
        assertThat(users.get(0).getName()).isEqualTo("Alex");
        assertThat(users.get(1).getName()).isEqualTo("Maria");
    }

    @Test
    void updateChangesUserData() {
        User saved = repository.save(new User("Alex", "alex@example.com", 25));

        saved.setName("Alexander");
        saved.setAge(26);
        User updated = repository.saveAndFlush(saved);

        assertThat(updated.getVersion()).isEqualTo(1);

        User found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Alexander");
        assertThat(found.getAge()).isEqualTo(26);
    }

    @Test
    void deleteRemovesUser() {
        User saved = repository.save(new User("Alex", "alex@example.com", 25));

        repository.deleteById(saved.getId());

        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    void saveThrowsExceptionOnDuplicateEmail() {
        repository.saveAndFlush(new User("Alex", "alex@example.com", 25));
        User duplicate = new User("Maria", "alex@example.com", 30);

        assertThrows(Exception.class, () -> repository.saveAndFlush(duplicate));
    }
}