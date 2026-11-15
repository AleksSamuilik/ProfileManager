# ProfileManager

Project team: Yana Charnaya, Aliaksei Samuilik

Учебный микросервисный проект на Spring Boot 3.3 / Java 17 (Maven multi-module).

| Модуль | Порт | Назначение |
|---|---|---|
| `user-service` | 8080 | CRUD пользователей (`/api/users`), PostgreSQL. При создании и удалении пользователя шлёт событие в Kafka |
| `notification-service` | 8081 | Читает события из Kafka и отправляет пользователю письмо. Также имеет REST API для прямой отправки |

## Как это работает

```
POST/DELETE /api/users ──> user-service ──(после коммита)──> Kafka топик user-events
                                                                   │  {"operation":"CREATE|DELETE","email":"..."}
                                                                   ▼
                                                          notification-service ──SMTP──> письмо пользователю
```

Тексты писем:

- создание: «Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан.» (название сайта — `notification.site-name` / переменная `SITE_NAME`);
- удаление: «Здравствуйте! Ваш аккаунт был удалён.»

## Запуск

```bash
docker compose up -d                       # PostgreSQL, Kafka, Mailpit (SMTP + веб-интерфейс на :8025)
cp user-service/.env.example user-service/.env   # пароль БД (по умолчанию change_me)
cd user-service && mvn spring-boot:run           # терминал 1 (.env читается из текущей папки)
cd notification-service && mvn spring-boot:run   # терминал 2
```

> Если пароль БД не задан в `.env`, передайте его переменной окружения `PROFILEMANAGER_DB_PASSWORD`.

Проверка:

```bash
curl -X POST localhost:8080/api/users -H 'Content-Type: application/json' \
     -d '{"name":"Alex","email":"alex@example.com","age":25}'
# письмо появится на http://localhost:8025

curl -X DELETE localhost:8080/api/users/1
```

### REST API notification-service

`POST /api/notifications` — отправить письмо напрямую (без Kafka):

```bash
curl -X POST localhost:8081/api/notifications -H 'Content-Type: application/json' \
     -d '{"email":"alex@example.com","operation":"CREATE"}'
```

`operation` — `CREATE` или `DELETE`. Ошибки: `400` — невалидный email/операция, `502` — SMTP недоступен.

### Настройка почты (notification-service)

Переменные окружения: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`,
`MAIL_STARTTLS`, `MAIL_FROM`, `KAFKA_BOOTSTRAP_SERVERS`. По умолчанию письма уходят на локальный Mailpit.

## Тесты

```bash
mvn test
```

- `notification-service` — интеграционные тесты не требуют Docker: настоящий SMTP-сервер в памяти (GreenMail) и
  встроенная Kafka. Проверяются письма по событиям `CREATE`/`DELETE` из Kafka и через REST API.
- `user-service` — интеграционные тесты используют PostgreSQL в Testcontainers (нужен Docker);
  без Docker все тесты запускаются на in-memory H2 специальной командой:
  `mvn test -Ph2` (или `mvn test -Dh2`; для одного модуля — `mvn -pl user-service test -Ph2`).
  Kafka в этих тестах замокана, проверяется отправка события.
