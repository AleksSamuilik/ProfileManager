package com.sam.profilemanager.dao;

import jakarta.persistence.PersistenceException;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sam.profilemanager.model.User;
import com.sam.profilemanager.util.HibernateUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class UserDaoImpl implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);

    private final Supplier<SessionFactory> sessionFactory;

    public UserDaoImpl() {
        this(HibernateUtil::getSessionFactory);
    }

    public UserDaoImpl(SessionFactory sessionFactory) {
        this(() -> sessionFactory);
    }

    private UserDaoImpl(Supplier<SessionFactory> sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public User create(User user) {
        Objects.requireNonNull(user, "user");
        inTransaction(s -> {
            s.persist(user);
            return null;
        });
        log.info("Created user id={}", user.getId());
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        Objects.requireNonNull(id, "id");
        return inReadOnlySession(s -> Optional.ofNullable(s.get(User.class, id)));
    }

    @Override
    public List<User> findAll() {
        return inReadOnlySession(s -> s.createQuery("from User order by id", User.class).list());
    }

    @Override
    public User update(User user) {
        Objects.requireNonNull(user, "user");
        User merged = inTransaction(s -> s.merge(user));
        log.info("Updated user id={}", merged.getId());
        return merged;
    }

    @Override
    public boolean delete(Long id) {
        Objects.requireNonNull(id, "id");
        boolean deleted = inTransaction(s -> {
            User user = s.get(User.class, id);
            if (user == null) {
                return false;
            }
            s.remove(user);
            return true;
        });
        if (deleted) {
            log.info("Deleted user id={}", id);
        }
        return deleted;
    }

    private <T> T inReadOnlySession(Function<Session, T> work) {
        try (Session session = sessionFactory.get().openSession()) {
            session.setDefaultReadOnly(true);
            session.setHibernateFlushMode(FlushMode.MANUAL);
            return work.apply(session);
        } catch (PersistenceException e) {
            log.error("Database error on read", e);
            throw new DaoException("Database error on read", e);
        }
    }

    private <T> T inTransaction(Function<Session, T> work) {
        try (Session session = sessionFactory.get().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                T result = work.apply(session);
                tx.commit();
                return result;
            } catch (RuntimeException e) {
                rollbackQuietly(tx);
                throw e;
            }
        } catch (PersistenceException e) {
            log.error("Database error", e);
            throw new DaoException("Database error", e);
        }
    }

    private void rollbackQuietly(Transaction tx) {
        try {
            if (tx.isActive()) {
                tx.rollback();
            }
        } catch (RuntimeException re) {
            log.error("Rollback failed", re);
        }
    }
}