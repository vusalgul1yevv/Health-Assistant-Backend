package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.EmailOtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EmailOtpVerificationRepository extends JpaRepository<EmailOtpVerification, Long> {
    List<EmailOtpVerification> findAllByEmailOrderByCreatedAtDesc(String email);

    Optional<EmailOtpVerification> findFirstByEmailOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("""
            delete from EmailOtpVerification e
            where e.expiresAt <= :now
               or e.verificationTokenUsedAt is not null
               or (e.verificationTokenExpiresAt is not null and e.verificationTokenExpiresAt <= :now)
            """)
    int deleteExpiredOrUsed(@Param("now") Instant now);
}
