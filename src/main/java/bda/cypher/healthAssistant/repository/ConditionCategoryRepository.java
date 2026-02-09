package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.ConditionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConditionCategoryRepository extends JpaRepository<ConditionCategory, Long> {
}
