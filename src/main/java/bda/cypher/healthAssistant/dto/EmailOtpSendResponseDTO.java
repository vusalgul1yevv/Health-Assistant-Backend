package bda.cypher.healthAssistant.dto;

import java.time.Instant;

public class EmailOtpSendResponseDTO {
    private Long verificationId;
    private Instant expiresAt;
    private Instant resendAvailableAt;
    private long resendCooldownSeconds;

    public EmailOtpSendResponseDTO() {
    }

    public EmailOtpSendResponseDTO(Long verificationId, Instant expiresAt, Instant resendAvailableAt, long resendCooldownSeconds) {
        this.verificationId = verificationId;
        this.expiresAt = expiresAt;
        this.resendAvailableAt = resendAvailableAt;
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public Long getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(Long verificationId) {
        this.verificationId = verificationId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getResendAvailableAt() {
        return resendAvailableAt;
    }

    public void setResendAvailableAt(Instant resendAvailableAt) {
        this.resendAvailableAt = resendAvailableAt;
    }

    public long getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(long resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }
}
