package com.CalisthenicList.CaliList.repositories;

import com.CalisthenicList.CaliList.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
	Optional<RefreshToken> findByToken(String token);
	Optional<RefreshToken> findByUserEmail(String email);
}
