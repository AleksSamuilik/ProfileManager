package com.sam.profilemanager.dao;

import com.sam.profilemanager.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {
    User create(User user);

    Optional<User> findById(Long id);

    List<User> findAll();

    User update(User user);

    boolean delete(Long id);
}