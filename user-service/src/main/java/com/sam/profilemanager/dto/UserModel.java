package com.sam.profilemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * HATEOAS-представление пользователя: данные пользователя + ссылки (_links) на связанные действия.
 */
@Relation(collectionRelation = "users", itemRelation = "user")
@Schema(name = "User", description = "Пользователь со ссылками для навигации (HAL)")
public class UserModel extends RepresentationModel<UserModel> {

    @Schema(description = "Идентификатор", example = "1")
    private final Long id;

    @Schema(description = "Имя", example = "Alex")
    private final String name;

    @Schema(description = "Email (уникальный)", example = "alex@example.com")
    private final String email;

    @Schema(description = "Возраст", example = "25")
    private final int age;

    @Schema(description = "Дата и время создания", example = "2026-11-16T10:15:30")
    private final LocalDateTime createdAt;

    @Schema(description = "Версия записи (оптимистичная блокировка)", example = "0")
    private final Long version;

    public UserModel(UserResponse user) {
        this.id = user.id();
        this.name = user.name();
        this.email = user.email();
        this.age = user.age();
        this.createdAt = user.createdAt();
        this.version = user.version();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public int getAge() {
        return age;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserModel other) || !super.equals(o)) {
            return false;
        }
        return age == other.age
                && Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(email, other.email)
                && Objects.equals(createdAt, other.createdAt)
                && Objects.equals(version, other.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, name, email, age, createdAt, version);
    }
}
