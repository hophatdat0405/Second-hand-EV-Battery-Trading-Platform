// File: edu.uth.userservice.DataLoader.java
package edu.uth.userservice;

import edu.uth.userservice.model.Role;
import edu.uth.userservice.model.User;
import edu.uth.userservice.repository.RoleRepository; // Import RoleRepository
import edu.uth.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.Set; // Import Set

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository; // Cần RoleRepository

    @Autowired
    private PasswordEncoder encoder;

    // Helper để tạo role nếu chưa có
    @Transactional
    private Role createRoleIfNotFound(String name, String description) { // Thêm description
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    // Dùng hàm khởi tạo mới
                    Role newRole = new Role(name, description); 
                    return roleRepository.save(newRole);
                });
    }
    @Override
    @Transactional // Rất quan trọng khi xử lý nhiều save()
    public void run(String... args) throws Exception {
        // 1. Tạo các vai trò cơ bản
       // 🔽 SỬA CÁC DÒNG NÀY 🔽
        Role userRole = createRoleIfNotFound("USER", "Default role for regular users");
        Role adminRole = createRoleIfNotFound("ADMIN", "Administrator with full access");
        Role superAdminRole = createRoleIfNotFound("SUPER_ADMIN", null); // Hoặc "Super admin role"
        Role staffRole = createRoleIfNotFound("STAFF", "Staff / moderator"); // <-- Thêm STAFF
        // 2. Tạo Super Admin (nếu chưa có)
        if (userRepository.findByEmail("superadmin@example.com").isEmpty()) {
            User sa = new User();
            sa.setName("Super Admin");
            sa.setEmail("superadmin@example.com");
            sa.setPassword(encoder.encode("superadmin123")); // Đổi pass này!
            sa.setAccountStatus("active");
            
            // Gán cả 3 vai trò
            sa.setRoles(Set.of(userRole, adminRole, superAdminRole));
            
            userRepository.save(sa);
            System.out.println("✅ Super Admin user created");
        }
        
        // 3. Tạo Admin thường (tài khoản cũ của bạn)
        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            User u = new User();
            u.setName("Admin");
            u.setEmail("admin@example.com");
            u.setPassword(encoder.encode("admin123")); // Đổi pass này!
            u.setAccountStatus("active");
            
            // Gán 2 vai trò
            u.setRoles(Set.of(userRole, adminRole));
            
            userRepository.save(u);
            System.out.println("✅ Admin user created");
        }
    }
}