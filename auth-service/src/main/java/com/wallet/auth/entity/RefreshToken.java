package com.wallet.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserCredential userCredential;

    public RefreshToken() {}

    public RefreshToken(Long id, String token, Instant expiryDate, UserCredential userCredential) {
        this.id = id;
        this.token = token;
        this.expiryDate = expiryDate;
        this.userCredential = userCredential;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }
    public UserCredential getUserCredential() { return userCredential; }
    public void setUserCredential(UserCredential userCredential) { this.userCredential = userCredential; }

    // Manual Builder
    public static RefreshTokenBuilder builder() {
        return new RefreshTokenBuilder();
    }

    public static class RefreshTokenBuilder {
        private Long id;
        private String token;
        private Instant expiryDate;
        private UserCredential userCredential;

        public RefreshTokenBuilder id(Long id) { this.id = id; return this; }
        public RefreshTokenBuilder token(String token) { this.token = token; return this; }
        public RefreshTokenBuilder expiryDate(Instant expiryDate) { this.expiryDate = expiryDate; return this; }
        public RefreshTokenBuilder userCredential(UserCredential userCredential) { this.userCredential = userCredential; return this; }

        public RefreshToken build() {
            return new RefreshToken(id, token, expiryDate, userCredential);
        }
    }
}
