package bda.cypher.healthAssistant.controller;

import bda.cypher.healthAssistant.dto.MedicationCreateRequestDTO;
import bda.cypher.healthAssistant.dto.MedicationResponseDTO;
import bda.cypher.healthAssistant.dto.MedicationUpdateRequestDTO;
import bda.cypher.healthAssistant.service.MedicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/medications")
@RequiredArgsConstructor
public class MedicationController {
    private final MedicationService medicationService;

    @PostMapping
    public ResponseEntity<MedicationResponseDTO> createMedication(@Valid @RequestBody MedicationCreateRequestDTO request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(medicationService.createMedication(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<MedicationResponseDTO>> getMyMedications(Authentication authentication) {
        return ResponseEntity.ok(medicationService.getUserMedications(authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicationResponseDTO> updateMedication(@PathVariable Long id,
                                                                  @RequestBody MedicationUpdateRequestDTO request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(medicationService.updateMedication(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedication(@PathVariable Long id, Authentication authentication) {
        medicationService.deleteMedication(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
