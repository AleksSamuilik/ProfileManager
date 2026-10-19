package sam.com.profilemanager.dao;

import sam.com.profilemanager.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {

    void create(User user);

    Optional<User> findById(Long id);

    List<User> findAll();

    void update(User user);

    boolean delete(Long id);
}