package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {
    List<Workout> findAllByUserEmailOrderByCreatedAtDesc(String email);
    Optional<Workout> findByIdAndUserEmail(Long id, String email);

    List<Workout> findAllByUserIdAndWeekStartAndSourceOrderByDayOfWeekAsc(Long userId, LocalDate weekStart, String source);

    @Modifying
    @Query("delete from Workout w where w.user.id = :userId and w.weekStart = :weekStart and w.source = :source")
    int deleteByUserIdAndWeekStartAndSource(@Param("userId") Long userId, @Param("weekStart") LocalDate weekStart, @Param("source") String source);

    @Query("select max(w.createdAt) from Workout w where w.user.id = :userId and w.weekStart = :weekStart and w.source = :source")
    Instant findLatestCreatedAt(@Param("userId") Long userId, @Param("weekStart") LocalDate weekStart, @Param("source") String source);
}
