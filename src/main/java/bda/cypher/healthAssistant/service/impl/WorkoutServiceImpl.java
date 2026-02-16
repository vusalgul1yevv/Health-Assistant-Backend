package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.WorkoutCreateRequestDTO;
import bda.cypher.healthAssistant.dto.WorkoutResponseDTO;
import bda.cypher.healthAssistant.dto.WorkoutUpdateRequestDTO;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.entity.Workout;
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.repository.WorkoutRepository;
import bda.cypher.healthAssistant.service.WorkoutService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkoutServiceImpl implements WorkoutService {
    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;

    public WorkoutServiceImpl(WorkoutRepository workoutRepository, UserRepository userRepository) {
        this.workoutRepository = workoutRepository;
        this.userRepository = userRepository;
    }

    @Override
    public WorkoutResponseDTO createWorkout(String userEmail, WorkoutCreateRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));

        Workout workout = new Workout();
        workout.setName(request.getName());
        workout.setCategory(request.getCategory());
        workout.setDurationMinutes(request.getDurationMinutes());
        workout.setCalories(request.getCalories());
        workout.setStartTime(request.getStartTime());
        workout.setEndTime(request.getEndTime());
        workout.setDayOfWeek(request.getDayOfWeek());
        workout.setInstructions(request.getInstructions());
        workout.setCreatedAt(Instant.now());
        workout.setUser(user);

        Workout saved = workoutRepository.save(workout);
        return mapToDTO(saved);
    }

    @Override
    public List<WorkoutResponseDTO> getUserWorkouts(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));

        return workoutRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public WorkoutResponseDTO updateWorkout(String userEmail, Long workoutId, WorkoutUpdateRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));

        Workout workout = workoutRepository.findByIdAndUserId(workoutId, user.getId())
                .orElseThrow(() -> new RuntimeException("Məşq tapılmadı"));

        if (request.getName() != null && !request.getName().isBlank()) {
            workout.setName(request.getName());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            workout.setCategory(request.getCategory());
        }
        if (request.getDurationMinutes() != null) {
            workout.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getCalories() != null) {
            workout.setCalories(request.getCalories());
        }
        if (request.getStartTime() != null && !request.getStartTime().isBlank()) {
            workout.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null && !request.getEndTime().isBlank()) {
            workout.setEndTime(request.getEndTime());
        }
        if (request.getDayOfWeek() != null && !request.getDayOfWeek().isBlank()) {
            workout.setDayOfWeek(request.getDayOfWeek());
        }
        if (request.getInstructions() != null) {
            workout.setInstructions(request.getInstructions());
        }

        Workout saved = workoutRepository.save(workout);
        return mapToDTO(saved);
    }

    @Override
    public void deleteWorkout(String userEmail, Long workoutId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));

        Workout workout = workoutRepository.findByIdAndUserId(workoutId, user.getId())
                .orElseThrow(() -> new RuntimeException("Məşq tapılmadı"));

        workoutRepository.delete(workout);
    }

    private WorkoutResponseDTO mapToDTO(Workout workout) {
        WorkoutResponseDTO dto = new WorkoutResponseDTO();
        dto.setId(workout.getId());
        dto.setName(workout.getName());
        dto.setCategory(workout.getCategory());
        dto.setDurationMinutes(workout.getDurationMinutes());
        dto.setCalories(workout.getCalories());
        dto.setStartTime(workout.getStartTime());
        dto.setEndTime(workout.getEndTime());
        dto.setDayOfWeek(workout.getDayOfWeek());
        dto.setInstructions(workout.getInstructions());
        dto.setCreatedAt(workout.getCreatedAt());
        return dto;
    }
}
