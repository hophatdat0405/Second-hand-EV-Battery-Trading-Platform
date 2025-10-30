package edu.uth.userservice.security;

import edu.uth.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT filter:
 *  - Bỏ qua các đường dẫn public (OPTIONS, /api/auth/**, static...)
 *  - Nếu có token hợp lệ -> load role names từ DB -> set Authentication với authorities ("ROLE_<NAME>")
 *  - Nếu token không hợp lệ -> không set Authentication (request sẽ bị chặn nếu endpoint yêu cầu auth)
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    // Nếu muốn mở rộng, thêm các đường dẫn public ở đây (backend trả file hoặc api public)
    private static final String[] PUBLIC_PREFIX = new String[] {
            "/api/auth", "/public", "/static", "/css/", "/js/", "/images/", "/favicon.ico"
    };

    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        // allow preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        for (String p : PUBLIC_PREFIX) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (isPublicPath(request)) {
                // Skip JWT check for public resources
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // no token -> continue (endpoints that require auth will reject)
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtUtil.validateToken(token)) {
                logger.debug("JWT token validation failed or expired for request: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            Integer userId = jwtUtil.extractUserId(token);
            if (userId == null) {
                logger.debug("JWT token did not contain user id claim");
                filterChain.doFilter(request, response);
                return;
            }

            // if already authenticated, skip
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Load roles from DB (may return empty set)
            Set<String> roleNames = userService.getRoleNamesForUser(userId);
            if (roleNames == null) roleNames = Collections.emptySet();

            Collection<SimpleGrantedAuthority> authorities = roleNames.stream()
                    .filter(rn -> rn != null && !rn.isBlank())
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(rn -> "ROLE_" + rn)    // must match Spring's hasRole("X") convention
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            logger.debug("Set Authentication for userId={} with authorities={}", userId, authorities);

        } catch (Exception ex) {
            // don't propagate — leave security context empty so endpoints that require auth will reject
            logger.warn("Error in JwtAuthenticationFilter: {}", ex.getMessage(), ex);
        }

        // continue filter chain
        filterChain.doFilter(request, response);
    }
}
