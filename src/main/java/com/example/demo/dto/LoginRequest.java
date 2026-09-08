package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
	@NotBlank(message = "Username is required")
	private String username;
    @NotBlank(message = "Password is required")
    private String password;
    public LoginRequest() {
		// TODO Auto-generated constructor stub
	}
	public LoginRequest(String username, String password) {
		super();
		this.username = username;
		this.password = password;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
    
}
