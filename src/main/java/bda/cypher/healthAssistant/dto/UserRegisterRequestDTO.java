package bda.cypher.healthAssistant.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class UserRegisterRequestDTO {
    @NotBlank(message = "Tam ad boş ola bilməz")
    private String fullName;

    @NotBlank(message = "Email boş ola bilməz")
    @Email(message = "Düzgün email formatı daxil edin")
    private String email;

    @NotBlank(message = "Şifrə boş ola bilməz")
    @Size(min = 6, message = "Şifrə ən azı 6 simvol olmalıdır")
    private String password;

    @NotNull(message = "Doğum tarixi qeyd olunmalıdır")
    @Past(message = "Doğum tarixi keçmişdə olmalıdır")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Cins seçilməlidir")
    private String gender;

    @NotNull(message = "Boy qeyd olunmalıdır")
    @Positive(message = "Boy müsbət ədəd olmalıdır")
    private Double height;

    @NotNull(message = "Çəki qeyd olunmalıdır")
    @Positive(message = "Çəki müsbət ədəd olmalıdır")
    private Double weight;

    @NotNull(message = "Xəstəlik seçilməlidir")
    private Long conditionId;

    @NotBlank(message = "Ağırlıq dərəcəsi seçilməlidir")
    private String severity;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public Long getConditionId() { return conditionId; }
    public void setConditionId(Long conditionId) { this.conditionId = conditionId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
