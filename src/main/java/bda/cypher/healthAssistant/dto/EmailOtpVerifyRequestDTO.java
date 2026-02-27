package bda.cypher.healthAssistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class EmailOtpVerifyRequestDTO {
    @NotNull(message = "verificationId boş ola bilməz")
    private Long verificationId;

    @NotBlank(message = "OTP boş ola bilməz")
    @Pattern(regexp = "^\\d{6}$", message = "OTP 6 rəqəmdən ibarət olmalıdır")
    private String otp;

    public Long getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(Long verificationId) {
        this.verificationId = verificationId;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
