package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "jwt_tokens", indexes = {
    @Index(name = "idx_jwt_tokens_token", columnList = "token"),
    @Index(name = "idx_jwt_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_jwt_tokens_expires_at", columnList = "expiresAt")
})
public class JWTToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Specifies that the tokenId will be auto-generated.
    private Integer tokenId; // Stores the unique identifier for each token.

    @ManyToOne // Establishes a many-to-one relationship with the User entity.
    @JoinColumn(name = "user_id", nullable = false) // Links the token to a specific user in the Users table.
    private User user; // Represents the user associated with the token.

    @Column(nullable = false, length = 1000) // Ensures that the token cannot be null and fits JWT claims.
    private String token; // Stores the JWT token string.

    @Column(nullable = false) // Ensures that the expiration time cannot be null.
    private LocalDateTime expiresAt; // Stores the expiration time of the token.

	public JWTToken(Integer tokenId, User user, String token, LocalDateTime expiresAt) {
		super();
		this.tokenId = tokenId;
		this.user = user;
		this.token = token;
		this.expiresAt = expiresAt;
	}

	public JWTToken(User user, String token, LocalDateTime expiresAt) {
		super();
		this.user = user;
		this.token = token;
		this.expiresAt = expiresAt;
	}

	public Integer getTokenId() {
		return tokenId;
	}

	public void setTokenId(Integer tokenId) {
		this.tokenId = tokenId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

    // Getters and Setters
    public JWTToken() {
		// TODO Auto-generated constructor stub
	}
    
    
}