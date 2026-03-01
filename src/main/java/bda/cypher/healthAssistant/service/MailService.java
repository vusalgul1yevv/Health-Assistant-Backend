package bda.cypher.healthAssistant.service;

public interface MailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
    void sendEmailOtp(String toEmail, String otp);
    void sendScheduleNotification(String toEmail, String subject, String content);
}
