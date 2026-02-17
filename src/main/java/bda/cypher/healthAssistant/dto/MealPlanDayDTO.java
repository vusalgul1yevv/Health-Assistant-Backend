package bda.cypher.healthAssistant.dto;

import java.util.ArrayList;
import java.util.List;

public class MealPlanDayDTO {
    private String dayOfWeek;
    private List<MealPlanMealDTO> meals = new ArrayList<>();

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public List<MealPlanMealDTO> getMeals() {
        return meals;
    }

    public void setMeals(List<MealPlanMealDTO> meals) {
        this.meals = meals;
    }
}
