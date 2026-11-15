-- Схема БД ProfileManager (PostgreSQL).
-- Приложение работает с hibernate.hbm2ddl.auto=validate и таблицы само не создаёт,
-- поэтому этот скрипт нужно выполнить один раз вручную:
--   psql -U postgres -d profilemanager -f src/main/resources/Schema.sql

CREATE TABLE IF NOT EXISTS users
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    age        INTEGER      NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0
);

-- Миграция для уже существующей таблицы (если она была создана до появления колонки version):
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
