package com.sam.profilemanager.util;

import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HibernateUtilTest {

    @Test
    void readDotEnvParsesSimpleKeyValue(@TempDir Path dir) throws IOException {
        Path env = dir.resolve(".env");
        Files.writeString(env, "PROFILEMANAGER_DB_PASSWORD=secret\n");

        Map<String, String> result = HibernateUtil.readDotEnv(env);

        assertEquals(Map.of("PROFILEMANAGER_DB_PASSWORD", "secret"), result);
    }

    @Test
    void readDotEnvSkipsCommentsAndBlankLines(@TempDir Path dir) throws IOException {
        Path env = dir.resolve(".env");
        Files.writeString(env, """                              
                PROFILEMANAGER_DB_PASSWORD=secret
                # another comment
                FOO=bar
                """);

        Map<String, String> result = HibernateUtil.readDotEnv(env);

        assertEquals(Map.of(
                "PROFILEMANAGER_DB_PASSWORD", "secret",
                "FOO", "bar"), result);
    }

    @Test
    void readDotEnvTrimsWhitespace(@TempDir Path dir) throws IOException {
        Path env = dir.resolve(".env");
        Files.writeString(env, "  KEY  =  value with spaces  \n");

        Map<String, String> result = HibernateUtil.readDotEnv(env);

        assertEquals("value with spaces", result.get("KEY"));
    }

    @Test
    void readDotEnvStripsDoubleQuotes(@TempDir Path dir) throws IOException {
        Path env = dir.resolve(".env");
        Files.writeString(env, "KEY=\"quoted value\"\n");

        assertEquals("quoted value", HibernateUtil.readDotEnv(env).get("KEY"));
    }

    @Test
    void readDotEnvStripsSingleQuotes(@TempDir Path dir) throws IOException {
        Path env = dir.resolve(".env");
        Files.writeString(env, "KEY='quoted value'\n");

        assertEquals("quoted value", HibernateUtil.readDotEnv(env).get("KEY"));
    }

    @Test
    void readDotEnvKeepsValueWithEqualsSign(@TempDir Path dir) throws IOException {
        Path env = dir.resolve(".env");
        Files.writeString(env, "KEY=abc=def=ghi\n");

        assertEquals("abc=def=ghi", HibernateUtil.readDotEnv(env).get("KEY"));
    }

    @Test
    void readDotEnvIgnoresMalformedLines(@TempDir Path dir) throws IOException {
        Path env = dir.resolve(".env");
        Files.writeString(env, """
                =noKey
                NO_EQUALS
                GOOD=value
                """);

        Map<String, String> result = HibernateUtil.readDotEnv(env);

        assertEquals(Map.of("GOOD", "value"), result);
    }

    @Test
    void readDotEnvReturnsEmptyMapWhenFileMissing(@TempDir Path dir) {
        Path missing = dir.resolve("does-not-exist.env");

        assertTrue(HibernateUtil.readDotEnv(missing).isEmpty());
    }

    @Test
    void readDotEnvReturnsEmptyMapWhenPathIsNull() {
        assertTrue(HibernateUtil.readDotEnv(null).isEmpty());
    }

    @Test
    void readDotEnvSupportsEmptyValue(@TempDir Path dir) throws IOException {
        Path env = dir.resolve(".env");
        Files.writeString(env, "EMPTY=\n");

        assertEquals("", HibernateUtil.readDotEnv(env).get("EMPTY"));
    }

    @Test
    void fillFromEnvDoesNothingWhenPropertyAlreadySet() {
        Configuration configuration = new Configuration()
                .setProperty("hibernate.connection.password", "from-properties");

        HibernateUtil.fillFromEnv(configuration, "hibernate.connection.password", "NON_EXISTENT_ENV_VAR");

        assertEquals("from-properties",
                configuration.getProperty("hibernate.connection.password"));
    }
}