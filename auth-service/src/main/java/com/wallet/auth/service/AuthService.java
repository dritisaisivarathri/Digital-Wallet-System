package com.wallet.auth.service;

import com.wallet.auth.dto.AuthRequest;
import com.wallet.auth.dto.AuthResponse;
import com.wallet.auth.dto.RegisterRequest;
import com.wallet.auth.dto.UpdateProfileRequest;
import com.wallet.auth.entity.UserCredential;
import com.wallet.auth.repository.UserCredentialRepository;
import com.wallet.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserCredentialRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public String saveUser(RegisterRequest request) {
        Optional<UserCredential> existing = repository.findByEmailIgnoreCase(request.getEmail());
        if(existing.isPresent()){
            throw new RuntimeException("User already exists with email " + request.getEmail());
        }
        if(repository.findByUsernameIgnoreCase(request.getUsername()).isPresent()){
            throw new RuntimeException("User already exists with username " + request.getUsername());
        }
        UserCredential credential = new UserCredential();
        credential.setUsername(request.getUsername());
        credential.setEmail(request.getEmail());
        credential.setPassword(passwordEncoder.encode(request.getPassword()));
        credential.setRole(request.getRole() == null || request.getRole().isEmpty() ? "USER" : request.getRole().toUpperCase());
        credential.setStatus("PENDING_KYC");
        
        repository.saveAndFlush(credential);

        return "User registration successful";
    }

    public AuthResponse login(AuthRequest request) {
        UserCredential user = repository.findByUsernameIgnoreCase(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getId().toString(), user.getFullName(), user.getPhoneNumber());
        return new AuthResponse(token, user.getId().toString(), user.getEmail(), user.getRole(), user.getFullName(), user.getPhoneNumber());
    }

    public void updateUserStatus(java.util.UUID userId, String status) {
        UserCredential user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setStatus(status);
        repository.save(user);
    }

    public UserCredential getProfile(java.util.UUID userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));
    }

    public UserCredential updateProfile(java.util.UUID userId, UpdateProfileRequest request) {
        UserCredential user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        
        return repository.save(user);
    }
}
