package bda.cypher.healthAssistant.dto;

import java.time.Instant;

public class EmailOtpVerifyResponseDTO {
    private String verificationToken;
    private Instant expiresAt;

    public EmailOtpVerifyResponseDTO() {
    }

    public EmailOtpVerifyResponseDTO(String verificationToken, Instant expiresAt) {
        this.verificationToken = verificationToken;
        this.expiresAt = expiresAt;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
