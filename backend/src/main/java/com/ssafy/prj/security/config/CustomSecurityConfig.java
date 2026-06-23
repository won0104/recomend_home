package com.ssafy.prj.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import com.ssafy.prj.security.password.Sha256PasswordEncoder;
import com.ssafy.prj.security.web.SecurityResponseHelper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class CustomSecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new Sha256PasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("USER")
                .build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/clients/**", "/favicon.ico").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/members").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/regions/**", "/api/properties/**", "/api/commercials/**",
                        "/api/boards", "/api/boards/{id}", "/api/notices", "/api/notices/{id}",
                        "/api/houses", "/api/houses/{aptSeq}", "/api/houses/{aptSeq}/deals").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/notices").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/notices/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/notices/**").hasRole("ADMIN")
                .requestMatchers("/api/auth/me", "/api/members/me", "/api/favorites/**",
                        "/api/boards/me", "/api/boards/**").authenticated()
                .anyRequest().permitAll());

        http.formLogin(login -> login.disable());
        http.logout(logout -> logout.disable());
        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) ->
                        SecurityResponseHelper.writeJson(response, HttpServletResponseStatus.UNAUTHORIZED,
                                java.util.Map.of("success", false, "message", "로그인이 필요합니다.", "data", null)))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        SecurityResponseHelper.writeJson(response, HttpServletResponseStatus.FORBIDDEN,
                                java.util.Map.of("success", false, "message", "권한이 없습니다.", "data", null))));

        http.rememberMe(remember -> remember
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(60 * 60 * 24 * 7));

        http.csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static final class HttpServletResponseStatus {
        static final int OK = 200;
        static final int UNAUTHORIZED = 401;
        static final int FORBIDDEN = 403;
    }
}
