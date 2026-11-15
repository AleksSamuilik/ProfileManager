-- Схема БД ProfileManager (PostgreSQL).
-- Приложение работает с hibernate.ddl-auto=validate и таблицы само не создаёт,
-- поэтому этот скрипт нужно выполнить один раз вручную:
--   psql -U postgres -d profilemanager -f user-service/src/main/resources/Schema.sql
-- (docker-compose.yml выполняет его автоматически при первом запуске контейнера).

-- Sequence должна совпадать с @SequenceGenerator в User (sequenceName = "user_sequence", allocationSize = 1)
CREATE SEQUENCE IF NOT EXISTS user_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT       NOT NULL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    age        INTEGER      NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0
);
