package sam.com.profilemanager;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sam.com.profilemanager.dao.DaoException;
import sam.com.profilemanager.dao.UserDao;
import sam.com.profilemanager.dao.UserDaoImpl;
import sam.com.profilemanager.model.User;
import sam.com.profilemanager.util.HibernateUtil;

import java.util.List;
import java.util.Scanner;

public class ProfileManagerApp {

    private static final Logger log = LoggerFactory.getLogger(ProfileManagerApp.class);

    private final UserDao dao = new UserDaoImpl();
    private final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        log.info("ProfileManager started");
        try {
            new ProfileManagerApp().run();
        } catch (Exception e) {
            log.error("Fatal error", e);
            System.out.println("Fatal error: " + e.getMessage());
        } finally {
            HibernateUtil.shutdown();
            log.info("ProfileManager stopped");
        }
    }

    private void run() {
        while (true) {
            System.out.println("\n1 - Create\n2 - find by id\n3 - all\n4 - update\n5 - delete\n0 - exit");
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
                    default -> System.out.println("not found");
                }
            } catch (NumberFormatException e) {
                System.out.println("Enter number");
            } catch (DaoException e) {
                System.out.println(describe(e));
            }
        }
    }

    private void create() {
        String name = ask("Name: ");
        String email = ask("Email: ");
        int age = Integer.parseInt(ask("Age: "));
        User user = new User(name, email, age);
        dao.create(user);
        System.out.println("Create: " + user);
    }

    private void find() {
        Long id = Long.parseLong(ask("id: "));
        dao.findById(id).ifPresentOrElse(
                System.out::println,
                () -> System.out.println("Not found"));
    }

    private void list() {
        List<User> users = dao.findAll();
        if (users.isEmpty()) {
            System.out.println("Empty");
        }
        users.forEach(System.out::println);
    }

    private void update() {
        Long id = Long.parseLong(ask("id: "));
        User user = dao.findById(id).orElse(null);
        if (user == null) {
            System.out.println("Not found");
            return;
        }
        String name = ask("Name [" + user.getName() + "]: ");
        String email = ask("Email [" + user.getEmail() + "]: ");
        String age = ask("Age [" + user.getAge() + "]: ");
        if (!name.isEmpty()) user.setName(name);
        if (!email.isEmpty()) user.setEmail(email);
        if (!age.isEmpty()) user.setAge(Integer.parseInt(age));
        User updated = dao.update(user);
        System.out.println("Update: " + updated);
    }

    private void delete() {
        Long id = Long.parseLong(ask("id: "));
        System.out.println(dao.delete(id) ? "Delete" : "Not Found");
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        return in.hasNextLine() ? in.nextLine().trim() : "0";
    }

    private String describe(DaoException e) {
        if (e.getCause() instanceof ConstraintViolationException) {
            return "This email already exists, or the field is empty";
        }
        return "Database error";
    }
}