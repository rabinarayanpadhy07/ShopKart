package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    public User registerUser(RegisterRequest request) {
        // Check both unique fields in one indexed query before hashing the password.
        userRepository.findByUsernameOrEmail(request.getUsername(), request.getEmail())
                .ifPresent(existing -> {
                    if (existing.getUsername().equals(request.getUsername())) {
                        throw new RuntimeException("Username is already taken");
                    }
                    throw new RuntimeException("Email is already registered");
                });
        // Build the entity server-side so only username/email/password are ever
        // attacker-controlled - role, id, and timestamps are never bound from client input.
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        return userRepository.save(user);
    }
}
