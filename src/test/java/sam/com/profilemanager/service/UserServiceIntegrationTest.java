package sam.com.profilemanager.service;

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
import sam.com.profilemanager.dao.DaoException;
import sam.com.profilemanager.dao.UserDaoImpl;
import sam.com.profilemanager.model.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class UserServiceIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("profilemanager")
                    .withUsername("postgres")
                    .withPassword("postgres");

    private static SessionFactory sessionFactory;
    private UserService service;

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
        service = new UserService(new UserDaoImpl(sessionFactory));
        try (Session session = sessionFactory.openSession()) {
            var tx = session.beginTransaction();
            session.createMutationQuery("delete from User").executeUpdate();
            tx.commit();
        }
    }

    @Test
    void createPersistsUserThroughRealDao() {
        User created = service.create("Alex", "alex@example.com", 25);

        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());

        Optional<User> found = service.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("Alex", found.get().getName());
    }

    @Test
    void createNormalizesWhitespaceBeforePersisting() {
        User created = service.create("  Alex  ", "  alex@example.com  ", 25);

        User found = service.findById(created.getId()).orElseThrow();
        assertEquals("Alex", found.getName());
        assertEquals("alex@example.com", found.getEmail());
    }

    @Test
    void createRejectsInvalidDataBeforeTouchingDatabase() {
        assertThrows(ValidationException.class,
                () -> service.create("   ", "alex@example.com", 25));
        assertThrows(ValidationException.class,
                () -> service.create("Alex", "bad-email", 25));
        assertThrows(ValidationException.class,
                () -> service.create("Alex", "alex@example.com", -1));

        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void createWithDuplicateEmailSurfacesDaoExceptionFromRealConstraint() {
        service.create("Alex", "alex@example.com", 25);

        assertThrows(DaoException.class,
                () -> service.create("Maria", "alex@example.com", 30));

        assertEquals(1, service.findAll().size());
    }

    @Test
    void updateAppliesValidatedChanges() {
        User created = service.create("Alex", "alex@example.com", 25);

        User updated = service.update(created, "Alexander", null, 26);

        User found = service.findById(created.getId()).orElseThrow();
        assertEquals("Alexander", found.getName());
        assertEquals("alex@example.com", found.getEmail());
        assertEquals(26, found.getAge());
        assertEquals(1L, updated.getVersion());
    }

    @Test
    void updateWithDuplicateEmailThrowsDaoException() {
        service.create("Alex", "alex@example.com", 25);
        User maria = service.create("Maria", "maria@example.com", 30);

        assertThrows(DaoException.class,
                () -> service.update(maria, null, "alex@example.com", null));
    }

    @Test
    void updateRejectsInvalidDataBeforeTouchingDatabase() {
        User created = service.create("Alex", "alex@example.com", 25);

        assertThrows(ValidationException.class,
                () -> service.update(created, "   ", null, null));
        assertThrows(ValidationException.class,
                () -> service.update(created, null, "bad-email", null));
        assertThrows(ValidationException.class,
                () -> service.update(created, null, null, -5));

        User found = service.findById(created.getId()).orElseThrow();
        assertEquals("Alex", found.getName());
        assertEquals("alex@example.com", found.getEmail());
        assertEquals(25, found.getAge());
    }

    @Test
    void deleteRemovesUserThroughRealDao() {
        User created = service.create("Alex", "alex@example.com", 25);

        assertTrue(service.delete(created.getId()));
        assertTrue(service.findById(created.getId()).isEmpty());
    }

    @Test
    void deleteReturnsFalseWhenUserDoesNotExist() {
        assertFalse(service.delete(-1L));
    }
}