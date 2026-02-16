package bda.cypher.healthAssistant.dto;

import jakarta.validation.constraints.NotBlank;

public class WorkoutCreateRequestDTO {
    @NotBlank(message = "Məşq adı boş ola bilməz")
    private String name;

    private String category;

    private Integer durationMinutes;

    private Integer calories;

    @NotBlank(message = "Başlama vaxtı boş ola bilməz")
    private String startTime;

    @NotBlank(message = "Bitmə vaxtı boş ola bilməz")
    private String endTime;

    @NotBlank(message = "Həftə günü boş ola bilməz")
    private String dayOfWeek;

    private String instructions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
