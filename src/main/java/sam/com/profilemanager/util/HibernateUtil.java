package sam.com.profilemanager.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import sam.com.profilemanager.model.User;

public final class HibernateUtil {

    static final String PASSWORD_ENV = "PROFILEMANAGER_DB_PASSWORD";
    private static final String PASSWORD_PROPERTY = "hibernate.connection.password";

    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            Configuration configuration = new Configuration().addAnnotatedClass(User.class);
            fillFromEnv(configuration, PASSWORD_PROPERTY, PASSWORD_ENV);
            sessionFactory = configuration.buildSessionFactory();
        }
        return sessionFactory;
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }

    private static void fillFromEnv(Configuration configuration, String property, String envName) {
        if (configuration.getProperty(property) != null) {
            return;
        }
        String value = System.getenv(envName);
        if (value != null) {
            configuration.setProperty(property, value);
        }
    }
}