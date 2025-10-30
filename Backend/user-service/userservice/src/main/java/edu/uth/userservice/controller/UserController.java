package edu.uth.userservice.controller;

import edu.uth.userservice.dto.ChangePasswordRequest;
import edu.uth.userservice.dto.UpdateProfileRequest;
import edu.uth.userservice.dto.UserDTO;
import edu.uth.userservice.model.User;
import edu.uth.userservice.security.JwtUtil;
import edu.uth.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = {"http://127.0.0.1:5501","http://localhost:3000","http://localhost:5501"})
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Helper: lấy userId (Integer) từ header Authorization: Bearer <token>
     * Trả null nếu không có token hợp lệ.
     */
    private Integer getUserIdFromAuthHeader(String authHeader) {
    // 1) try from SecurityContext (set by JwtAuthenticationFilter)
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof Integer) {
        return (Integer) auth.getPrincipal();
    }

    // 2) fallback parse header (backwards compatible)
    if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
    String token = authHeader.substring(7);
    try {
        return jwtUtil.extractUserId(token);
    } catch (Exception ex) {
        return null;
    }
}

    /** Lấy profile user hiện tại */
    @PutMapping("/profile")
public ResponseEntity<?> updateProfile(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestBody UpdateProfileRequest req) {

    Integer userId = getUserIdFromAuthHeader(authHeader);
    if (userId == null) return ResponseEntity.status(401).body("Unauthorized");

    // Normalize incoming email (lowercase + trim) to avoid case issues
    String incomingEmail = req.getEmail() == null ? null : req.getEmail().trim().toLowerCase();
    String incomingPhone = req.getPhone() == null ? null : req.getPhone().trim();

    // Check unique email if provided
    if (incomingEmail != null && !incomingEmail.isBlank()) {
        Optional<User> byEmail = userService.findByEmail(incomingEmail);
        if (byEmail.isPresent() && !Objects.equals(byEmail.get().getUserId(), userId)) {
            return ResponseEntity.badRequest().body("Email already in use");
        }
    }

    // Check unique phone if provided
    if (incomingPhone != null && !incomingPhone.isBlank()) {
        Optional<User> byPhone = userService.findByPhone(incomingPhone);
        if (byPhone.isPresent() && !Objects.equals(byPhone.get().getUserId(), userId)) {
            return ResponseEntity.badRequest().body("Phone already in use");
        }
    }

    try {
        // call service with normalized email/phone
        User updated = userService.updateProfile(userId,
                req.getName(),
                incomingEmail,
                incomingPhone,
                req.getAddress(), incomingPhone
                );

        // If email changed, inform client so it can force re-login if desired
        String note = null;
        if (incomingEmail != null && !incomingEmail.equalsIgnoreCase((updated.getEmail() == null ? "" : updated.getEmail()))) {
            note = "email-changed";
        }

        // Build response body: updated user + optional note
        // You can create a DTO, here simple map:
        Map<String, Object> body = new HashMap<>();
        body.put("user", new UserDTO(updated));
        if (note != null) body.put("note", note);

        return ResponseEntity.ok(body);
    } catch (IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    } catch (Exception ex) {
        return ResponseEntity.status(500).body("Server error");
    }
}
    /**
     * Đổi mật khẩu: nhận { currentPassword, newPassword }
     * Yêu cầu Authorization header
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ChangePasswordRequest req) {

        Integer userId = getUserIdFromAuthHeader(authHeader);
        if (userId == null) return ResponseEntity.status(401).body("Unauthorized");

        if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()
                || req.getNewPassword() == null || req.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Both currentPassword and newPassword are required");
        }

        try {
            userService.changePassword(userId, req.getCurrentPassword(), req.getNewPassword());
            return ResponseEntity.ok("Password changed");
        } catch (IllegalArgumentException ex) {
            // ví dụ current password sai -> trả 400
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
    }
}
