package com.sam.profilemanager.util;

import com.sam.profilemanager.model.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class HibernateUtil {

    private static final Logger log = LoggerFactory.getLogger(HibernateUtil.class);

    static final String PASSWORD_ENV = "PROFILEMANAGER_DB_PASSWORD";
    private static final String PASSWORD_PROPERTY = "hibernate.connection.password";
    private static final Path DOT_ENV = Path.of(".env");

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

    static void fillFromEnv(Configuration configuration, String property, String envName) {
        if (configuration.getProperty(property) != null) {
            return;
        }
        String value = System.getenv(envName);
        if (value == null) {
            value = readDotEnv().get(envName);
        }
        if (value != null && !value.isBlank()) {
            configuration.setProperty(property, value);
        } else {
            log.warn("Environment variable {} is not set; database password will be empty", envName);
        }
    }

    static Map<String, String> readDotEnv() {
        return readDotEnv(DOT_ENV);
    }

    static Map<String, String> readDotEnv(Path path) {
        Map<String, String> result = new HashMap<>();
        if (path == null || !Files.isRegularFile(path)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(path)) {   // ← path, не DOT_ENV
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                result.put(key, value);
            }
        } catch (IOException e) {
            log.warn("Failed to read {}: {}", path, e.getMessage());
        }
        return result;
    }
}