package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
    List<Medication> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
