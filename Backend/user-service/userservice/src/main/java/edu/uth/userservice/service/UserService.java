package edu.uth.userservice.service;

import edu.uth.userservice.model.User;
import edu.uth.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
     */
    public User register(User user) {
        // hash password trước khi lưu
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getAccountStatus() == null) user.setAccountStatus("active");
        return repo.save(user);
    }

    /**
     * Kiểm tra mật khẩu thô (raw) so với hashed lưu trong DB
     */
    public boolean checkPassword(String rawPassword, String hashed) {
        return passwordEncoder.matches(rawPassword, hashed);
    }

    /**
     * Cập nhật profile: name, email, phone, address, cityName.
     * Trả về User đã cập nhật.
     * Nếu user không tồn tại -> ném IllegalArgumentException.
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
        if (cityName != null) u.setCityName(cityName);

        // Lưu và trả về user đã cập nhật
        return repo.save(u);
    }

    /**
     * Đổi mật khẩu:
     * - Kiểm tra user tồn tại
     * - Kiểm tra currentPassword khớp
     * - Encode newPassword và lưu
     * Nếu user không tồn tại hoặc currentPassword không đúng -> ném IllegalArgumentException
     */
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

        // hash new password và lưu
        u.setPassword(passwordEncoder.encode(newPassword));
        repo.save(u);
    }
}
