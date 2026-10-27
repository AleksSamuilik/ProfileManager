package sam.com.profilemanager.service;

import sam.com.profilemanager.dao.UserDao;
import sam.com.profilemanager.model.User;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserService {

    static final int MAX_TEXT_LENGTH = 255;
    static final int MIN_AGE = 0;
    static final int MAX_AGE = 150;

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$");

    private final UserDao dao;

    public UserService(UserDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    public User create(String name, String email, int age) {
        User user = new User(validName(name), validEmail(email), validAge(age));
        return dao.create(user);
    }

    public Optional<User> findById(Long id) {
        return dao.findById(Objects.requireNonNull(id, "id"));
    }

    public List<User> findAll() {
        return dao.findAll();
    }

    public User update(User user, String name, String email, Integer age) {
        Objects.requireNonNull(user, "user");
        String newName = name == null ? user.getName() : validName(name);
        String newEmail = email == null ? user.getEmail() : validEmail(email);
        int newAge = age == null ? user.getAge() : validAge(age);

        user.setName(newName);
        user.setEmail(newEmail);
        user.setAge(newAge);
        return dao.update(user);
    }

    public boolean delete(Long id) {
        return dao.delete(Objects.requireNonNull(id, "id"));
    }

    private static String validName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            throw new ValidationException("Name must not be empty");
        }
        if (value.length() > MAX_TEXT_LENGTH) {
            throw new ValidationException("Name is too long (max " + MAX_TEXT_LENGTH + " characters)");
        }
        return value;
    }

    private static String validEmail(String email) {
        String value = email == null ? "" : email.trim();
        if (value.isEmpty()) {
            throw new ValidationException("Email must not be empty");
        }
        if (value.length() > MAX_TEXT_LENGTH) {
            throw new ValidationException("Email is too long (max " + MAX_TEXT_LENGTH + " characters)");
        }
        if (!EMAIL.matcher(value).matches()) {
            throw new ValidationException("Email is invalid");
        }
        return value;
    }

    private static int validAge(int age) {
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new ValidationException("Age must be between " + MIN_AGE + " and " + MAX_AGE);
        }
        return age;
    }
}
