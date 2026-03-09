package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.WorkoutTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, Long> {
    List<WorkoutTemplate> findAllByConditionsId(Long conditionId);
}

