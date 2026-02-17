package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    Optional<MealPlan> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);
    Optional<MealPlan> findByIdAndUserId(Long id, Long userId);
}
