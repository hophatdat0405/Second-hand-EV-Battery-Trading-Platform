package edu.uth.userservice;

import edu.uth.userservice.model.Role;
import edu.uth.userservice.model.User;
import edu.uth.userservice.repository.RoleRepository;
import edu.uth.userservice.repository.UserRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate; // ✅ 1. Thêm import này
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private RabbitTemplate rabbitTemplate; // ✅ 2. Inject RabbitTemplate

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
            
            // Lưu user và lấy lại đối tượng đã lưu (để có ID)
            User savedUser = userRepository.save(u);
            System.out.println("✅ User created: " + email + " (ID: " + savedUser.getUserId() + ")");

            // ✅ 3. Kiểm tra nếu là STAFF thì bắn Event sang Wallet Service
            boolean isStaff = roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase("STAFF"));
            if (isStaff) {
                sendStaffCreatedEvent(savedUser.getUserId().longValue());
            }

        } else {
            System.out.println("ℹ️ User already exists: " + email);
        }
    }

    // ✅ 4. Hàm gửi event thủ công (Giả lập logic của UserService)
    private void sendStaffCreatedEvent(Long userId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("role", "STAFF");
            event.put("eventType", "ADD"); // Báo là thêm quyền STAFF

            // Gửi vào Exchange "ev.exchange" với Routing Key "user.role.updated"
            rabbitTemplate.convertAndSend("ev.exchange", "user.role.updated", event);
            
            System.out.println("📤 [RabbitMQ] Đã gửi sự kiện tạo STAFF cho userId: " + userId);
        } catch (Exception e) {
            System.err.println("❌ [RabbitMQ] Lỗi khi gửi sự kiện: " + e.getMessage());
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
            sa.setPassword(encoder.encode("superadmin123")); 
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
            u.setPassword(encoder.encode("admin123")); 
            u.setAccountStatus("active");
            u.setRoles(Set.of(userRole, adminRole));
            userRepository.save(u);
            System.out.println("✅ Admin user created");
        }

        // 4. Tạo thêm 5 tài khoản chỉ có quyền USER
        List<String[]> users = List.of(
                new String[]{"Nguyễn Văn Một", "user1@example.com", "user1pass"},
                new String[]{"Trần Văn Hai", "user2@example.com", "user2pass"},
                new String[]{"Lê Thị Ba", "user3@example.com", "user3pass"},
                new String[]{"Phạm Văn Bốn", "user4@example.com", "user4pass"},
                new String[]{"Hoàng Thị Năm", "user5@example.com", "user5pass"}
        );
        for (String[] info : users) {
            createUserIfNotFound(info[0], info[1], info[2], "active", Set.of(userRole));
        }

        // 5. Tạo 3 tài khoản có quyền STAFF (kèm USER)
        // 🔥 LƯU Ý: Những user này sẽ kích hoạt hàm sendStaffCreatedEvent ở trên
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