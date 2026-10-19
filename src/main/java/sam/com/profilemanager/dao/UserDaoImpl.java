package sam.com.profilemanager.dao;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sam.com.profilemanager.model.User;
import sam.com.profilemanager.util.HibernateUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class UserDaoImpl implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);

    @Override
    public void create(User user) {
        inTransaction(s -> {
            s.persist(user);
            return null;
        });
        log.info("Created user id={}", user.getId());
    }

    @Override
    public Optional<User> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.setDefaultReadOnly(true);
            return Optional.ofNullable(session.get(User.class, id));
        } catch (HibernateException e) {
            log.error("Database error on findById id={}", id, e);
            throw new DaoException("Database error on findById id=" + id, e);
        }
    }

    @Override
    public List<User> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.setDefaultReadOnly(true);
            return session.createQuery("from User order by id", User.class).list();
        } catch (HibernateException e) {
            log.error("Database error on findAll", e);
            throw new DaoException("ОDatabase error on findAll", e);
        }
    }

    @Override
    public User update(User user) {
        User merged = inTransaction(s -> s.merge(user));
        log.info("Updated user id={}", merged.getId());
        return merged;
    }

    @Override
    public boolean delete(Long id) {
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

    private <T> T inTransaction(Function<Session, T> work) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            T result = work.apply(session);
            tx.commit();
            return result;
        } catch (HibernateException e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException re) {
                    log.error("Rollback failed", re);
                }
            }
            log.error("Database error", e);
            throw new DaoException("Database error", e);
        }
    }
}