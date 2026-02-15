package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
    List<Medication> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Medication> findByIdAndUserId(Long id, Long userId);
    long deleteByIdAndUserId(Long id, Long userId);
}
