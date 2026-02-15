package bda.cypher.healthAssistant.service;

import bda.cypher.healthAssistant.dto.MedicationCreateRequestDTO;
import bda.cypher.healthAssistant.dto.MedicationResponseDTO;

import java.util.List;

public interface MedicationService {
    MedicationResponseDTO createMedication(String userEmail, MedicationCreateRequestDTO request);
    List<MedicationResponseDTO> getUserMedications(String userEmail);
}
