package bda.cypher.healthAssistant.service;

import bda.cypher.healthAssistant.dto.EmailOtpSendResponseDTO;
import bda.cypher.healthAssistant.dto.EmailOtpVerifyResponseDTO;

public interface EmailOtpService {
    EmailOtpSendResponseDTO sendOtp(String email, String ip, String userAgent);

    EmailOtpVerifyResponseDTO verifyOtp(Long verificationId, String otp);

    void consumeVerificationToken(String email, String verificationToken);
}
