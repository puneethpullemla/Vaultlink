package com.vaultlink.vaultlink.controller;

import com.vaultlink.vaultlink.dto.LoginRequest;
import com.vaultlink.vaultlink.dto.RegisterRequest;
import com.vaultlink.vaultlink.model.User;
import com.vaultlink.vaultlink.security.JwtService;
import com.vaultlink.vaultlink.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest request) {
        return authService.register(request.getName(), request.getEmail(), request.getPassword());
    }

    @PostMapping("/login")
    public java.util.Map<String, Object> login(@RequestBody LoginRequest request) {
        User user = authService.login(request.getEmail(), request.getPassword());
        String token = jwtService.generateToken(user.getId(), user.getEmail());

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        return response;
    }
}