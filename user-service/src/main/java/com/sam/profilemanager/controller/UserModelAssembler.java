package com.sam.profilemanager.controller;

import com.sam.profilemanager.dto.UserModel;
import com.sam.profilemanager.dto.UserResponse;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Превращает UserResponse в UserModel и добавляет ссылки:
 * <ul>
 *     <li>{@code self} — сам пользователь (GET);</li>
 *     <li>{@code update} — изменить пользователя (PUT на тот же адрес);</li>
 *     <li>{@code delete} — удалить пользователя (DELETE на тот же адрес);</li>
 *     <li>{@code users} — список всех пользователей.</li>
 * </ul>
 */
@Component
public class UserModelAssembler implements RepresentationModelAssembler<UserResponse, UserModel> {

    @Override
    public UserModel toModel(UserResponse user) {
        UserModel model = new UserModel(user);
        model.add(linkTo(methodOn(UserController.class).getUserById(user.id())).withSelfRel());
        model.add(linkTo(methodOn(UserController.class).updateUser(user.id(), null)).withRel("update"));
        model.add(linkTo(methodOn(UserController.class).deleteUser(user.id())).withRel("delete"));
        model.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("users"));
        return model;
    }
}
