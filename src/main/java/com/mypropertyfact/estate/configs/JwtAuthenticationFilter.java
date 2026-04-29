package com.mypropertyfact.estate.configs;

import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.services.JwtService;
import io.jsonwebtoken.ExpiredJwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final HandlerExceptionResolver handlerExceptionResolver;

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            HandlerExceptionResolver handlerExceptionResolver) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = null;
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token == null && request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("token".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String userEmail = jwtService.extractUsername(token);

            // Always apply a valid JWT to the context. Do not require authentication == null:
            // an AnonymousAuthenticationToken or stale context would otherwise skip this and
            // @PreAuthorize("@adminPermissionService.can(...)") would see no User principal → 403.
            if (userEmail != null && !userEmail.isBlank()) {

                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                if (userDetails instanceof User loadedUser && loadedUser.needsPortalActivation()) {
                    filterChain.doFilter(request, response);
                    return;
                }

                    if (jwtService.isTokenValid(token, userDetails)) {
                        boolean sessionValid = true;
                        if (userDetails instanceof User user) {
                            boolean staffSessionVersioned = user.getRoles() != null && user.getRoles().stream()
                                    .anyMatch(r -> r != null && Boolean.TRUE.equals(r.getIsActive())
                                            && ("SUPERADMIN".equalsIgnoreCase(r.getRoleName())
                                                    || "ADMIN".equalsIgnoreCase(r.getRoleName())));
                            Integer tokenVersion = jwtService.extractTokenVersion(token);
                            Integer currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
                            if (staffSessionVersioned) {
                                sessionValid = (tokenVersion != null && tokenVersion.equals(currentVersion));
                            } else if (tokenVersion != null) {
                                // Consumer / non-staff: invalidate sessions when tokenVersion changes (e.g. password reset).
                                sessionValid = Objects.equals(tokenVersion, currentVersion);
                            }
                        }
                    if (sessionValid) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request));

                        SecurityContextHolder.getContext()
                                .setAuthentication(authToken);
                    }
                }
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.trace("JWT expired: {}", e.getMessage());
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT error", e);
            handlerExceptionResolver.resolveException(
                    request, response, null, e);
        }
    }
}
