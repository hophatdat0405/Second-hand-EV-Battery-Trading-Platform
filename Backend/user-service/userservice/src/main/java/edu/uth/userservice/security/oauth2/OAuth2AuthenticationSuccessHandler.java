// File: OAuth2AuthenticationSuccessHandler.java (Hoàn chỉnh cho localStorage)
package edu.uth.userservice.security.oauth2;

import edu.uth.userservice.model.User;
import edu.uth.userservice.service.UserService;
import edu.uth.userservice.security.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final String frontendRedirect;

    public OAuth2AuthenticationSuccessHandler(JwtUtil jwtUtil,
                                              UserService userService,
                                              @org.springframework.beans.factory.annotation.Value("${app.frontend.redirect-after-login}") String frontendRedirect) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.frontendRedirect = frontendRedirect; // URL từ application.properties
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        String email = null;
        try {
            if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                org.springframework.security.oauth2.core.user.OAuth2User oauthUser =
                        (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
                email = (String) oauthUser.getAttribute("email");
            }
        } catch (Exception ex) {
            logger.warn("Failed to extract email from OAuth2 principal: {}", ex.getMessage(), ex);
        }

        if (email == null) {
            response.sendRedirect(frontendRedirect + "?error=no_email");
            return;
        }

        // ✅ FIX LỖI 403: Dùng findByEmailWithRoles để lấy roles
        Optional<User> opt = userService.findByEmailWithRoles(email); 
        Integer userId = opt.map(User::getUserId).orElse(null);
        
        if (userId == null || opt.isEmpty()) {
             response.sendRedirect(frontendRedirect + "?error=user_not_found");
             return;
        }
        
        User user = opt.get();
        Set<String> roles = user.getRoles().stream()
                                .map(r -> r.getName())
                                .collect(Collectors.toSet());

        String token;
        try {
            // ✅ SỬ DỤNG: jwtUtil.generateToken(subject, userId, roles)
            token = jwtUtil.generateToken(email, userId, roles);
        } catch (Exception ex) {
            response.sendRedirect(frontendRedirect + "?error=token_error");
            return;
        }

        // ✅ QUAN TRỌNG: Gửi token về frontend qua URL Hash Fragment (#)
        String redirectUrl = frontendRedirect + "#token=" + token;

        logger.info("OAuth success for email={}, userId={} — redirecting with token hash", email, userId);
        response.sendRedirect(redirectUrl);
    }
}