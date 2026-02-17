package bda.cypher.healthAssistant.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MealPlanResponseDTO {
    private Long id;
    private LocalDate weekStart;
    private List<MealPlanDayDTO> days = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public List<MealPlanDayDTO> getDays() {
        return days;
    }

    public void setDays(List<MealPlanDayDTO> days) {
        this.days = days;
    }
}
