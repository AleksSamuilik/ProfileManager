package sam.com.profilemanager.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sam.com.profilemanager.model.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class UserDaoImplIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("profilemanager")
                    .withUsername("postgres")
                    .withPassword("postgres");

    private static SessionFactory sessionFactory;
    private UserDaoImpl dao;

    @BeforeAll
    static void initSessionFactory() {
        Configuration configuration = new Configuration()
                .addAnnotatedClass(User.class)
                .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                .setProperty("hibernate.connection.url", POSTGRES.getJdbcUrl())
                .setProperty("hibernate.connection.username", POSTGRES.getUsername())
                .setProperty("hibernate.connection.password", POSTGRES.getPassword())
                .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "false");
        sessionFactory = configuration.buildSessionFactory();
    }

    @AfterAll
    static void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @BeforeEach
    void setUp() {
        dao = new UserDaoImpl(sessionFactory);
        try (Session session = sessionFactory.openSession()) {
            var tx = session.beginTransaction();
            session.createMutationQuery("delete from User").executeUpdate();
            tx.commit();
        }
    }


    @Test
    void createPersistsUser() {
        User user = new User("Alex", "alex@example.com", 25);

        User created = dao.create(user);

        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());
        assertEquals(0L, created.getVersion());
    }

    @Test
    void createWithDuplicateEmailThrowsDaoException() {
        dao.create(new User("Alex", "alex@example.com", 25));

        DaoException ex = assertThrows(DaoException.class,
                () -> dao.create(new User("Maria", "alex@example.com", 30)));

        assertNotNull(ex.getCause());
    }


    @Test
    void findByIdExistingUser() {
        User created = dao.create(new User("Alex", "alex@example.com", 25));

        Optional<User> found = dao.findById(created.getId());

        assertTrue(found.isPresent());
        assertEquals("Alex", found.get().getName());
        assertEquals("alex@example.com", found.get().getEmail());
        assertEquals(25, found.get().getAge());
    }

    @Test
    void findByIdUserDoesNotExist() {
        assertTrue(dao.findById(-1L).isEmpty());
    }

    @Test
    void findByIdNullId() {
        assertThrows(NullPointerException.class, () -> dao.findById(null));
    }


    @Test
    void findAllOrderedById() {
        dao.create(new User("Alex", "alex@example.com", 25));
        dao.create(new User("Maria", "maria@example.com", 30));

        List<User> users = dao.findAll();

        assertEquals(2, users.size());
        assertEquals("Alex", users.get(0).getName());
        assertEquals("Maria", users.get(1).getName());
        assertTrue(users.get(0).getId() < users.get(1).getId());
    }

    @Test
    void findAllNoUsers() {
        assertTrue(dao.findAll().isEmpty());
    }


    @Test
    void updateChangesUserData() {
        User created = dao.create(new User("Alex", "alex@example.com", 25));
        created.setName("Alexander");
        created.setAge(26);

        User updated = dao.update(created);

        assertEquals(created.getId(), updated.getId());
        User found = dao.findById(created.getId()).orElseThrow();
        assertEquals("Alexander", found.getName());
        assertEquals(26, found.getAge());
        assertEquals(1L, found.getVersion());
    }

    @Test
    void updateWithStaleVersionThrowsDaoException() {
        User created = dao.create(new User("Alex", "alex@example.com", 25));
        User first = dao.findById(created.getId()).orElseThrow();
        User second = dao.findById(created.getId()).orElseThrow();

        first.setName("First");
        dao.update(first);

        second.setName("Second");
        assertThrows(DaoException.class, () -> dao.update(second));
    }

    @Test
    void updateNullUser() {
        assertThrows(NullPointerException.class, () -> dao.update(null));
    }


    @Test
    void deleteExistingUser() {
        User created = dao.create(new User("Alex", "alex@example.com", 25));

        assertTrue(dao.delete(created.getId()));
        assertTrue(dao.findById(created.getId()).isEmpty());
    }

    @Test
    void deleteUserDoesNotExist() {
        assertFalse(dao.delete(-1L));
    }

    @Test
    void deleteNullId() {
        assertThrows(NullPointerException.class, () -> dao.delete(null));
    }


    @Test
    void testsAreIsolatedFromEachOther() {
        assertEquals(0, dao.findAll().size());
    }
}