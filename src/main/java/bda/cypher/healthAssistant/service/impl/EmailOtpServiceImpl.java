package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.EmailOtpSendResponseDTO;
import bda.cypher.healthAssistant.dto.EmailOtpVerifyResponseDTO;
import bda.cypher.healthAssistant.entity.EmailOtpVerification;
import bda.cypher.healthAssistant.repository.EmailOtpVerificationRepository;
import bda.cypher.healthAssistant.service.EmailOtpService;
import bda.cypher.healthAssistant.service.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class EmailOtpServiceImpl implements EmailOtpService {
    private final EmailOtpVerificationRepository repository;
    private final MailService mailService;
    private final String otpPepper;
    private final long otpTtlSeconds;
    private final long resendCooldownSeconds;
    private final int maxAttempts;
    private final long verificationTokenTtlSeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    public EmailOtpServiceImpl(EmailOtpVerificationRepository repository,
                               MailService mailService,
                               @Value("${jwt.secret}") String otpPepper,
                               @Value("${otp.ttl-seconds:300}") long otpTtlSeconds,
                               @Value("${otp.resend-cooldown-seconds:60}") long resendCooldownSeconds,
                               @Value("${otp.max-attempts:5}") int maxAttempts,
                               @Value("${otp.verification-token-ttl-seconds:600}") long verificationTokenTtlSeconds) {
        this.repository = repository;
        this.mailService = mailService;
        this.otpPepper = otpPepper;
        this.otpTtlSeconds = otpTtlSeconds;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxAttempts = maxAttempts;
        this.verificationTokenTtlSeconds = verificationTokenTtlSeconds;
    }

    @Override
    @Transactional
    public EmailOtpSendResponseDTO sendOtp(String email, String ip, String userAgent) {
        String normalizedEmail = normalizeEmail(email);
        Instant now = Instant.now();

        repository.deleteExpiredOrUsed(now);

        EmailOtpVerification existing = repository.findFirstByEmailOrderByCreatedAtDesc(normalizedEmail).orElse(null);
        if (existing != null && existing.getVerifiedAt() == null && existing.getExpiresAt().isAfter(now)) {
            if (existing.getResendAvailableAt() != null && existing.getResendAvailableAt().isAfter(now)) {
                long secondsLeft = Duration.between(now, existing.getResendAvailableAt()).toSeconds();
                throw new RuntimeException("OTP yenidən göndərmək üçün " + secondsLeft + " saniyə gözləyin");
            }
            repository.delete(existing);
        }

        String otp = generateOtp();
        EmailOtpVerification verification = new EmailOtpVerification();
        verification.setEmail(normalizedEmail);
        verification.setOtpHash(hashValue("otp", otp));
        verification.setCreatedAt(now);
        verification.setExpiresAt(now.plusSeconds(otpTtlSeconds));
        verification.setResendAvailableAt(now.plusSeconds(resendCooldownSeconds));
        verification.setAttemptCount(0);
        verification.setIp(ip);
        verification.setUserAgent(userAgent);
        EmailOtpVerification saved = repository.save(verification);

        mailService.sendEmailOtp(normalizedEmail, otp);

        return new EmailOtpSendResponseDTO(
                saved.getId(),
                saved.getExpiresAt(),
                saved.getResendAvailableAt(),
                resendCooldownSeconds
        );
    }

    @Override
    @Transactional
    public EmailOtpVerifyResponseDTO verifyOtp(Long verificationId, String otp) {
        Instant now = Instant.now();
        repository.deleteExpiredOrUsed(now);

        EmailOtpVerification verification = repository.findById(verificationId)
                .orElseThrow(() -> new RuntimeException("OTP tapılmadı"));

        if (verification.getVerificationTokenUsedAt() != null) {
            repository.delete(verification);
            throw new RuntimeException("OTP etibarsızdır");
        }
        if (verification.getVerifiedAt() != null) {
            throw new RuntimeException("OTP artıq təsdiqlənib");
        }
        if (!verification.getExpiresAt().isAfter(now)) {
            repository.delete(verification);
            throw new RuntimeException("OTP müddəti bitib");
        }

        if (verification.getAttemptCount() >= maxAttempts) {
            repository.delete(verification);
            throw new RuntimeException("OTP üçün cəhd limiti bitib");
        }

        String hash = hashValue("otp", otp);
        if (!hash.equals(verification.getOtpHash())) {
            verification.setAttemptCount(verification.getAttemptCount() + 1);
            if (verification.getAttemptCount() >= maxAttempts) {
                repository.delete(verification);
                throw new RuntimeException("OTP üçün cəhd limiti bitib");
            }
            repository.save(verification);
            throw new RuntimeException("OTP yanlışdır");
        }

        String verificationToken = generateVerificationToken();
        verification.setVerifiedAt(now);
        verification.setVerificationTokenHash(hashValue("token", verificationToken));
        verification.setVerificationTokenExpiresAt(now.plusSeconds(verificationTokenTtlSeconds));
        repository.save(verification);

        return new EmailOtpVerifyResponseDTO(verificationToken, verification.getVerificationTokenExpiresAt());
    }

    @Override
    @Transactional
    public void consumeVerificationToken(String email, String verificationToken) {
        if (verificationToken == null || verificationToken.isBlank()) {
            throw new RuntimeException("Email təsdiqi üçün token tələb olunur");
        }
        String normalizedEmail = normalizeEmail(email);
        Instant now = Instant.now();
        repository.deleteExpiredOrUsed(now);

        List<EmailOtpVerification> records = repository.findAllByEmailOrderByCreatedAtDesc(normalizedEmail);
        String tokenHash = hashValue("token", verificationToken);
        for (EmailOtpVerification record : records) {
            if (record.getVerifiedAt() == null) {
                continue;
            }
            if (record.getVerificationTokenUsedAt() != null) {
                continue;
            }
            if (record.getVerificationTokenHash() == null || record.getVerificationTokenExpiresAt() == null) {
                continue;
            }
            if (!record.getVerificationTokenExpiresAt().isAfter(now)) {
                continue;
            }
            if (!tokenHash.equals(record.getVerificationTokenHash())) {
                continue;
            }
            record.setVerificationTokenUsedAt(now);
            repository.delete(record);
            return;
        }
        throw new RuntimeException("Email təsdiqi tokeni etibarsızdır");
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String generateOtp() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private String generateVerificationToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashValue(String type, String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = type + ":" + raw + ":" + otpPepper;
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception ex) {
            throw new RuntimeException("Hash xətası");
        }
    }
}
