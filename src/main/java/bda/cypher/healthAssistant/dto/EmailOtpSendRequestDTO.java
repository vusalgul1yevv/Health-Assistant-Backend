package bda.cypher.healthAssistant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class EmailOtpSendRequestDTO {
    @NotBlank(message = "Email boş ola bilməz")
    @Email(message = "Düzgün email formatı daxil edin")
    @Pattern(regexp = "(?i)^[A-Z0-9._%+-]+@gmail\\.com$", message = "Yalnız gmail.com email qəbul olunur")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
