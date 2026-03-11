package bda.cypher.healthAssistant.controller;

import bda.cypher.healthAssistant.dto.MealPlanDayDTO;
import bda.cypher.healthAssistant.dto.MealPlanMealDTO;
import bda.cypher.healthAssistant.dto.MealPlanResponseDTO;
import bda.cypher.healthAssistant.dto.MedicationResponseDTO;
import bda.cypher.healthAssistant.dto.WorkoutResponseDTO;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.service.MailService;
import bda.cypher.healthAssistant.service.MealPlanService;
import bda.cypher.healthAssistant.service.MedicationService;
import bda.cypher.healthAssistant.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
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
    private final MealPlanService mealPlanService;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Value("${notifications.enabled:true}")
    private boolean notificationsEnabled;

    @GetMapping("/today")
    public ResponseEntity<HomeScheduleResponse> getTodaySchedule(Authentication authentication) {
        ZoneId zoneId = ZoneId.of("Asia/Baku");
        LocalDate today = LocalDate.now(zoneId);
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        String shortDay = getShortDay(dayOfWeek);
        String fullDay = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<HomeScheduleItem> items = buildTodayItems(authentication.getName(), dayOfWeek, shortDay, fullDay, weekStart);

        items.sort(Comparator.comparing(item -> parseTime(item.time()), Comparator.nullsLast(Comparator.naturalOrder())));

        return ResponseEntity.ok(new HomeScheduleResponse(today.toString(), shortDay, items));
    }

    @Scheduled(fixedDelayString = "${notifications.check-interval-ms:60000}")
    public void sendScheduleNotifications() {
        if (!notificationsEnabled) {
            return;
        }
        ZoneId zoneId = ZoneId.of("Asia/Baku");
        LocalDate today = LocalDate.now(zoneId);
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        String shortDay = getShortDay(dayOfWeek);
        String fullDay = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalTime now = LocalTime.now(zoneId);
        List<User> users = userRepository.findAll();
        for (User user : users) {
            String email = user.getEmail();
            if (email == null || email.isBlank()) {
                continue;
            }
            List<HomeScheduleItem> items = buildTodayItems(email, dayOfWeek, shortDay, fullDay, weekStart);
            for (HomeScheduleItem item : items) {
                LocalTime itemTime = parseTime(item.time());
                if (itemTime == null) {
                    continue;
                }
                if (itemTime.getHour() == now.getHour() && itemTime.getMinute() == now.getMinute()) {
                    String subject = notificationSubject(item);
                    String content = notificationContent(item);
                    try {
                        mailService.sendScheduleNotification(email, subject, content);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private List<HomeScheduleItem> buildTodayItems(String email, DayOfWeek dayOfWeek, String shortDay, String fullDay, LocalDate weekStart) {
        List<MedicationResponseDTO> medications = medicationService.getUserMedications(email);
        List<WorkoutResponseDTO> workouts = workoutService.getUserWorkouts(email)
                .stream()
                .filter(workout -> weekStart != null && weekStart.equals(workout.getWeekStart()))
                .filter(workout -> matchesDay(workout.getDayOfWeek(), dayOfWeek, shortDay, fullDay))
                .collect(Collectors.toList());
        MealPlanResponseDTO plan = mealPlanService.getAiPlanByWeekStart(email, weekStart);

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
        if (plan != null && plan.getDays() != null) {
            for (MealPlanDayDTO day : plan.getDays()) {
                if (!matchesDay(day.getDayOfWeek(), dayOfWeek, shortDay, fullDay)) {
                    continue;
                }
                if (day.getMeals() == null) {
                    continue;
                }
                for (MealPlanMealDTO meal : day.getMeals()) {
                    String typeLabel = mealTypeLabel(meal.getMealType());
                    String mealTitle = meal.getTitle();
                    String title = typeLabel;
                    String subtitle = mealTitle == null || mealTitle.isBlank() ? null : mealTitle;
                    items.add(new HomeScheduleItem("MEAL", title, subtitle, meal.getTime(), null));
                }
            }
        }
        return items;
    }

    private String notificationSubject(HomeScheduleItem item) {
        return switch (item.type()) {
            case "MEAL" -> "Yemək vaxtı";
            case "MEDICATION" -> "Dərman vaxtı";
            case "WORKOUT" -> "Məşq vaxtı";
            default -> "Bildiriş";
        };
    }

    private String notificationContent(HomeScheduleItem item) {
        String title = item.title() == null ? "" : item.title();
        String time = item.time() == null ? "" : item.time();
        return switch (item.type()) {
            case "MEAL" -> "Yemək vaxtıdır: " + title + " (" + time + ")";
            case "MEDICATION" -> "Dərman vaxtıdır: " + title + " (" + time + ")";
            case "WORKOUT" -> "Məşq vaxtıdır: " + title + " (" + time + ")";
            default -> "Bildiriş vaxtıdır: " + title + " (" + time + ")";
        };
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

    private String mealTypeLabel(String mealType) {
        if (mealType == null) {
            return "Yemək";
        }
        return switch (mealType) {
            case "Breakfast" -> "Səhər yeməyi";
            case "Lunch" -> "Nahar";
            case "Dinner" -> "Şam yeməyi";
            default -> mealType;
        };
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
