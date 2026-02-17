package bda.cypher.healthAssistant.service;

import bda.cypher.healthAssistant.dto.MealPlanGenerateRequestDTO;
import bda.cypher.healthAssistant.dto.MealPlanResponseDTO;
import bda.cypher.healthAssistant.dto.MealPlanUpdateRequestDTO;

import java.time.LocalDate;

public interface MealPlanService {
    MealPlanResponseDTO getCurrentPlan(String userEmail);
    MealPlanResponseDTO getPlanByWeekStart(String userEmail, LocalDate weekStart);
    MealPlanResponseDTO getPlanById(String userEmail, Long id);
    MealPlanResponseDTO generatePlan(String userEmail, MealPlanGenerateRequestDTO request);
    MealPlanResponseDTO updatePlan(String userEmail, Long id, MealPlanUpdateRequestDTO request);
    byte[] exportPlan(String userEmail, Long id);
}
