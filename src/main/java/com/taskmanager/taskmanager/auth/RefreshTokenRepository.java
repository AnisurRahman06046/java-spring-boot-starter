package com.taskmanager.taskmanager.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.taskmanager.taskmanager.user.User;


@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    // delete all refresh token for a user ; used for logout
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user=:user")
    void deleteByUser(User user);

    // count active token per user
    long countByUserAndRevokedFalse(User user);
}
