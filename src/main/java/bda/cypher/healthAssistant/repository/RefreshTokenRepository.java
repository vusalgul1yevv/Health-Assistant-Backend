package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    int deleteByExpiresAtBefore(Instant cutoff);
    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);
}
