package sam.com.profilemanager;

import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sam.com.profilemanager.dao.DaoException;
import sam.com.profilemanager.dao.UserDaoImpl;
import sam.com.profilemanager.model.User;
import sam.com.profilemanager.service.UserService;
import sam.com.profilemanager.service.ValidationException;
import sam.com.profilemanager.util.HibernateUtil;

import java.util.List;
import java.util.Scanner;

public class ProfileManagerApp {

    private static final Logger log = LoggerFactory.getLogger(ProfileManagerApp.class);

    private final UserService service;
    private final Scanner in = new Scanner(System.in);

    public ProfileManagerApp(UserService service) {
        this.service = service;
    }

    public static void main(String[] args) {
        log.info("ProfileManager started");
        try {
            new ProfileManagerApp(new UserService(new UserDaoImpl())).run();
        } catch (Exception e) {
            log.error("Fatal error", e);
            System.out.println("Fatal error: " + e.getMessage());
        } finally {
            HibernateUtil.shutdown();
            log.info("ProfileManager stopped");
        }
    }

    private void run() {
        try {
            while (true) {
                System.out.println("\n1 - Create\n2 - Find by id\n3 - List all\n4 - Update\n5 - Delete\n0 - Exit");
                String choice = ask("> ");
                try {
                    switch (choice) {
                        case "1" -> create();
                        case "2" -> find();
                        case "3" -> list();
                        case "4" -> update();
                        case "5" -> delete();
                        case "0" -> {
                            return;
                        }
                        default -> System.out.println("Unknown option");
                    }
                } catch (ValidationException e) {
                    System.out.println(e.getMessage());
                } catch (DaoException e) {
                    System.out.println(describe(e));
                }
            }
        } catch (InputClosedException e) {
            System.out.println("\nInput closed, exiting");
        }
    }

    private void create() {
        String name = ask("Name: ");
        String email = ask("Email: ");
        int age = askInt("Age: ");
        User user = service.create(name, email, age);
        System.out.println("Created: " + user);
    }

    private void find() {
        Long id = askLong("Id: ");
        service.findById(id).ifPresentOrElse(
                System.out::println,
                () -> System.out.println("Not found"));
    }

    private void list() {
        List<User> users = service.findAll();
        if (users.isEmpty()) {
            System.out.println("No users");
        }
        users.forEach(System.out::println);
    }

    private void update() {
        Long id = askLong("Id: ");
        User user = service.findById(id).orElse(null);
        if (user == null) {
            System.out.println("Not found");
            return;
        }
        String name = ask("Name [" + user.getName() + "]: ");
        String email = ask("Email [" + user.getEmail() + "]: ");
        Integer age = askOptionalInt("Age [" + user.getAge() + "]: ");
        User updated = service.update(
                user,
                name.isEmpty() ? null : name,
                email.isEmpty() ? null : email,
                age);
        System.out.println("Updated: " + updated);
    }

    private void delete() {
        Long id = askLong("Id: ");
        System.out.println(service.delete(id) ? "Deleted" : "Not found");
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) {
            throw new InputClosedException();
        }
        return in.nextLine().trim();
    }

    private int askInt(String prompt) {
        while (true) {
            String text = ask(prompt);
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Enter a whole number");
            }
        }
    }

    private Integer askOptionalInt(String prompt) {
        while (true) {
            String text = ask(prompt);
            if (text.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Enter a whole number or leave empty");
            }
        }
    }

    private Long askLong(String prompt) {
        while (true) {
            String text = ask(prompt);
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                System.out.println("Enter a whole number");
            }
        }
    }

    private String describe(DaoException e) {
        if (hasCause(e, ConstraintViolationException.class)) {
            return "A user with this email already exists";
        }
        if (hasCause(e, OptimisticLockException.class) || hasCause(e, StaleObjectStateException.class)) {
            return "The user was changed by someone else, please try again";
        }
        String message = e.getMessage();
        return message == null || message.isBlank() ? "Database error" : "Database error: " + message;
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private static final class InputClosedException extends RuntimeException {
    }
}