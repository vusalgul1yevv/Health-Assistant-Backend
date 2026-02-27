package bda.cypher.healthAssistant.service;

import bda.cypher.healthAssistant.dto.WorkoutCreateRequestDTO;
import bda.cypher.healthAssistant.dto.WorkoutResponseDTO;
import bda.cypher.healthAssistant.dto.WorkoutUpdateRequestDTO;

import java.time.LocalDate;
import java.util.List;

public interface WorkoutService {
    WorkoutResponseDTO createWorkout(String userEmail, WorkoutCreateRequestDTO request);
    List<WorkoutResponseDTO> getUserWorkouts(String userEmail);
    List<WorkoutResponseDTO> getAiWorkoutPlanByWeekStart(String userEmail, LocalDate weekStart, boolean force);
    WorkoutResponseDTO updateWorkout(String userEmail, Long workoutId, WorkoutUpdateRequestDTO request);
    void deleteWorkout(String userEmail, Long workoutId);
}
