package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.WorkoutCreateRequestDTO;
import bda.cypher.healthAssistant.dto.WorkoutResponseDTO;
import bda.cypher.healthAssistant.dto.WorkoutUpdateRequestDTO;
import bda.cypher.healthAssistant.entity.HealthCondition;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.entity.Workout;
import bda.cypher.healthAssistant.entity.WorkoutTemplate;
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.repository.WorkoutRepository;
import bda.cypher.healthAssistant.repository.WorkoutTemplateRepository;
import bda.cypher.healthAssistant.service.WorkoutService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkoutServiceImpl implements WorkoutService {
    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final WorkoutTemplateRepository workoutTemplateRepository;

    public WorkoutServiceImpl(WorkoutRepository workoutRepository,
                              UserRepository userRepository,
                              WorkoutTemplateRepository workoutTemplateRepository) {
        this.workoutRepository = workoutRepository;
        this.userRepository = userRepository;
        this.workoutTemplateRepository = workoutTemplateRepository;
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
        return workoutRepository.findAllByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<WorkoutResponseDTO> getAiWorkoutPlanByWeekStart(String userEmail, LocalDate weekStart, boolean force) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));
        LocalDate targetWeek = weekStart != null ? weekStart : currentWeekStart();
        String source = "template";
        if (!force) {
            List<Workout> existing = workoutRepository.findAllByUserIdAndWeekStartAndSourceOrderByDayOfWeekAsc(user.getId(), targetWeek, source);
            if (isValidTemplatePlan(existing)) {
                return existing.stream().map(this::mapToDTO).collect(Collectors.toList());
            }
        }
        workoutRepository.deleteByUserIdAndWeekStartAndSource(user.getId(), targetWeek, source);
        List<Workout> plan = createPlanFromTemplates(user, targetWeek, source, force);
        return plan.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public WorkoutResponseDTO updateWorkout(String userEmail, Long workoutId, WorkoutUpdateRequestDTO request) {
        Workout workout = workoutRepository.findByIdAndUserEmail(workoutId, userEmail)
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
        Workout workout = workoutRepository.findByIdAndUserEmail(workoutId, userEmail)
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
        dto.setWeekStart(workout.getWeekStart());
        dto.setSource(workout.getSource());
        dto.setInstructions(workout.getInstructions());
        dto.setCreatedAt(workout.getCreatedAt());
        return dto;
    }

    private LocalDate currentWeekStart() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private List<Workout> createPlanFromTemplates(User user, LocalDate weekStart, String source, boolean useRandom) {
        List<WorkoutTemplate> templates = new ArrayList<>();
        HealthCondition condition = user.getHealthCondition();
        if (condition != null && condition.getId() != null) {
            templates.addAll(workoutTemplateRepository.findAllByConditionsId(condition.getId()));
        }
        if (templates.isEmpty()) {
            templates.addAll(workoutTemplateRepository.findAll());
        }
        if (templates.isEmpty()) {
            return getOrCreateDefaultPlan(user, weekStart, source);
        }
        Instant now = Instant.now();
        Random random = useRandom
                ? new Random()
                : new Random(Objects.hash(user.getId(), weekStart));
        List<String> days = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        List<Workout> out = new ArrayList<>();
        for (String day : days) {
            List<WorkoutTemplate> dayTemplates = templates.stream()
                    .filter(t -> day.equalsIgnoreCase(t.getDayOfWeek()))
                    .collect(Collectors.toList());
            WorkoutTemplate pick = pickRandom(dayTemplates, random);
            if (pick == null) {
                pick = pickRandom(templates, random);
            }
            if (pick == null) {
                continue;
            }
            Workout w = new Workout();
            w.setUser(user);
            w.setWeekStart(weekStart);
            w.setSource(source);
            w.setCreatedAt(now);
            w.setDayOfWeek(day);
            w.setName(pick.getName());
            w.setCategory(pick.getCategory());
            w.setDurationMinutes(pick.getDurationMinutes());
            w.setCalories(pick.getCalories());
            w.setStartTime(pick.getStartTime());
            w.setEndTime(pick.getEndTime());
            w.setInstructions(pick.getInstructions());
            out.add(w);
        }
        if (!isValidTemplatePlan(out)) {
            return getOrCreateDefaultPlan(user, weekStart, source);
        }
        return workoutRepository.saveAll(out);
    }

    private WorkoutTemplate pickRandom(List<WorkoutTemplate> templates, Random random) {
        if (templates == null || templates.isEmpty()) {
            return null;
        }
        return templates.get(random.nextInt(templates.size()));
    }

    private boolean isValidTemplatePlan(List<Workout> workouts) {
        if (workouts == null || workouts.size() != 7) {
            return false;
        }
        Set<String> daySet = new HashSet<>();
        for (Workout w : workouts) {
            if (w.getDayOfWeek() == null || w.getDayOfWeek().isBlank() || w.getName() == null || w.getName().isBlank()) {
                return false;
            }
            daySet.add(w.getDayOfWeek());
        }
        return daySet.size() == 7;
    }

    private List<Workout> getOrCreateDefaultPlan(User user, LocalDate weekStart, String source) {
        List<Workout> existing = workoutRepository.findAllByUserIdAndWeekStartAndSourceOrderByDayOfWeekAsc(user.getId(), weekStart, source);
        if (existing.size() == 7) {
            return existing;
        }
        workoutRepository.deleteByUserIdAndWeekStartAndSource(user.getId(), weekStart, source);
        Instant now = Instant.now();
        List<Workout> list = new ArrayList<>();
        list.add(defaultWorkout(user, weekStart, now, "Mon", "Səhər gəzintisi", "Açıq hava", 30, 150, "08:00", "08:30", "Parkda rahat tempdə 30 dəqiqə gəzin. Su qəbulunu unutmayın.", source));
        list.add(defaultWorkout(user, weekStart, now, "Tue", "Yüngül yoga", "Daxili - yüngül", 25, 100, "08:00", "08:25", "Yüngül stretching və nəfəs məşqləri edin. Ağrı olarsa dayanın.", source));
        list.add(defaultWorkout(user, weekStart, now, "Wed", "Bədən çəkisi məşqi", "Daxili - orta", 35, 200, "08:00", "08:35", "3 set: squat 12, push-up 10, plank 30s. Aralarda 60s istirahət.", source));
        list.add(defaultWorkout(user, weekStart, now, "Thu", "Sürətli gəzinti", "Açıq hava", 30, 170, "08:00", "08:30", "Sürətli tempdə gəzin. Nəbziniz çox yüksələrsə tempini azaldın.", source));
        list.add(defaultWorkout(user, weekStart, now, "Fri", "Mobilizasiya və stretching", "Daxili - yüngül", 20, 70, "08:00", "08:20", "Bel, boyun, çiyin və ayaq əzələləri üçün stretching edin.", source));
        list.add(defaultWorkout(user, weekStart, now, "Sat", "Velosiped və ya qaçış yolu", "Kardio", 30, 220, "10:00", "10:30", "Orta tempdə kardio edin. Əvvəl 5 dəqiqə isinmə, sonra əsas hissə.", source));
        list.add(defaultWorkout(user, weekStart, now, "Sun", "Aktiv istirahət", "Yüngül", 20, 80, "10:00", "10:20", "Yüngül gəzinti və stretching. Bədəni bərpa edin.", source));
        return workoutRepository.saveAll(list);
    }

    private Workout defaultWorkout(User user, LocalDate weekStart, Instant createdAt, String dayOfWeek, String name,
                                   String category, Integer durationMinutes, Integer calories, String startTime, String endTime,
                                   String instructions, String source) {
        Workout w = new Workout();
        w.setUser(user);
        w.setWeekStart(weekStart);
        w.setSource(source);
        w.setCreatedAt(createdAt);
        w.setDayOfWeek(dayOfWeek);
        w.setName(name);
        w.setCategory(category);
        w.setDurationMinutes(durationMinutes);
        w.setCalories(calories);
        w.setStartTime(startTime);
        w.setEndTime(endTime);
        w.setInstructions(instructions);
        return w;
    }
}
