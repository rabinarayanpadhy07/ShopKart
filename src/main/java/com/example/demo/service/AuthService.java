package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.entity.JWTToken;
import com.example.demo.entity.User;
import com.example.demo.repository.JWTTokenRepository;
import com.example.demo.repository.UserRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final Key SIGNING_KEY;

    private final UserRepository userRepository;
    private final JWTTokenRepository jwtTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${google.client.id:}")
    private String googleClientId;

    // Injecting jwt.secret from properties file
    @Autowired
    public AuthService(UserRepository userRepository, JWTTokenRepository jwtTokenRepository,
                       @Value("${jwt.secret}") String jwtSecret) {
        this.userRepository = userRepository;
        this.jwtTokenRepository = jwtTokenRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();

        // Ensure the key length is at least 64 bytes
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalArgumentException("JWT_SECRET in application.properties must be at least 64 bytes long for HS512.");
        }
        this.SIGNING_KEY = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        return user;
    }

    public String generateToken(User user) {
        String token;
        LocalDateTime now = LocalDateTime.now();
        JWTToken existingToken = jwtTokenRepository.findByUserId(user.getUserId());

        if (existingToken != null && now.isBefore(existingToken.getExpiresAt())) {
            token = existingToken.getToken();
        } else {
            token = generateNewToken(user);
            if (existingToken != null) {
                jwtTokenRepository.delete(existingToken);
            }
            saveToken(user, token);
        }
        return token;
    }

    private String generateNewToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS512)
                .compact();
    }

    public void saveToken(User user, String token) {
        JWTToken jwtToken = new JWTToken(user, token, LocalDateTime.now().plusHours(1));
        jwtTokenRepository.save(jwtToken);
    }

    public void logout(User user) {
        jwtTokenRepository.deleteByUserId(user.getUserId());
    }

    public boolean validateToken(String token) {
        try {
            System.err.println("VALIDATING TOKEN...");

            // Parse and validate the token
            Jwts.parserBuilder()
                .setSigningKey(SIGNING_KEY)
                .build()
                .parseClaimsJws(token);

            // Check if the token exists in the database and is not expired
            Optional<JWTToken> jwtToken = jwtTokenRepository.findByToken(token);
            if (jwtToken.isPresent()) {
                System.err.println("Token Expiry: " + jwtToken.get().getExpiresAt());
                System.err.println("Current Time: " + LocalDateTime.now());
                return jwtToken.get().getExpiresAt().isAfter(LocalDateTime.now());
            }

            return false;
        } catch (Exception e) {
            System.err.println("Token validation failed: " + e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SIGNING_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public User authenticateGoogleUser(String idToken) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        try {
            ResponseEntity<Map> responseEntity = restTemplate.getForEntity(url, Map.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                throw new RuntimeException("Failed to verify Google token");
            }
            Map<String, Object> body = responseEntity.getBody();

            // Validate audience
            String aud = (String) body.get("aud");
            if (googleClientId != null && !googleClientId.trim().isEmpty() && !googleClientId.equals("your-google-client-id.apps.googleusercontent.com")) {
                if (!googleClientId.equals(aud)) {
                    throw new RuntimeException("Google token audience mismatch");
                }
            }

            String email = (String) body.get("email");
            if (email == null) {
                throw new RuntimeException("Email not found in Google token");
            }

            // Find or create user
            Optional<User> existingUserOpt = userRepository.findByEmail(email);
            if (existingUserOpt.isPresent()) {
                return existingUserOpt.get();
            }

            // Check if username based on email is available
            String baseUsername = email.split("@")[0];
            String username = baseUsername;
            int counter = 1;
            while (userRepository.findByUsername(username).isPresent()) {
                username = baseUsername + counter;
                counter++;
            }

            // Create new user
            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setRole(com.example.demo.entity.Role.CUSTOMER);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            return userRepository.save(user);

        } catch (Exception e) {
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }
}
