package bda.cypher.healthAssistant.service;

public interface MailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
