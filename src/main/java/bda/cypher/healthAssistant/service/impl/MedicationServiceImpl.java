package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.MedicationCreateRequestDTO;
import bda.cypher.healthAssistant.dto.MedicationResponseDTO;
import bda.cypher.healthAssistant.dto.MedicationUpdateRequestDTO;
import bda.cypher.healthAssistant.entity.Medication;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.repository.MedicationRepository;
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.service.MedicationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicationServiceImpl implements MedicationService {
    private final MedicationRepository medicationRepository;
    private final UserRepository userRepository;

    public MedicationServiceImpl(MedicationRepository medicationRepository, UserRepository userRepository) {
        this.medicationRepository = medicationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public MedicationResponseDTO createMedication(String userEmail, MedicationCreateRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));

        Medication medication = new Medication();
        medication.setName(request.getName());
        medication.setDose(request.getDose());
        medication.setTime(request.getTime());
        medication.setFrequency(request.getFrequency());
        medication.setNotes(request.getNotes());
        medication.setIntakeCondition(request.getIntakeCondition());
        medication.setCreatedAt(Instant.now());
        medication.setUser(user);

        Medication saved = medicationRepository.save(medication);
        return mapToDTO(saved);
    }

    @Override
    public List<MedicationResponseDTO> getUserMedications(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));
        return medicationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public MedicationResponseDTO updateMedication(String userEmail, Long medicationId, MedicationUpdateRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));

        Medication medication = medicationRepository.findByIdAndUserId(medicationId, user.getId())
                .orElseThrow(() -> new RuntimeException("Dərman tapılmadı"));

        if (request.getName() != null && !request.getName().isBlank()) {
            medication.setName(request.getName());
        }
        if (request.getDose() != null) {
            medication.setDose(request.getDose());
        }
        if (request.getTime() != null && !request.getTime().isBlank()) {
            medication.setTime(request.getTime());
        }
        if (request.getFrequency() != null && !request.getFrequency().isBlank()) {
            medication.setFrequency(request.getFrequency());
        }
        if (request.getNotes() != null) {
            medication.setNotes(request.getNotes());
        }
        if (request.getIntakeCondition() != null) {
            medication.setIntakeCondition(request.getIntakeCondition());
        }

        Medication saved = medicationRepository.save(medication);
        return mapToDTO(saved);
    }

    @Override
    public void deleteMedication(String userEmail, Long medicationId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));
        long deleted = medicationRepository.deleteByIdAndUserId(medicationId, user.getId());
        if (deleted == 0) {
            throw new RuntimeException("Dərman tapılmadı");
        }
    }

    private MedicationResponseDTO mapToDTO(Medication medication) {
        MedicationResponseDTO dto = new MedicationResponseDTO();
        dto.setId(medication.getId());
        dto.setName(medication.getName());
        dto.setDose(medication.getDose());
        dto.setTime(medication.getTime());
        dto.setFrequency(medication.getFrequency());
        dto.setNotes(medication.getNotes());
        dto.setIntakeCondition(medication.getIntakeCondition());
        dto.setCreatedAt(medication.getCreatedAt());
        return dto;
    }
}
