package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.HealthConditionTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthConditionTranslationRepository extends JpaRepository<HealthConditionTranslation, Long> {
    Optional<HealthConditionTranslation> findByConditionId(Long conditionId);
}
