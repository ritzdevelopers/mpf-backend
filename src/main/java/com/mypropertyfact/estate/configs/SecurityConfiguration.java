package com.mypropertyfact.estate.configs;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true, // Enables @Secured annotation
        jsr250Enabled = true, // Enables @RolesAllowed annotation
        prePostEnabled = true // Enables @PreAuthorize, @PostAuthorize annotations
)
public class SecurityConfiguration {
    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("SUPERADMIN", "ADMIN")
                        .requestMatchers("/api/v1/user/**").authenticated()
                        .requestMatchers("/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("SUPERADMIN")
                        .requestMatchers("/api/v1/users/**").hasRole("SUPERADMIN")
                        .requestMatchers("/api/v1/auth/session").authenticated()
                        .requestMatchers("/api/v1/auth/admin-permission-definitions").authenticated()
                        /** Cookie-based refresh of access token (no Bearer header required). */
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh-token").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").authenticated()
                        .requestMatchers("/api/v1/auth/logout").permitAll()
                        .anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"timestamp\":\"" + java.time.Instant.now()
                                    + "\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"path\":\""
                                    + request.getRequestURI() + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"timestamp\":\"" + java.time.Instant.now()
                                    + "\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied - insufficient permissions\",\"path\":\""
                                    + request.getRequestURI() + "\"}");
                        }));
        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(corsProperties.isAllowCredentials());
        List<String> origins = normalizeCorsList(corsProperties.getAllowedOrigins(),
                "http://localhost:3000", "https://mypropertyfact.in");
        config.setAllowedOriginPatterns(origins);
        log.info("Allowed Origins: {}", origins);
        config.setAllowedMethods(normalizeCorsList(corsProperties.getAllowedMethods(),
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(normalizeCorsList(corsProperties.getAllowedHeaders(), "*"));
        config.setMaxAge(corsProperties.getMaxAge() > 0 ? corsProperties.getMaxAge() : 3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Normalize CORS list from properties: supports indexed list or single comma-separated string.
     * Trims and removes surrounding quotes from each value.
     */
    private static List<String> normalizeCorsList(List<String> fromProps, String... defaults) {
        if (fromProps == null || fromProps.isEmpty()) {
            return Arrays.asList(defaults);
        }
        List<String> result = fromProps.stream()
                .flatMap(s -> s.contains(",") ? Stream.of(s.split(",")) : Stream.of(s))
                .map(String::trim)
                .map(s -> s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")
                        ? s.substring(1, s.length() - 1) : s)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        return result.isEmpty() ? Arrays.asList(defaults) : result;
    }

}
