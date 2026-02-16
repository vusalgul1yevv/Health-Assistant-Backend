package bda.cypher.healthAssistant.service;

import bda.cypher.healthAssistant.dto.WorkoutCreateRequestDTO;
import bda.cypher.healthAssistant.dto.WorkoutResponseDTO;
import bda.cypher.healthAssistant.dto.WorkoutUpdateRequestDTO;

import java.util.List;

public interface WorkoutService {
    WorkoutResponseDTO createWorkout(String userEmail, WorkoutCreateRequestDTO request);
    List<WorkoutResponseDTO> getUserWorkouts(String userEmail);
    WorkoutResponseDTO updateWorkout(String userEmail, Long workoutId, WorkoutUpdateRequestDTO request);
    void deleteWorkout(String userEmail, Long workoutId);
}
