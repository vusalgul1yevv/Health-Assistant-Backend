package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.service.MailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SendGridMailService implements MailService {
    private final String sendGridApiKey;
    private final String mailFrom;

    public SendGridMailService(@Value("${sendgrid.api-key}") String sendGridApiKey,
                               @Value("${mail.from}") String mailFrom) {
        this.sendGridApiKey = sendGridApiKey;
        this.mailFrom = mailFrom;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        if (sendGridApiKey == null || sendGridApiKey.isBlank() || mailFrom == null || mailFrom.isBlank()) {
            throw new RuntimeException("Email konfigurasiya xətası");
        }

        Email from = new Email(mailFrom);
        Email to = new Email(toEmail);
        String subject = "Şifrəni yeniləyin";
        Content content = new Content("text/plain", "Şifrəni yeniləmək üçün link: " + resetLink);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sendGrid = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            if (response.getStatusCode() >= 400) {
                throw new RuntimeException("Email göndərilə bilmədi");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Email göndərilə bilmədi");
        }
    }

    @Override
    public void sendEmailOtp(String toEmail, String otp) {
        if (sendGridApiKey == null || sendGridApiKey.isBlank() || mailFrom == null || mailFrom.isBlank()) {
            throw new RuntimeException("Email konfigurasiya xətası");
        }

        Email from = new Email(mailFrom);
        Email to = new Email(toEmail);
        String subject = "Email təsdiq kodu";
        Content content = new Content("text/plain", "OTP kodunuz: " + otp + " (5 dəqiqə etibarlıdır)");
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sendGrid = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            if (response.getStatusCode() >= 400) {
                throw new RuntimeException("Email göndərilə bilmədi");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Email göndərilə bilmədi");
        }
    }

    @Override
    public void sendScheduleNotification(String toEmail, String subject, String content) {
        if (sendGridApiKey == null || sendGridApiKey.isBlank() || mailFrom == null || mailFrom.isBlank()) {
            throw new RuntimeException("Email konfigurasiya xətası");
        }

        Email from = new Email(mailFrom);
        Email to = new Email(toEmail);
        Content mailContent = new Content("text/plain", content);
        Mail mail = new Mail(from, subject, to, mailContent);

        SendGrid sendGrid = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            if (response.getStatusCode() >= 400) {
                throw new RuntimeException("Email göndərilə bilmədi");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Email göndərilə bilmədi");
        }
    }
}
