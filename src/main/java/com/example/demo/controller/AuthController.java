package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.LoginRequest;
import com.example.demo.entity.User;
import com.example.demo.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@CrossOrigin(origins = "${spring.web.cors.allowed-origins:http://localhost:5174}", allowCredentials = "true")
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @Value("${app.security.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${app.security.cookie-same-site:None}")
    private String cookieSameSite;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            User user = authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
            String token = authService.generateToken(user);

            ResponseCookie cookie = ResponseCookie.from("authToken", token)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(3600)
                    .sameSite(cookieSameSite)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Login successful");
            responseBody.put("role", user.getRole().name());
            responseBody.put("username", user.getUsername());

            return ResponseEntity.ok(responseBody);
            
        } 
        catch (RuntimeException e) 
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user != null) {
                authService.logout(user);
            }
            ResponseCookie cookie = ResponseCookie.from("authToken", "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(0)
                    .sameSite(cookieSameSite)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "Logout successful");
            return ResponseEntity.ok(responseBody);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Logout failed");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> requestBody, HttpServletResponse response) {
        try {
            String idToken = requestBody.get("credential");
            if (idToken == null || idToken.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Google ID token (credential) is required"));
            }

            User user = authService.authenticateGoogleUser(idToken);
            String token = authService.generateToken(user);

            ResponseCookie cookie = ResponseCookie.from("authToken", token)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(3600)
                    .sameSite(cookieSameSite)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Login successful");
            responseBody.put("role", user.getRole().name());
            responseBody.put("username", user.getUsername());

            return ResponseEntity.ok(responseBody);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }
}
