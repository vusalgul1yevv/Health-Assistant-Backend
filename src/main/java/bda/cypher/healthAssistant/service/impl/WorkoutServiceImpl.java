package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.WorkoutCreateRequestDTO;
import bda.cypher.healthAssistant.dto.WorkoutResponseDTO;
import bda.cypher.healthAssistant.dto.WorkoutUpdateRequestDTO;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.entity.Workout;
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.repository.WorkoutRepository;
import bda.cypher.healthAssistant.service.WorkoutService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class WorkoutServiceImpl implements WorkoutService {
    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService aiExecutor;
    private final String aiBaseUrl;
    private final String aiModel;
    private final String aiApiKey;
    private final boolean aiEnabled;
    private final long aiTimeoutMs;
    private final long aiCacheTtlHours;
    private final int aiMaxTokens;
    private final ConcurrentHashMap<String, CompletableFuture<Void>> aiInFlight = new ConcurrentHashMap<>();

    public WorkoutServiceImpl(WorkoutRepository workoutRepository,
                              UserRepository userRepository,
                              ObjectMapper objectMapper,
                              @Value("${ai.groq.base-url:https://api.groq.com/openai/v1/chat/completions}") String aiBaseUrl,
                              @Value("${ai.groq.model:}") String aiModel,
                              @Value("${ai.groq.api-key:}") String aiApiKey,
                              @Value("${ai.groq.enabled:false}") boolean aiEnabled,
                              @Value("${ai.groq.timeout-ms:10000}") long aiTimeoutMs,
                              @Value("${ai.groq.parallel-limit:5}") int aiParallelLimit,
                              @Value("${ai.groq.cache-ttl-hours:24}") long aiCacheTtlHours,
                              @Value("${ai.groq.max-tokens:2600}") int aiMaxTokens) {
        this.workoutRepository = workoutRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.aiBaseUrl = aiBaseUrl;
        this.aiModel = aiModel;
        this.aiApiKey = aiApiKey;
        this.aiEnabled = aiEnabled;
        this.aiTimeoutMs = aiTimeoutMs;
        this.aiCacheTtlHours = aiCacheTtlHours;
        this.aiMaxTokens = aiMaxTokens;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(aiTimeoutMs, 1000)))
                .build();
        this.aiExecutor = Executors.newFixedThreadPool(Math.max(aiParallelLimit, 1));
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
        if (!force) {
            List<Workout> ai = workoutRepository.findAllByUserIdAndWeekStartAndSourceOrderByDayOfWeekAsc(user.getId(), targetWeek, "ai");
            if (isValidAiPlan(ai)) {
                return ai.stream().map(this::mapToDTO).collect(Collectors.toList());
            }
        } else {
            workoutRepository.deleteByUserIdAndWeekStartAndSource(user.getId(), targetWeek, "ai");
        }
        triggerAiGeneration(user, targetWeek);
        List<Workout> core = getOrCreateDefaultPlan(user, targetWeek);
        return core.stream().map(this::mapToDTO).collect(Collectors.toList());
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

    private List<Workout> getOrCreateDefaultPlan(User user, LocalDate weekStart) {
        List<Workout> existing = workoutRepository.findAllByUserIdAndWeekStartAndSourceOrderByDayOfWeekAsc(user.getId(), weekStart, "default");
        if (existing.size() == 7) {
            return existing;
        }
        workoutRepository.deleteByUserIdAndWeekStartAndSource(user.getId(), weekStart, "default");
        Instant now = Instant.now();
        List<Workout> list = new ArrayList<>();
        list.add(defaultWorkout(user, weekStart, now, "Mon", "Səhər gəzintisi", "Açıq hava", 30, 150, "08:00", "08:30", "Parkda rahat tempdə 30 dəqiqə gəzin. Su qəbulunu unutmayın."));
        list.add(defaultWorkout(user, weekStart, now, "Tue", "Yüngül yoga", "Daxili - yüngül", 25, 100, "08:00", "08:25", "Yüngül stretching və nəfəs məşqləri edin. Ağrı olarsa dayanın."));
        list.add(defaultWorkout(user, weekStart, now, "Wed", "Bədən çəkisi məşqi", "Daxili - orta", 35, 200, "08:00", "08:35", "3 set: squat 12, push-up 10, plank 30s. Aralarda 60s istirahət."));
        list.add(defaultWorkout(user, weekStart, now, "Thu", "Sürətli gəzinti", "Açıq hava", 30, 170, "08:00", "08:30", "Sürətli tempdə gəzin. Nəbziniz çox yüksələrsə tempini azaldın."));
        list.add(defaultWorkout(user, weekStart, now, "Fri", "Mobilizasiya və stretching", "Daxili - yüngül", 20, 70, "08:00", "08:20", "Bel, boyun, çiyin və ayaq əzələləri üçün stretching edin."));
        list.add(defaultWorkout(user, weekStart, now, "Sat", "Velosiped və ya qaçış yolu", "Kardio", 30, 220, "10:00", "10:30", "Orta tempdə kardio edin. Əvvəl 5 dəqiqə isinmə, sonra əsas hissə."));
        list.add(defaultWorkout(user, weekStart, now, "Sun", "Aktiv istirahət", "Yüngül", 20, 80, "10:00", "10:20", "Yüngül gəzinti və stretching. Bədəni bərpa edin."));
        return workoutRepository.saveAll(list);
    }

    private Workout defaultWorkout(User user, LocalDate weekStart, Instant createdAt, String dayOfWeek, String name,
                                   String category, Integer durationMinutes, Integer calories, String startTime, String endTime,
                                   String instructions) {
        Workout w = new Workout();
        w.setUser(user);
        w.setWeekStart(weekStart);
        w.setSource("default");
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

    private void triggerAiGeneration(User user, LocalDate weekStart) {
        if (!aiEnabled || aiApiKey == null || aiApiKey.isBlank() || aiModel == null || aiModel.isBlank()) {
            return;
        }
        String key = user.getId() + ":" + weekStart;
        aiInFlight.computeIfAbsent(key, k -> CompletableFuture.runAsync(() -> {
            try {
                generateAiPlan(user, weekStart);
            } finally {
                aiInFlight.remove(k);
            }
        }, aiExecutor));
    }

    private void generateAiPlan(User user, LocalDate weekStart) {
        String content = callGroq(buildAiPrompt(user, weekStart));
        if (content == null || content.isBlank()) {
            return;
        }
        List<WorkoutPayload> workouts = parseAiWorkouts(content);
        if (!isValidAiWorkouts(workouts)) {
            return;
        }
        workoutRepository.deleteByUserIdAndWeekStartAndSource(user.getId(), weekStart, "ai");
        Instant now = Instant.now();
        List<Workout> entities = workouts.stream().map(w -> {
            Workout e = new Workout();
            e.setUser(user);
            e.setWeekStart(weekStart);
            e.setSource("ai");
            e.setCreatedAt(now);
            e.setDayOfWeek(w.dayOfWeek);
            e.setName(w.name);
            e.setCategory(w.category);
            e.setDurationMinutes(w.durationMinutes);
            e.setCalories(w.calories);
            e.setStartTime(w.startTime);
            e.setEndTime(w.endTime);
            e.setInstructions(w.instructions);
            return e;
        }).collect(Collectors.toList());
        workoutRepository.saveAll(entities);
    }

    private String buildAiPrompt(User user, LocalDate weekStart) {
        Integer age = user.getDateOfBirth() == null ? null : Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
        String gender = user.getGender();
        Double height = user.getHeight();
        Double weight = user.getWeight();
        String condition = user.getHealthCondition() != null ? user.getHealthCondition().getName() : null;
        String severity = user.getSeverity();
        StringBuilder builder = new StringBuilder();
        builder.append("Return ONLY raw JSON (no markdown). ");
        builder.append("All human-readable text must be in Azerbaijani (workout names, categories, instructions). ");
        builder.append("Create a 7-day workout plan as JSON with EXACTLY 7 items. ");
        builder.append("Use dayOfWeek values Mon,Tue,Wed,Thu,Fri,Sat,Sun. ");
        builder.append("Format: {\"workouts\":[{\"dayOfWeek\":\"Mon\",\"name\":\"...\",\"category\":\"...\",\"durationMinutes\":30,\"calories\":150,\"startTime\":\"08:00\",\"endTime\":\"08:30\",\"instructions\":\"...\"}]}. ");
        builder.append("Week start: ").append(weekStart).append(". ");
        if (age != null) builder.append("Age: ").append(age).append(". ");
        if (gender != null && !gender.isBlank()) builder.append("Gender: ").append(gender).append(". ");
        if (height != null) builder.append("HeightCm: ").append(height).append(". ");
        if (weight != null) builder.append("WeightKg: ").append(weight).append(". ");
        if (condition != null && !condition.isBlank()) builder.append("HealthCondition: ").append(condition).append(". ");
        if (severity != null && !severity.isBlank()) builder.append("Severity: ").append(severity).append(". ");
        builder.append("Keep instructions short (1-2 sentences). ");
        return builder.toString();
    }

    private String callGroq(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", aiModel,
                    "temperature", 0.4,
                    "max_tokens", aiMaxTokens,
                    "messages", List.of(
                            Map.of("role", "system", "content", "Return only raw JSON. No markdown. Use Azerbaijani for human-readable text."),
                            Map.of("role", "user", "content", prompt)
                    )
            );
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiBaseUrl))
                    .timeout(Duration.ofMillis(Math.max(aiTimeoutMs, 1000)))
                    .header("Authorization", "Bearer " + aiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.at("/choices/0/message/content");
            return contentNode.isMissingNode() ? null : contentNode.asText();
        } catch (Exception ex) {
            return null;
        }
    }

    private List<WorkoutPayload> parseAiWorkouts(String content) {
        try {
            String json = extractJson(content);
            JsonNode root = objectMapper.readTree(json);
            JsonNode workoutsNode = root.get("workouts");
            if (workoutsNode == null || !workoutsNode.isArray()) {
                return null;
            }
            List<WorkoutPayload> items = new ArrayList<>();
            for (JsonNode node : workoutsNode) {
                String day = normalizeDay(node.path("dayOfWeek").asText(null));
                String name = node.path("name").asText(null);
                if (day == null || name == null || name.isBlank()) {
                    continue;
                }
                WorkoutPayload w = new WorkoutPayload();
                w.dayOfWeek = day;
                w.name = name.trim();
                w.category = node.path("category").asText(null);
                w.durationMinutes = node.path("durationMinutes").isNumber() ? node.path("durationMinutes").asInt() : null;
                w.calories = node.path("calories").isNumber() ? node.path("calories").asInt() : null;
                w.startTime = node.path("startTime").asText(null);
                w.endTime = node.path("endTime").asText(null);
                w.instructions = node.path("instructions").asText(null);
                if (w.durationMinutes == null || w.durationMinutes <= 0) {
                    w.durationMinutes = 30;
                }
                if (w.startTime == null || w.startTime.isBlank()) {
                    w.startTime = "08:00";
                }
                if (w.endTime == null || w.endTime.isBlank()) {
                    w.endTime = "08:30";
                }
                items.add(w);
            }
            return items;
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isValidAiWorkouts(List<WorkoutPayload> workouts) {
        if (workouts == null || workouts.size() != 7) {
            return false;
        }
        Set<String> daySet = new HashSet<>();
        for (WorkoutPayload w : workouts) {
            if (w.dayOfWeek == null || w.name == null || w.name.isBlank()) {
                return false;
            }
            daySet.add(w.dayOfWeek);
        }
        return daySet.size() == 7;
    }

    private boolean isValidAiPlan(List<Workout> workouts) {
        if (workouts == null || workouts.size() != 7) {
            return false;
        }
        Instant latest = workouts.stream().map(Workout::getCreatedAt).max(Instant::compareTo).orElse(null);
        return isAiFresh(latest);
    }

    private boolean isAiFresh(Instant createdAt) {
        if (createdAt == null || aiCacheTtlHours <= 0) {
            return false;
        }
        return createdAt.isAfter(Instant.now().minus(Duration.ofHours(aiCacheTtlHours)));
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("{")) {
            return trimmed;
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String normalizeDay(String day) {
        if (day == null) {
            return null;
        }
        String value = day.trim().toLowerCase();
        return switch (value) {
            case "mon", "monday" -> "Mon";
            case "tue", "tues", "tuesday" -> "Tue";
            case "wed", "wednesday" -> "Wed";
            case "thu", "thur", "thurs", "thursday" -> "Thu";
            case "fri", "friday" -> "Fri";
            case "sat", "saturday" -> "Sat";
            case "sun", "sunday" -> "Sun";
            default -> null;
        };
    }

    private static class WorkoutPayload {
        String dayOfWeek;
        String name;
        String category;
        Integer durationMinutes;
        Integer calories;
        String startTime;
        String endTime;
        String instructions;
    }
}
