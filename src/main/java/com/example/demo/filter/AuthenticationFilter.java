package com.example.demo.filter;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    private final AuthService authService;
    private final UserRepository userRepository;

    private static final String[] UNAUTHENTICATED_PATHS = {
        "/api/users/register",
        "/api/auth/login",
        "/api/auth/google",
        "/admin",
        "/",
        "/index.html",
        "/favicon.svg",
        "/favicon.ico"
    };

    public AuthenticationFilter(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestURI = request.getRequestURI();
            logger.info("Request URI: {}", requestURI);

            // Public catalog: guests can browse products and search suggestions
            if (isPublicCatalogGet(request, requestURI)) {
                attachUserIfPresent(request);
                filterChain.doFilter(request, response);
                return;
            }

            // Allow unauthenticated paths
            if (isUnauthenticatedPath(requestURI)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Handle preflight (OPTIONS) requests
            if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
                filterChain.doFilter(request, response);
                return;
            }

            // Extract and validate the token
            String token = getAuthTokenFromCookies(request);
            if (token == null || !authService.validateToken(token)) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Invalid or missing token");
                return;
            }

            // Extract username and verify user
            String username = authService.extractUsername(token);
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: User not found");
                return;
            }

            // Get authenticated user and role
            User authenticatedUser = userOptional.get();
            Role role = authenticatedUser.getRole();
            logger.info("Authenticated User: {}, Role: {}", authenticatedUser.getUsername(), role);

            // Set Spring Security Context authentication
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Attach user details to request for backward compatibility in controllers
            request.setAttribute("authenticatedUser", authenticatedUser);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Unexpected error in AuthenticationFilter", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    private boolean isPublicCatalogGet(HttpServletRequest request, String requestURI) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return "/api/products".equals(requestURI) || requestURI.startsWith("/api/products/suggestions") || "/api/products/categories".equals(requestURI) || requestURI.startsWith("/api/reviews/product/");
    }

    private void attachUserIfPresent(HttpServletRequest request) {
        try {
            String token = getAuthTokenFromCookies(request);
            if (token == null || !authService.validateToken(token)) {
                return;
            }
            String username = authService.extractUsername(token);
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return;
            }
            User authenticatedUser = userOptional.get();
            Role role = authenticatedUser.getRole();
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("authenticatedUser", authenticatedUser);
        } catch (Exception e) {
            logger.warn("Could not attach optional user for public catalog request", e);
        }
    }

    private boolean isUnauthenticatedPath(String requestURI) {
        return Arrays.asList(UNAUTHENTICATED_PATHS).contains(requestURI) || requestURI.startsWith("/error") || requestURI.startsWith("/assets/");
    }

    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

    private String getAuthTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> "authToken".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
