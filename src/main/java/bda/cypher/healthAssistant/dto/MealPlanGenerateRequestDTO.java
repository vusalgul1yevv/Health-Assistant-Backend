package bda.cypher.healthAssistant.dto;

import java.time.LocalDate;

public class MealPlanGenerateRequestDTO {
    private LocalDate weekStart;

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }
}
