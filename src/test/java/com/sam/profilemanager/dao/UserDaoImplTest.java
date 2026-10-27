package com.sam.profilemanager.dao;

import com.sam.profilemanager.model.User;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sam.profilemanager.util.HibernateUtil;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoImplTest {

    private UserDao userDao;

    @BeforeEach
    void setUp() {
        userDao = new UserDaoImpl();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            var transaction = session.beginTransaction();
            session.createMutationQuery("delete from User").executeUpdate();
            transaction.commit();
        }
    }

    @AfterAll
    static void closeDatabase() {
        HibernateUtil.shutdown();
    }

    @Test
    void createAndFindById() {
        User user = new User("Alex", "alex@example.com", 25);

        userDao.create(user);

        assertNotNull(user.getId());

        Optional<User> result = userDao.findById(user.getId());

        assertTrue(result.isPresent());
        assertEquals("Alex", result.get().getName());
        assertEquals("alex@example.com", result.get().getEmail());
        assertEquals(25, result.get().getAge());
    }

    @Test
    void findByIdReturnsEmptyWhenUserDoesNotExist() {
        assertTrue(userDao.findById(-1L).isEmpty());
    }

    @Test
    void findAllReturnsAllUsers() {
        userDao.create(new User("Alex", "alex@example.com", 25));
        userDao.create(new User("Maria", "maria@example.com", 30));

        List<User> users = userDao.findAll();

        assertEquals(2, users.size());
        assertEquals("Alex", users.get(0).getName());
        assertEquals("Maria", users.get(1).getName());
    }

    @Test
    void updateChangesUserData() {
        User user = new User("Alex", "alex@example.com", 25);
        userDao.create(user);

        user.setName("Alexander");
        user.setAge(26);

        User updated = userDao.update(user);

        assertEquals(user.getId(), updated.getId());

        User found = userDao.findById(user.getId()).orElseThrow();
        assertEquals("Alexander", found.getName());
        assertEquals(26, found.getAge());
    }

    @Test
    void deleteRemovesExistingUser() {
        User user = new User("Alex", "alex@example.com", 25);
        userDao.create(user);

        assertTrue(userDao.delete(user.getId()));
        assertTrue(userDao.findById(user.getId()).isEmpty());
    }

    @Test
    void deleteReturnsFalseWhenUserDoesNotExist() {
        assertFalse(userDao.delete(-1L));
    }

    @Test
    void createFillsCreatedAt() {
        User user = new User("Alex", "alex@example.com", 25);

        userDao.create(user);

        assertNotNull(userDao.findById(user.getId()).orElseThrow().getCreatedAt());
    }
}