package edu.uth.userservice;

import edu.uth.userservice.model.User;
import edu.uth.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository repo;

    @Autowired
    private PasswordEncoder encoder;

    @Override
    public void run(String... args) throws Exception {
        if (repo.findByEmail("admin@example.com").isEmpty()) {
            User u = new User();
            u.setName("Admin");
            u.setEmail("admin@example.com");
            u.setPassword(encoder.encode("admin123"));
            u.setAccountStatus("active");
            repo.save(u);
            System.out.println("Admin user created");
        }
    }
}
