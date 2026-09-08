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

public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    private final AuthService authService;

    private static final String[] UNAUTHENTICATED_PATHS = {
        "/api/users/register",
        "/api/auth/login",
        "/api/auth/google",
        "/api/health",
        "/admin",
        "/",
        "/index.html",
        "/favicon.svg",
        "/favicon.ico"
    };

    public AuthenticationFilter(AuthService authService) {
        this.authService = authService;
    }

    public AuthenticationFilter(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestURI = request.getRequestURI();
            logger.debug("Processing request URI: {}", requestURI);

            // Public catalog: guests can browse products, suggestions, categories, filters, reviews
            // without performing any database authentication
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

            // Extract token from cookie
            String token = getAuthTokenFromCookies(request);
            if (token == null || !authService.validateTokenCryptographic(token)) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Invalid or missing token");
                return;
            }

            // Extract user directly from verified JWT claims without querying the database
            User authenticatedUser = authService.extractUserFromToken(token);
            if (authenticatedUser == null || authenticatedUser.getUsername() == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: User not found");
                return;
            }

            Role role = authenticatedUser.getRole() != null ? authenticatedUser.getRole() : Role.CUSTOMER;
            logger.debug("Authenticated User: {}, Role: {}", authenticatedUser.getUsername(), role);

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
        return "/api/products".equals(requestURI)
                || requestURI.startsWith("/api/products/suggestions")
                || "/api/products/categories".equals(requestURI)
                || "/api/products/filters".equals(requestURI)
                || requestURI.startsWith("/api/reviews/product/");
    }

    /**
     * Optional user attachment for public catalog GET requests.
     * Evaluates JWT claims purely in memory with ZERO database queries.
     */
    private void attachUserIfPresent(HttpServletRequest request) {
        try {
            String token = getAuthTokenFromCookies(request);
            if (token == null || !authService.validateTokenCryptographic(token)) {
                return;
            }
            User authenticatedUser = authService.extractUserFromToken(token);
            if (authenticatedUser == null || authenticatedUser.getUsername() == null) {
                return;
            }
            Role role = authenticatedUser.getRole() != null ? authenticatedUser.getRole() : Role.CUSTOMER;
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("authenticatedUser", authenticatedUser);
        } catch (Exception e) {
            logger.debug("Could not attach optional user for public catalog request: {}", e.getMessage());
        }
    }

    private boolean isUnauthenticatedPath(String requestURI) {
        return Arrays.asList(UNAUTHENTICATED_PATHS).contains(requestURI)
                || "/api/auth/logout".equals(requestURI)
                || requestURI.startsWith("/error")
                || requestURI.startsWith("/assets/");
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
