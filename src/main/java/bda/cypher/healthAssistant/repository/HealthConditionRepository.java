package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.HealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthConditionRepository extends JpaRepository<HealthCondition, Long> {
    Optional<HealthCondition> findByNameAndCategoryId(String name, Long categoryId);
    List<HealthCondition> findAllByNameAndCategoryId(String name, Long categoryId);
}
