package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.entity.JWTToken;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.JWTTokenRepository;
import com.example.demo.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final Key SIGNING_KEY;

    // In-memory revocation list so logout takes effect immediately without a
    // DB round-trip on every request. Entries are keyed by raw token and
    // pruned once their own expiration passes (bounded by jwt.expiration).
    private final ConcurrentHashMap<String, Long> revokedTokens = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final JWTTokenRepository jwtTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    @Value("${google.client.id:}")
    private String googleClientId;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpirationMs;

    @Autowired
    public AuthService(UserRepository userRepository,
                       JWTTokenRepository jwtTokenRepository,
                       @Value("${jwt.secret}") String jwtSecret,
                       PasswordEncoder passwordEncoder,
                       RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.jwtTokenRepository = jwtTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;

        // Ensure the key length is at least 64 bytes
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalArgumentException("JWT_SECRET in application.properties must be at least 64 bytes long for HS512.");
        }
        this.SIGNING_KEY = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public AuthService(UserRepository userRepository, JWTTokenRepository jwtTokenRepository, String jwtSecret) {
        this(userRepository, jwtTokenRepository, jwtSecret, new BCryptPasswordEncoder(), new RestTemplate());
    }

    public User authenticate(String username, String password) {
        long totalStart = System.currentTimeMillis();

        long t0 = System.nanoTime();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
        long lookupMs = (System.nanoTime() - t0) / 1_000_000;

        long t1 = System.nanoTime();
        boolean matches = passwordEncoder.matches(password, user.getPassword());
        long pwMs = (System.nanoTime() - t1) / 1_000_000;

        long totalDurationMs = System.currentTimeMillis() - totalStart;

        logger.info("Login performance: username={}, userLookupMs={}, passwordVerifyMs={}, totalDurationMs={}",
                user.getUsername(), lookupMs, pwMs, totalDurationMs);

        if (!matches) {
            throw new RuntimeException("Invalid username or password");
        }
        return user;
    }

    public String generateToken(User user) {
        // Authentication validates JWT claims in memory, so persisting a token here
        // only adds two blocking database writes to every successful sign-in.
        return generateNewToken(user);
    }

    private String generateNewToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getUserId())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS512)
                .compact();
    }

    public void saveToken(User user, String token) {
        JWTToken jwtToken = new JWTToken(user, token, LocalDateTime.now().plusNanos(jwtExpirationMs * 1_000_000));
        jwtTokenRepository.save(jwtToken);
    }

    public void logout(User user, String token) {
        if (user != null && user.getUserId() != null) {
            jwtTokenRepository.deleteByUserId(user.getUserId());
        }
        revokeToken(token);
    }

    /**
     * Marks a token as revoked so it fails validation immediately, even
     * though it remains cryptographically valid until its natural expiry.
     */
    private void revokeToken(String token) {
        if (token == null) {
            return;
        }
        try {
            long expiryMs = parseClaims(token).getExpiration().getTime();
            revokedTokens.put(token, expiryMs);
        } catch (Exception e) {
            logger.debug("Could not record revocation for token: {}", e.getMessage());
        }
    }

    private boolean isRevoked(String token) {
        Long expiryMs = revokedTokens.get(token);
        if (expiryMs == null) {
            return false;
        }
        if (expiryMs < System.currentTimeMillis()) {
            // Naturally expired since revocation - safe to forget, crypto check will reject it anyway.
            revokedTokens.remove(token);
            return false;
        }
        return true;
    }

    /**
     * Validates JWT signature and expiration cryptographically in-memory
     * without querying the database.
     */
    public boolean validateToken(String token) {
        return validateTokenCryptographic(token);
    }

    public boolean validateTokenCryptographic(String token) {
        if (isRevoked(token)) {
            return false;
        }
        try {
            Jwts.parserBuilder()
                .setSigningKey(SIGNING_KEY)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            logger.debug("Cryptographic token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SIGNING_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts user details directly from verified JWT claims without a database query.
     * Falls back to database lookup only if the userId claim is absent (legacy tokens).
     */
    public User extractUserFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String username = claims.getSubject();
            String roleStr = claims.get("role", String.class);
            Object userIdObj = claims.get("userId");
            String email = claims.get("email", String.class);

            Integer userId = null;
            if (userIdObj instanceof Number num) {
                userId = num.intValue();
            } else if (userIdObj instanceof String str) {
                try {
                    userId = Integer.parseInt(str);
                } catch (NumberFormatException ignored) {}
            }

            Role role = Role.CUSTOMER;
            if (roleStr != null) {
                try {
                    role = Role.valueOf(roleStr.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            User user = new User();
            user.setUserId(userId);
            user.setUsername(username);
            user.setRole(role);
            user.setEmail(email);

            // Fallback for legacy tokens issued without userId claim
            if (userId == null && username != null) {
                userRepository.findByUsername(username).ifPresent(existing -> {
                    user.setUserId(existing.getUserId());
                    if (user.getEmail() == null) user.setEmail(existing.getEmail());
                });
            }

            return user;
        } catch (Exception e) {
            logger.debug("Failed to extract user from token claims: {}", e.getMessage());
            return null;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public User authenticateGoogleUser(String idToken) {
        return authenticateGoogleUser(idToken, null);
    }

    public User authenticateGoogleUser(String idToken, Role expectedRole) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        try {
            ResponseEntity<Map> responseEntity = restTemplate.getForEntity(url, Map.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                throw new RuntimeException("Failed to verify Google token");
            }
            Map<String, Object> body = responseEntity.getBody();

            String aud = (String) body.get("aud");
            if (googleClientId == null || googleClientId.trim().isEmpty()) {
                throw new RuntimeException("Google client ID is not configured");
            }
            if (!googleClientId.equals(aud)) {
                throw new RuntimeException("Google token audience mismatch");
            }
            String issuer = (String) body.get("iss");
            if (!"accounts.google.com".equals(issuer) && !"https://accounts.google.com".equals(issuer)) {
                throw new RuntimeException("Google token issuer mismatch");
            }
            if (!Boolean.parseBoolean(String.valueOf(body.get("email_verified")))) {
                throw new RuntimeException("Google email is not verified");
            }

            String email = (String) body.get("email");
            if (email == null) {
                throw new RuntimeException("Email not found in Google token");
            }

            Optional<User> existingUserOpt = userRepository.findByEmail(email);
            if (existingUserOpt.isPresent()) {
                User existing = existingUserOpt.get();
                if (expectedRole == Role.ADMIN && existing.getRole() != Role.ADMIN) {
                    throw new RuntimeException("Access denied. This Google account is not an admin.");
                }
                return existing;
            }

            if (expectedRole == Role.ADMIN) {
                throw new RuntimeException("No admin account exists for this Google email. Ask a super-admin to create one first.");
            }

            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "");
            if (baseUsername.isBlank()) {
                baseUsername = "user";
            }
            String username = baseUsername;
            int counter = 1;
            while (userRepository.findByUsername(username).isPresent()) {
                username = baseUsername + counter;
                counter++;
            }

            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setRole(Role.CUSTOMER);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            return userRepository.save(user);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }
}
