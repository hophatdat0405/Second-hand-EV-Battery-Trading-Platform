package edu.uth.userservice.service;

import edu.uth.userservice.model.Role;
import edu.uth.userservice.model.User;
import edu.uth.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository repo;
    

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleService roleService;

    /**
     * Tìm user theo email
     */
    public Optional<User> findByEmail(String email) {
        return repo.findByEmail(email);
    }

    /**
     * Tìm user theo phone
     */
    public Optional<User> findByPhone(String phone) {
        return repo.findFirstByPhone(phone);
    }

    /**
     * Tìm user theo id (Integer)
     */
    public Optional<User> findById(Integer id) {
        return repo.findById(id);
    }

    /**
     * Đăng ký user: luôn hash password trước khi lưu.
     * Gán role USER mặc định.
     */
    @Transactional
    public User register(User user) {
        // hash password trước khi lưu
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getAccountStatus() == null) user.setAccountStatus("active");

        // Save (without roles) first to get userId
        User saved = repo.save(user);

        // Gán role mặc định USER (nếu tồn tại trong DB)
        Role userRole = roleService.findByName("USER").orElse(null);
        if (userRole != null) {
            saved.getRoles().add(userRole);
            saved = repo.save(saved);
        }

        return saved;
    }

    /**
     * Kiểm tra mật khẩu thô (raw) so với hashed lưu trong DB
     */
    public boolean checkPassword(String rawPassword, String hashed) {
        return passwordEncoder.matches(rawPassword, hashed);
    }

    /**
     * Cập nhật profile: name, email, phone, address, cityName.
     */
    @Transactional
    public User updateProfile(Integer userId,
                              String name,
                              String email,
                              String phone,
                              String address,
                              String cityName) throws IllegalArgumentException {
        Optional<User> opt = repo.findById(userId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User u = opt.get();

        // Chỉ gán nếu không null (frontend có thể gửi trường rỗng)
        if (name != null) u.setName(name);
        if (email != null) u.setEmail(email.trim().toLowerCase());
        if (phone != null) u.setPhone(phone);
        if (address != null) u.setAddress(address);
        return repo.save(u);
    }

    @Transactional
    public void changePassword(Integer userId, String currentPassword, String newPassword) throws IllegalArgumentException {
        Optional<User> opt = repo.findById(userId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User u = opt.get();

        if (!checkPassword(currentPassword, u.getPassword())) {
            throw new IllegalArgumentException("Current password incorrect");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        repo.save(u);
    }

    /* ---------------- new role management methods ---------------- */

    /**
     * Add a role to a user. Returns updated User.
     * Throws IllegalArgumentException if user or role not found.
     */
    @Transactional
    public User addRoleToUser(Integer userId, String roleName) {
        User user = repo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Role role = roleService.findByName(roleName).orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        // avoid duplicate
        if (user.getRoles().stream().noneMatch(r -> r.getName().equalsIgnoreCase(roleName))) {
            user.getRoles().add(role);
            user = repo.save(user);
        }
        return user;
    }

    /**
     * Remove a role from a user. Returns updated User.
     */
    @Transactional
    public User removeRoleFromUser(Integer userId, String roleName) {
        User user = repo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean removed = user.getRoles().removeIf(r -> r.getName().equalsIgnoreCase(roleName));
        if (removed) {
            user = repo.save(user);
        }
        return user;
    }

    /**
     * Replace user's roles with the provided role names (clear -> set).
     * Returns updated User.
     */
    @Transactional
    public User setRolesForUser(Integer userId, List<String> roleNames) {
        User user = repo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        // gather Role entities (throw if any invalid)
        Set<Role> newRoles = roleNames == null ? new HashSet<>() :
                roleNames.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .map(rn -> roleService.findByName(rn).orElseThrow(
                                () -> new IllegalArgumentException("Role not found: " + rn)))
                        .collect(Collectors.toSet());

        user.setRoles(newRoles);
        return repo.save(user);
    }

    // in UserService
public List<User> findAllUsers() {
    return repo.findAll();
}


    /**
     * Helper: return role names of a user
     */
    public List<User> listAllUsers() {
    return repo.findAll();
}

    public Set<String> getRoleNamesForUser(Integer userId) {
        return repo.findById(userId)
                .map(u -> u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }

     /**
     * Set accountStatus (e.g. "active","locked")
     */
    @Transactional
    public User setAccountStatus(Integer userId, String status) {
        User u = repo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        u.setAccountStatus(status);
        return repo.save(u);
    }

    @Transactional
    public User lockUser(Integer userId) {
        return setAccountStatus(userId, "locked");
    }

    @Transactional
    public User unlockUser(Integer userId) {
        return setAccountStatus(userId, "active");
    }
    
    
}
