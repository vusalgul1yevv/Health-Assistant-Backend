package bda.cypher.healthAssistant.controller;

import bda.cypher.healthAssistant.dto.WorkoutCreateRequestDTO;
import bda.cypher.healthAssistant.dto.WorkoutResponseDTO;
import bda.cypher.healthAssistant.dto.WorkoutUpdateRequestDTO;
import bda.cypher.healthAssistant.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/workouts")
@RequiredArgsConstructor
public class WorkoutController {
    private final WorkoutService workoutService;

    @PostMapping
    public ResponseEntity<WorkoutResponseDTO> createWorkout(@Valid @RequestBody WorkoutCreateRequestDTO request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(workoutService.createWorkout(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<WorkoutResponseDTO>> getMyWorkouts(Authentication authentication) {
        return ResponseEntity.ok(workoutService.getUserWorkouts(authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkoutResponseDTO> updateWorkout(@PathVariable Long id,
                                                            @RequestBody WorkoutUpdateRequestDTO request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(workoutService.updateWorkout(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkout(@PathVariable Long id, Authentication authentication) {
        workoutService.deleteWorkout(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
