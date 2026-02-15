package bda.cypher.healthAssistant.controller;

import bda.cypher.healthAssistant.dto.MedicationCreateRequestDTO;
import bda.cypher.healthAssistant.dto.MedicationResponseDTO;
import bda.cypher.healthAssistant.service.MedicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
}
