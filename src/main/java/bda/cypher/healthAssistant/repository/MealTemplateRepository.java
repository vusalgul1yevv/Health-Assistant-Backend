package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.MealTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MealTemplateRepository extends JpaRepository<MealTemplate, Long> {
    Optional<MealTemplate> findByName(String name);
    List<MealTemplate> findAllByConditionsId(Long conditionId);
}
