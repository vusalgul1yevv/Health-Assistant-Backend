package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {
    List<Workout> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Workout> findByIdAndUserId(Long id, Long userId);
}
