package com.sarthak.BizNex.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarthak.BizNex.entity.User;
import com.sarthak.BizNex.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Blocks access to all protected endpoints (after authentication) when a user has mustChangePassword=true,
 * except the allowed first-login/password change & auth endpoints.
 */
@Component
public class ForcePasswordChangeFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/v1/auth/first-login/password",
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password"
    );

    public ForcePasswordChangeFilter(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        if (ALLOWED_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        String username = auth.getName();
        if (username != null) {
            var opt = userRepository.findByUsername(username.toLowerCase());
            if (opt.isPresent() && opt.get().isMustChangePassword()) {
                response.setStatus(423);
                response.setContentType("application/json");
                response.getWriter().write(objectMapper.writeValueAsString(new ErrorPayload(
                        "PASSWORD_CHANGE_REQUIRED",
                        "Change password at /api/v1/auth/first-login/password before accessing other endpoints.")));
                return; // short-circuit
            }
        }
        filterChain.doFilter(request, response);
    }

    private record ErrorPayload(String error, String message) {}
}
