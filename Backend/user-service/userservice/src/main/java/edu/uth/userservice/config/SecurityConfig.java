package edu.uth.userservice.config;

import edu.uth.userservice.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // 1) always allow preflight
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                // 2) public static & pages (anyone)
                auth.requestMatchers("/", "/index.html", "/login.html", "/register.html",
                        "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll();

                // 3) auth endpoints
                auth.requestMatchers("/api/auth/**").permitAll();

                // 4) admin endpoints (only ADMIN)
                auth.requestMatchers("/api/admin/**", "/admin.html", "/admin-panel/**", "/admin/**")
                        .hasRole("ADMIN");

                // 5) staff endpoints (STAFF or ADMIN)
                auth.requestMatchers("/api/staff/**", "/staff/**", "/staff-dashboard.html", "/moderation/**")
                        .hasAnyRole("STAFF", "ADMIN");

                // 6) user endpoints (authenticated)
                auth.requestMatchers("/api/user/**", "/profile.html", "/liked.html", "/compare.html")
                        .authenticated();

                // 7) default: authenticated for everything else
                auth.anyRequest().authenticated();
            });

        // Important: JWT filter must run before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    
}
