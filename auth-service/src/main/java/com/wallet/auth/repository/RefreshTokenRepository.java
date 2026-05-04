package com.wallet.auth.repository;

import com.wallet.auth.entity.RefreshToken;
import com.wallet.auth.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUserCredential(UserCredential userCredential);
    int deleteByUserCredential(UserCredential userCredential);
}
