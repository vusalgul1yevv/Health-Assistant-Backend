package bda.cypher.healthAssistant.controller;

import bda.cypher.healthAssistant.dto.MedicationResponseDTO;
import bda.cypher.healthAssistant.dto.WorkoutResponseDTO;
import bda.cypher.healthAssistant.service.MedicationService;
import bda.cypher.healthAssistant.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {
    private final MedicationService medicationService;
    private final WorkoutService workoutService;

    @GetMapping("/today")
    public ResponseEntity<HomeScheduleResponse> getTodaySchedule(Authentication authentication) {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        String shortDay = getShortDay(dayOfWeek);
        String fullDay = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        List<MedicationResponseDTO> medications = medicationService.getUserMedications(authentication.getName());
        List<WorkoutResponseDTO> workouts = workoutService.getUserWorkouts(authentication.getName())
                .stream()
                .filter(workout -> matchesDay(workout.getDayOfWeek(), dayOfWeek, shortDay, fullDay))
                .collect(Collectors.toList());

        List<HomeScheduleItem> items = new ArrayList<>();
        for (MedicationResponseDTO medication : medications) {
            String title = medication.getName();
            if (medication.getDose() != null && !medication.getDose().isBlank()) {
                title = medication.getName() + " - " + medication.getDose();
            }
            String subtitle = buildMedicationSubtitle(medication);
            items.add(new HomeScheduleItem("MEDICATION", title, subtitle, medication.getTime(), null));
        }
        for (WorkoutResponseDTO workout : workouts) {
            String subtitle = buildWorkoutSubtitle(workout);
            items.add(new HomeScheduleItem("WORKOUT", workout.getName(), subtitle, workout.getStartTime(), workout.getEndTime()));
        }

        items.sort(Comparator.comparing(item -> parseTime(item.time()), Comparator.nullsLast(Comparator.naturalOrder())));

        return ResponseEntity.ok(new HomeScheduleResponse(today.toString(), shortDay, items));
    }

    private boolean matchesDay(String value, DayOfWeek dayOfWeek, String shortDay, String fullDay) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.equals(dayOfWeek.name().toLowerCase())
                || normalized.equals(shortDay.toLowerCase())
                || normalized.equals(fullDay.toLowerCase());
    }

    private String getShortDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Mon";
            case TUESDAY -> "Tue";
            case WEDNESDAY -> "Wed";
            case THURSDAY -> "Thu";
            case FRIDAY -> "Fri";
            case SATURDAY -> "Sat";
            case SUNDAY -> "Sun";
        };
    }

    private String buildMedicationSubtitle(MedicationResponseDTO medication) {
        String frequency = medication.getFrequency();
        String intake = medication.getIntakeCondition();
        if (frequency != null && !frequency.isBlank() && intake != null && !intake.isBlank()) {
            return frequency + " - " + intake;
        }
        if (frequency != null && !frequency.isBlank()) {
            return frequency;
        }
        if (intake != null && !intake.isBlank()) {
            return intake;
        }
        return null;
    }

    private String buildWorkoutSubtitle(WorkoutResponseDTO workout) {
        if (workout.getDurationMinutes() != null) {
            return workout.getDurationMinutes() + " min";
        }
        if (workout.getCategory() != null && !workout.getCategory().isBlank()) {
            return workout.getCategory();
        }
        return null;
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("HH:mm:ss"),
                DateTimeFormatter.ofPattern("h:mm a"),
                DateTimeFormatter.ofPattern("hh:mm a")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    public record HomeScheduleResponse(
            String date,
            String dayOfWeek,
            List<HomeScheduleItem> items
    ) {}

    public record HomeScheduleItem(
            String type,
            String title,
            String subtitle,
            String time,
            String endTime
    ) {}
}
