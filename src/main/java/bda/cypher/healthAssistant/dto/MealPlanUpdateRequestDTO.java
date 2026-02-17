package bda.cypher.healthAssistant.dto;

import java.util.ArrayList;
import java.util.List;

public class MealPlanUpdateRequestDTO {
    private List<MealPlanDayDTO> days = new ArrayList<>();

    public List<MealPlanDayDTO> getDays() {
        return days;
    }

    public void setDays(List<MealPlanDayDTO> days) {
        this.days = days;
    }
}
