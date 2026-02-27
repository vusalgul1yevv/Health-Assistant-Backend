package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.ConditionCategoryTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConditionCategoryTranslationRepository extends JpaRepository<ConditionCategoryTranslation, Long> {
    Optional<ConditionCategoryTranslation> findByCategoryId(Long categoryId);
}
