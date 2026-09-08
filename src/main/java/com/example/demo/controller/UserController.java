package com.example.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import com.example.demo.entity.User;
import com.example.demo.service.RateLimitService;
import com.example.demo.service.UserService;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserDTO;
import jakarta.validation.Valid;

@RestController
@CrossOrigin(origins = "${spring.web.cors.allowed-origins:http://localhost:5174}")
@RequestMapping("/api/users")
public class UserController {

    private static final int REGISTER_MAX_ATTEMPTS = 5;
    private static final int REGISTER_WINDOW_SECONDS = 600;

    private final UserService userService;
    private final RateLimitService rateLimitService;

    @Autowired
    public UserController(UserService userService, RateLimitService rateLimitService) {
        this.userService = userService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest, HttpServletRequest request) {
        if (!rateLimitService.tryAcquire("register:" + clientIp(request), REGISTER_MAX_ATTEMPTS, REGISTER_WINDOW_SECONDS)) {
            return ResponseEntity.status(429).body(Map.of("error", "Too many registration attempts. Please try again later."));
        }
        try {
            User registeredUser = userService.registerUser(registerRequest);
            return ResponseEntity.ok(Map.of("message", "User registered successfully", "user", new UserDTO(registeredUser)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(Map.of("username", user.getUsername(), "role", user.getRole().name()));
    }
}
