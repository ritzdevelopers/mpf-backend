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

import com.mypropertyfact.estate.services.AdminAuditLogService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
    /**
     * Always merged with {@code app.cors.allowed-origins} so a partial production
     * config (e.g. only staging) cannot block the public site and admin (excel upload, etc.).
     */
    private static final List<String> CORS_BASE_ORIGINS = List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "https://mypropertyfact.in",
            "https://www.mypropertyfact.in",
            "https://mypropertyfact.com",
            "https://www.mypropertyfact.com");

    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AdminAuditLogService adminAuditLogService;

    @Bean
    public AdminAuditLoggingFilter adminAuditLoggingFilter() {
        return new AdminAuditLoggingFilter(adminAuditLogService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AdminAuditLoggingFilter adminAuditLoggingFilter)
            throws Exception {
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
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/unlock-enquiries")
                                .hasAnyRole("SUPERADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/enquiry-access-status")
                                .hasAnyRole("SUPERADMIN", "ADMIN")
                        /** Cookie-based refresh of access token (no Bearer header required). */
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh-token").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").authenticated()
                        .requestMatchers("/api/v1/auth/logout").permitAll()
                        .anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(adminAuditLoggingFilter, JwtAuthenticationFilter.class)
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
        List<String> origins = buildMergedAllowedOrigins(corsProperties.getAllowedOrigins());
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
     * Merges {@link #CORS_BASE_ORIGINS} with values from config (comma-separated and quoted entries
     * allowed). Order: base set first, then any extras from properties.
     */
    private static List<String> buildMergedAllowedOrigins(List<String> fromProps) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(CORS_BASE_ORIGINS);
        for (String entry : expandCorsPropertyEntries(fromProps)) {
            merged.add(entry);
        }
        return new ArrayList<>(merged);
    }

    private static List<String> expandCorsPropertyEntries(List<String> fromProps) {
        if (fromProps == null || fromProps.isEmpty()) {
            return List.of();
        }
        return fromProps.stream()
                .filter(Objects::nonNull)
                .flatMap(s -> s.contains(",") ? Stream.of(s.split(",")) : Stream.of(s))
                .map(String::trim)
                .map(s -> s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")
                        ? s.substring(1, s.length() - 1) : s)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Normalize CORS list from properties: supports indexed list or single comma-separated string.
     * Trims and removes surrounding quotes from each value. If the property is unset, uses
     * {@code defaults} for methods/headers; origins use {@link #buildMergedAllowedOrigins} instead.
     */
    private static List<String> normalizeCorsList(List<String> fromProps, String... defaults) {
        if (fromProps == null || fromProps.isEmpty()) {
            return Arrays.asList(defaults);
        }
        List<String> result = expandCorsPropertyEntries(fromProps);
        return result.isEmpty() ? Arrays.asList(defaults) : result;
    }

}
