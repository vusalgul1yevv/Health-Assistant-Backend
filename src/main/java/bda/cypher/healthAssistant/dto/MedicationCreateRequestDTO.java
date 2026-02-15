package bda.cypher.healthAssistant.dto;

import jakarta.validation.constraints.NotBlank;

public class MedicationCreateRequestDTO {
    @NotBlank(message = "Dərman adı boş ola bilməz")
    private String name;

    private String dose;

    @NotBlank(message = "Vaxt boş ola bilməz")
    private String time;

    @NotBlank(message = "Tezlik boş ola bilməz")
    private String frequency;

    private String notes;

    private String intakeCondition;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String dose) {
        this.dose = dose;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getIntakeCondition() {
        return intakeCondition;
    }

    public void setIntakeCondition(String intakeCondition) {
        this.intakeCondition = intakeCondition;
    }
}
