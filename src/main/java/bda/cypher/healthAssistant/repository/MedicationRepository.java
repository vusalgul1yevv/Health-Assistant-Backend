package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
    List<Medication> findAllByUserEmailOrderByCreatedAtDesc(String email);
    Optional<Medication> findByIdAndUserEmail(Long id, String email);
    long deleteByIdAndUserEmail(Long id, String email);
}
