package local.gateway_service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.SecurityWebFilterChain;

// 401 - chưa đăng nhập
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;

import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.ServerRedirectStrategy;

import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Autowired
    private JwtUtil jwtUtil;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                .authorizeExchange(exchanges -> exchanges
                        // PUBLIC
                        .pathMatchers(
                                "/", "/index.html", "/login.html", "/register.html", "/navbar.html",
                                "/footer.html",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                "/api/auth/**","/oauth2/**","/login/oauth2/**","/api/listings/**","/api/images/**","/api/files/**","/api/products/**","/uploads/**",
                       "/oauth-success.html" ).permitAll()

                        // ADMIN
                        .pathMatchers(
                                "/admin-revenue.html",
                                "/admin-roles.html",
                                "/admin_wallet_dashboard.html",
                                "/payroll.html"
                        ).hasRole("ADMIN")

                        // STAFF
                        .pathMatchers(
                                "/admin-listings.html",
                                "/admin-transaction.html"
                        ).hasAnyRole("STAFF", "ADMIN")

                        // USER
                        .pathMatchers(
                                "/wallet.html",
                                "/compare.html",
                                "/contract-history.html",
                                "/contract.html",
                                "/deposit_success.html",
                                "/deposit.html",
                                "/edit_news.html",
                                "/liked.html",
                                "/my-reviews.html",
                              //  "/navbar.html",
                              //  "/footer.html",
                                "/notifications.html",
                                "/oauth-success.html",
                                "/online_payment.html",
                                "/payment_fail.html",
                                "/payment_success.html",
                                "/payment.html",
                                "/product_detail.html",
                                "/Product_Listings.html",
                                "/product-all.html",
                                "/profile.html",
                                "/purchase.html",
                                "/cart.html",
                                "/chat.html"
                        ).hasAnyRole("USER", "STAFF", "ADMIN")

                        // Mặc định: phải đăng nhập
                        .anyExchange().authenticated()
                )

                // XỬ LÝ LỖI 401 + 403
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new RedirectServerAuthenticationEntryPoint("/login.html"))
                        .accessDeniedHandler(accessDeniedHandler())
                )

                // Custom Security Context (JWT Cookie)
                .securityContextRepository(cookieSecurityContextRepository())
                .build();
    }

    // 🟢 Handler cho lỗi 403
    @Bean
    public ServerAccessDeniedHandler accessDeniedHandler() {
        ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();

        return (exchange, denied) ->
                redirectStrategy.sendRedirect(exchange, URI.create("/index.html"));
    }


    // 🟢 Lấy JWT từ Cookie và tạo Authentication
    private ServerSecurityContextRepository cookieSecurityContextRepository() {
        return new ServerSecurityContextRepository() {
            @Override
            public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
                return Mono.empty();
            }

            @Override
            public Mono<SecurityContext> load(ServerWebExchange exchange) {
                HttpCookie cookie = exchange.getRequest().getCookies().getFirst("jwt_token");
                if (cookie == null) return Mono.empty();

                String token = cookie.getValue();

                if (!jwtUtil.validateToken(token)) return Mono.empty();

                String userId = String.valueOf(jwtUtil.extractUserId(token));
                Set<String> roles = jwtUtil.extractRoles(token);

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                return Mono.just(new SecurityContextImpl(auth));
            }
        };
    }
}
