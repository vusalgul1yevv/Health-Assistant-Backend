package bda.cypher.healthAssistant.controller;

import bda.cypher.healthAssistant.dto.MealPlanGenerateRequestDTO;
import bda.cypher.healthAssistant.dto.MealPlanResponseDTO;
import bda.cypher.healthAssistant.dto.MealPlanUpdateRequestDTO;
import bda.cypher.healthAssistant.service.MealPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/meal-plans")
@RequiredArgsConstructor
public class MealPlanController {
    private final MealPlanService mealPlanService;

    @GetMapping("/current")
    public ResponseEntity<MealPlanResponseDTO> getCurrent(Authentication authentication) {
        return ResponseEntity.ok(mealPlanService.getCurrentPlan(authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<MealPlanResponseDTO> getByWeekStart(@RequestParam(required = false) LocalDate weekStart,
                                                              Authentication authentication) {
        if (weekStart == null) {
            return ResponseEntity.ok(mealPlanService.getCurrentPlan(authentication.getName()));
        }
        return ResponseEntity.ok(mealPlanService.getPlanByWeekStart(authentication.getName(), weekStart));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MealPlanResponseDTO> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(mealPlanService.getPlanById(authentication.getName(), id));
    }

    @PostMapping("/generate")
    public ResponseEntity<MealPlanResponseDTO> generate(@Valid @RequestBody MealPlanGenerateRequestDTO request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(mealPlanService.generatePlan(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MealPlanResponseDTO> update(@PathVariable Long id,
                                                      @Valid @RequestBody MealPlanUpdateRequestDTO request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(mealPlanService.updatePlan(authentication.getName(), id, request));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id,
                                         @RequestParam(required = false) String format,
                                         Authentication authentication) {
        byte[] content = mealPlanService.exportPlan(authentication.getName(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=meal-plan-" + id + ".txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }
}
