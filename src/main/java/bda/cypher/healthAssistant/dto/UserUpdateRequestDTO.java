package bda.cypher.healthAssistant.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public class UserUpdateRequestDTO {
    private String fullName;
    @Past(message = "Doğum tarixi keçmişdə olmalıdır")
    private LocalDate dateOfBirth;
    private String gender;
    @Positive(message = "Boy müsbət ədəd olmalıdır")
    @DecimalMin(value = "50", message = "Boy 50 ilə 250 arası olmalıdır")
    @DecimalMax(value = "250", message = "Boy 50 ilə 250 arası olmalıdır")
    private Double height;
    @Positive(message = "Çəki müsbət ədəd olmalıdır")
    @DecimalMin(value = "20", message = "Çəki 20 ilə 400 arası olmalıdır")
    @DecimalMax(value = "300", message = "Çəki 20 ilə 400 arası olmalıdır")
    private Double weight;
    private Long conditionId;
    private String severity;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

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
