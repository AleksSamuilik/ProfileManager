package com.sam.profilemanager.controller;

import com.sam.profilemanager.dto.ErrorResponse;
import com.sam.profilemanager.dto.UserModel;
import com.sam.profilemanager.dto.UserRequest;
import com.sam.profilemanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Users", description = "Управление пользователями. Ответы содержат ссылки HATEOAS (_links)")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserModelAssembler assembler;

    public UserController(UserService userService, UserModelAssembler assembler) {
        this.userService = userService;
        this.assembler = assembler;
    }

    @Operation(summary = "Список пользователей",
            description = "Возвращает всех пользователей в формате HAL: массив в `_embedded.users` и ссылки в `_links`. "
                    + "Если пользователей нет, `_embedded` в ответе отсутствует.")
    @ApiResponse(responseCode = "200", description = "Список пользователей")
    @GetMapping
    public CollectionModel<UserModel> getAllUsers() {
        List<UserModel> users = userService.findAll().stream()
                .map(assembler::toModel)
                .toList();
        return CollectionModel.of(users,
                linkTo(methodOn(UserController.class).getAllUsers()).withSelfRel());
    }

    @Operation(summary = "Получить пользователя по ID")
    @ApiResponse(responseCode = "200", description = "Пользователь найден")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public UserModel getUserById(
            @Parameter(description = "ID пользователя", example = "1") @PathVariable Long id) {
        return assembler.toModel(userService.findById(id));
    }

    @Operation(summary = "Создать пользователя",
            description = "Создаёт пользователя и отправляет событие CREATE в Kafka. "
                    + "Адрес созданного ресурса возвращается в заголовке `Location`.")
    @ApiResponse(responseCode = "201", description = "Пользователь создан")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email уже используется",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<UserModel> createUser(@Valid @RequestBody UserRequest request) {
        UserModel model = assembler.toModel(userService.create(request));
        return ResponseEntity
                .created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(model);
    }

    @Operation(summary = "Обновить пользователя")
    @ApiResponse(responseCode = "200", description = "Пользователь обновлён")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email уже используется другим пользователем",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public UserModel updateUser(
            @Parameter(description = "ID пользователя", example = "1") @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        return assembler.toModel(userService.update(id, request));
    }

    @Operation(summary = "Удалить пользователя",
            description = "Удаляет пользователя и отправляет событие DELETE в Kafka.")
    @ApiResponse(responseCode = "204", description = "Пользователь удалён")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @Parameter(description = "ID пользователя", example = "1") @PathVariable Long id) {
        userService.delete(id);
    }
}
