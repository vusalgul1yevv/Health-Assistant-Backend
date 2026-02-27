package bda.cypher.healthAssistant.config;

import bda.cypher.healthAssistant.repository.EmailOtpVerificationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "otp.enabled", havingValue = "true")
public class OtpCleanupScheduler {
    private final EmailOtpVerificationRepository repository;

    public OtpCleanupScheduler(EmailOtpVerificationRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${otp.cleanup-interval-ms:60000}")
    @Transactional
    public void cleanup() {
        repository.deleteExpiredOrUsed(Instant.now());
    }
}
