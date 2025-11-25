// File: edu.uth.userservice.DataLoader.java

package edu.uth.userservice;

import edu.uth.userservice.model.Role;
import edu.uth.userservice.model.User;
import edu.uth.userservice.repository.RoleRepository;
import edu.uth.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    // Helper để tạo role nếu chưa có
    @Transactional
    private Role createRoleIfNotFound(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role newRole = new Role(name, description);
                    return roleRepository.save(newRole);
                });
    }

    // Helper để tạo user nếu chưa tồn tại
    @Transactional
    private void createUserIfNotFound(String name, String email, String rawPassword, String accountStatus, Set<Role> roles) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User u = new User();
            u.setName(name);
            u.setEmail(email);
            u.setPassword(encoder.encode(rawPassword));
            u.setAccountStatus(accountStatus);
            u.setRoles(roles);
            userRepository.save(u);
            System.out.println("✅ User created: " + email + " (roles=" + roles.stream().map(Role::getName).toList() + ")");
        } else {
            System.out.println("ℹ️ User already exists: " + email);
        }
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Tạo các vai trò cơ bản
        Role userRole = createRoleIfNotFound("USER", "Default role for regular users");
        Role adminRole = createRoleIfNotFound("ADMIN", "Administrator with full access");
        Role superAdminRole = createRoleIfNotFound("SUPER_ADMIN", "Super admin role");
        Role staffRole = createRoleIfNotFound("STAFF", "Staff / moderator");

        // 2. Tạo Super Admin (nếu chưa có)
        if (userRepository.findByEmail("superadmin@example.com").isEmpty()) {
            User sa = new User();
            sa.setName("Super Admin");
            sa.setEmail("superadmin@example.com");
            sa.setPassword(encoder.encode("superadmin123")); // Hãy đổi mật khẩu mặc định sau khi deploy
            sa.setAccountStatus("active");
            sa.setRoles(Set.of(userRole, adminRole, superAdminRole));
            userRepository.save(sa);
            System.out.println("✅ Super Admin user created");
        }

        // 3. Tạo Admin thường (nếu chưa có)
        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            User u = new User();
            u.setName("Admin");
            u.setEmail("admin@example.com");
            u.setPassword(encoder.encode("admin123")); // Hãy đổi mật khẩu mặc định sau khi deploy
            u.setAccountStatus("active");
            u.setRoles(Set.of(userRole, adminRole));
            userRepository.save(u);
            System.out.println("✅ Admin user created");
        }

        // 4. Tạo thêm 5 tài khoản chỉ có quyền USER
        List<String[]> users = List.of(
                new String[]{"User One", "user1@example.com", "user1pass"},
                new String[]{"User Two", "user2@example.com", "user2pass"},
                new String[]{"User Three", "user3@example.com", "user3pass"},
                new String[]{"User Four", "user4@example.com", "user4pass"},
                new String[]{"User Five", "user5@example.com", "user5pass"}
        );
        for (String[] info : users) {
            createUserIfNotFound(info[0], info[1], info[2], "active", Set.of(userRole));
        }

        // 5. Tạo 3 tài khoản có quyền STAFF (kèm USER)
        List<String[]> staffs = List.of(
                new String[]{"Staff One", "staff1@example.com", "staff1pass"},
                new String[]{"Staff Two", "staff2@example.com", "staff2pass"},
                new String[]{"Staff Three", "staff3@example.com", "staff3pass"}
        );
        for (String[] info : staffs) {
            createUserIfNotFound(info[0], info[1], info[2], "active", Set.of(userRole, staffRole));
        }

        // 6. Tạo 2 tài khoản có quyền ADMIN (kèm USER)
        List<String[]> admins = List.of(
                new String[]{"Admin Extra One", "admin1@example.com", "admin1pass"},
                new String[]{"Admin Extra Two", "admin2@example.com", "admin2pass"}
        );
        for (String[] info : admins) {
            createUserIfNotFound(info[0], info[1], info[2], "active", Set.of(userRole, adminRole));
        }
    }
}
