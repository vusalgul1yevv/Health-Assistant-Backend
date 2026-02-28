package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.MealPlanDayDTO;
import bda.cypher.healthAssistant.dto.MealPlanGenerateRequestDTO;
import bda.cypher.healthAssistant.dto.MealPlanMealDTO;
import bda.cypher.healthAssistant.dto.MealPlanResponseDTO;
import bda.cypher.healthAssistant.dto.MealPlanUpdateRequestDTO;
import bda.cypher.healthAssistant.entity.MealPlan;
import bda.cypher.healthAssistant.entity.MealPlanDay;
import bda.cypher.healthAssistant.entity.MealPlanMeal;
import bda.cypher.healthAssistant.entity.MealTemplate;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.repository.MealPlanRepository;
import bda.cypher.healthAssistant.repository.MealTemplateRepository;
import bda.cypher.healthAssistant.repository.ShoppingListRepository;
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.service.MealPlanService;
import bda.cypher.healthAssistant.entity.ShoppingList;
import bda.cypher.healthAssistant.entity.ShoppingCategory;
import bda.cypher.healthAssistant.entity.ShoppingItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class MealPlanServiceImpl implements MealPlanService {
    private static final Pattern AZ_CHAR_PATTERN = Pattern.compile("[əğıöüşç]");
    private static final List<String> AZ_HINTS = List.of(
            "səhər", "seher", "nahar", "şam", "sam", "yemək", "yemek", "gəzinti", "gezinti",
            "məşq", "mesq", "yüngül", "yungul", "dəqiqə", "deqiqe", "təlimat", "telimat",
            "tərəv", "terevez", "meyv", "süd", "sud", "qoz", "fındıq", "findiq", "sağlam", "saglam"
    );
    private final MealPlanRepository mealPlanRepository;
    private final UserRepository userRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final MealTemplateRepository mealTemplateRepository;
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

    public MealPlanServiceImpl(MealPlanRepository mealPlanRepository,
                               UserRepository userRepository,
                               ShoppingListRepository shoppingListRepository,
                               MealTemplateRepository mealTemplateRepository,
                               ObjectMapper objectMapper,
                               @Value("${ai.groq.base-url:https://api.groq.com/openai/v1/chat/completions}") String aiBaseUrl,
                               @Value("${ai.groq.model:}") String aiModel,
                               @Value("${ai.groq.api-key:}") String aiApiKey,
                               @Value("${ai.groq.enabled:false}") boolean aiEnabled,
                               @Value("${ai.groq.timeout-ms:10000}") long aiTimeoutMs,
                               @Value("${ai.groq.parallel-limit:5}") int aiParallelLimit,
                               @Value("${ai.groq.cache-ttl-hours:24}") long aiCacheTtlHours,
                               @Value("${ai.groq.max-tokens:2600}") int aiMaxTokens) {
        this.mealPlanRepository = mealPlanRepository;
        this.userRepository = userRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.mealTemplateRepository = mealTemplateRepository;
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
    @Transactional
    public MealPlanResponseDTO getCurrentPlan(String userEmail) {
        User user = getUser(userEmail);
        LocalDate weekStart = currentWeekStart();
        MealPlan plan = getOrCreateCorePlan(user, weekStart);
        return mapToDTO(plan);
    }

    @Override
    @Transactional
    public MealPlanResponseDTO getPlanByWeekStart(String userEmail, LocalDate weekStart) {
        User user = getUser(userEmail);
        MealPlan plan = getOrCreateCorePlan(user, weekStart);
        return mapToDTO(plan);
    }

    @Override
    @Transactional
    public MealPlanResponseDTO getAiPlanByWeekStart(String userEmail, LocalDate weekStart) {
        return getAiPlanByWeekStart(userEmail, weekStart, false);
    }

    @Override
    @Transactional
    public MealPlanResponseDTO getAiPlanByWeekStart(String userEmail, LocalDate weekStart, boolean force) {
        User user = getUser(userEmail);
        LocalDate targetWeek = weekStart != null ? weekStart : currentWeekStart();
        MealPlan aiPlan = mealPlanRepository.findByUserIdAndWeekStartAndSource(user.getId(), targetWeek, "ai")
                .orElse(null);
        if (aiPlan != null && !force) {
            return mapToDTO(aiPlan);
        }
        if (aiPlan != null) {
            mealPlanRepository.delete(aiPlan);
        }
        MealPlan preset = createPresetPlan(user, targetWeek);
        return mapToDTO(preset);
    }

    @Override
    public MealPlanResponseDTO getPlanById(String userEmail, Long id) {
        User user = getUser(userEmail);
        MealPlan plan = mealPlanRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Plan tapılmadı"));
        return mapToDTO(plan);
    }

    @Override
    @Transactional
    public MealPlanResponseDTO generatePlan(String userEmail, MealPlanGenerateRequestDTO request) {
        User user = getUser(userEmail);
        LocalDate weekStart = request.getWeekStart() != null ? request.getWeekStart() : currentWeekStart();
        MealPlan plan = mealPlanRepository.findByUserIdAndWeekStartAndSource(user.getId(), weekStart, "generated")
                .orElseGet(() -> createDefaultPlan(user, weekStart, "generated"));
        plan.setSource("generated");
        plan.setCreatedAt(Instant.now());
        plan.getDays().clear();
        plan.getDays().addAll(defaultDays(plan));
        MealPlan saved = mealPlanRepository.save(plan);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public MealPlanResponseDTO updatePlan(String userEmail, Long id, MealPlanUpdateRequestDTO request) {
        User user = getUser(userEmail);
        MealPlan plan = mealPlanRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Plan tapılmadı"));
        plan.getDays().clear();
        plan.getDays().addAll(mapDays(plan, request.getDays()));
        MealPlan saved = mealPlanRepository.save(plan);
        return mapToDTO(saved);
    }

    @Override
    public byte[] exportPlan(String userEmail, Long id) {
        User user = getUser(userEmail);
        MealPlan plan = mealPlanRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Plan tapılmadı"));
        StringBuilder builder = new StringBuilder();
        builder.append("WeekStart: ").append(plan.getWeekStart()).append("\n");
        for (MealPlanDay day : plan.getDays()) {
            builder.append(day.getDayOfWeek()).append("\n");
            for (MealPlanMeal meal : day.getMeals()) {
                builder.append(" - ")
                        .append(meal.getMealType())
                        .append(" | ")
                        .append(meal.getTitle() == null || meal.getTitle().isBlank() ? defaultMealTitle(meal.getMealType()) : meal.getTitle());
                if (meal.getTime() != null && !meal.getTime().isBlank()) {
                    builder.append(" | ").append(meal.getTime());
                }
                builder.append("\n");
            }
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User tapılmadı"));
    }

    private MealPlan getOrCreateCorePlan(User user, LocalDate weekStart) {
        MealPlan generated = mealPlanRepository.findByUserIdAndWeekStartAndSource(user.getId(), weekStart, "generated")
                .orElse(null);
        if (generated != null) {
            return generated;
        }
        MealPlan defaultPlan = mealPlanRepository.findByUserIdAndWeekStartAndSource(user.getId(), weekStart, "default")
                .orElse(null);
        if (defaultPlan != null) {
            return defaultPlan;
        }
        return mealPlanRepository.save(createDefaultPlan(user, weekStart, "default"));
    }

    private LocalDate currentWeekStart() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private MealPlan createDefaultPlan(User user, LocalDate weekStart, String source) {
        MealPlan plan = new MealPlan();
        plan.setUser(user);
        plan.setWeekStart(weekStart);
        plan.setCreatedAt(Instant.now());
        plan.setSource(source);
        plan.getDays().addAll(defaultDays(plan));
        return plan;
    }

    private MealPlan createPresetPlan(User user, LocalDate weekStart) {
        if (user.getHealthCondition() == null) {
            return mealPlanRepository.save(createDefaultPlan(user, weekStart, "ai"));
        }
        List<MealTemplate> templates = mealTemplateRepository.findAllByConditionsId(user.getHealthCondition().getId());
        MealPlan plan = mealPlanRepository.findByUserIdAndWeekStartAndSource(user.getId(), weekStart, "ai")
                .orElseGet(() -> createDefaultPlan(user, weekStart, "ai"));
        plan.setSource("ai");
        plan.setCreatedAt(Instant.now());
        plan.getDays().clear();
        if (templates.isEmpty()) {
            plan.getDays().addAll(defaultDays(plan));
            return mealPlanRepository.save(plan);
        }
        Map<String, List<MealTemplate>> byType = templates.stream()
                .collect(Collectors.groupingBy(MealTemplate::getMealType));
        List<String> days = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        List<String> types = List.of("Breakfast", "Lunch", "Dinner");
        List<Ingredient> ingredients = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            String dayName = days.get(dayIndex);
            MealPlanDay day = new MealPlanDay();
            day.setMealPlan(plan);
            day.setDayOfWeek(dayName);
            for (String type : types) {
                List<MealTemplate> list = byType.getOrDefault(type, new ArrayList<>());
                if (list.isEmpty()) {
                    continue;
                }
                int baseIndex = Math.floorMod(Objects.hash(user.getHealthCondition().getId(), weekStart.toString(), type), list.size());
                int selected = (baseIndex + dayIndex) % list.size();
                MealTemplate template = list.get(selected);
                MealPlanMeal meal = new MealPlanMeal();
                meal.setMealPlanDay(day);
                meal.setMealType(type);
                meal.setTitle(template.getName());
                meal.setTime(null);
                day.getMeals().add(meal);
                template.getIngredients().forEach(ing -> {
                    if (ing.getName() != null && !ing.getName().isBlank()) {
                        ingredients.add(new Ingredient(dayName, ing.getName().trim(),
                                ing.getQuantity() == null ? "" : ing.getQuantity().trim(),
                                normalizeCategory(ing.getCategory())));
                    }
                });
            }
            plan.getDays().add(day);
        }
        MealPlan saved = mealPlanRepository.save(plan);
        if (!ingredients.isEmpty()) {
            updateShoppingListFromIngredients(user, weekStart, ingredients);
        }
        return saved;
    }

    private List<MealPlanDay> defaultDays(MealPlan plan) {
        List<MealPlanDay> days = new ArrayList<>();
        days.add(createDay(plan, "Mon"));
        days.add(createDay(plan, "Tue"));
        days.add(createDay(plan, "Wed"));
        days.add(createDay(plan, "Thu"));
        days.add(createDay(plan, "Fri"));
        days.add(createDay(plan, "Sat"));
        days.add(createDay(plan, "Sun"));
        return days;
    }

    private MealPlanDay createDay(MealPlan plan, String dayOfWeek) {
        MealPlanDay day = new MealPlanDay();
        day.setDayOfWeek(dayOfWeek);
        day.setMealPlan(plan);
        day.getMeals().add(createMeal(day, "Breakfast", defaultMealTitle("Breakfast"), "07:00 AM"));
        day.getMeals().add(createMeal(day, "Lunch", defaultMealTitle("Lunch"), "12:00 PM"));
        day.getMeals().add(createMeal(day, "Dinner", defaultMealTitle("Dinner"), "06:00 PM"));
        return day;
    }

    private MealPlanMeal createMeal(MealPlanDay day, String mealType, String title, String time) {
        MealPlanMeal meal = new MealPlanMeal();
        meal.setMealPlanDay(day);
        meal.setMealType(mealType);
        meal.setTitle(title);
        meal.setTime(time);
        return meal;
    }

    private List<MealPlanDay> mapDays(MealPlan plan, List<MealPlanDayDTO> days) {
        if (days == null) {
            return new ArrayList<>();
        }
        return days.stream().map(dayDto -> {
            MealPlanDay day = new MealPlanDay();
            day.setMealPlan(plan);
            day.setDayOfWeek(dayDto.getDayOfWeek());
            day.getMeals().addAll(mapMeals(day, dayDto.getMeals()));
            return day;
        }).collect(Collectors.toList());
    }

    private List<MealPlanMeal> mapMeals(MealPlanDay day, List<MealPlanMealDTO> meals) {
        if (meals == null) {
            return new ArrayList<>();
        }
        return meals.stream().map(mealDto -> {
            MealPlanMeal meal = new MealPlanMeal();
            meal.setMealPlanDay(day);
            meal.setMealType(mealDto.getMealType());
            meal.setTitle(mealDto.getTitle());
            meal.setTime(mealDto.getTime());
            return meal;
        }).collect(Collectors.toList());
    }

    private MealPlanResponseDTO mapToDTO(MealPlan plan) {
        MealPlanResponseDTO dto = new MealPlanResponseDTO();
        dto.setId(plan.getId());
        dto.setWeekStart(plan.getWeekStart());
        dto.setSource(plan.getSource());
        List<MealPlanDayDTO> days = plan.getDays().stream().map(day -> {
            MealPlanDayDTO dayDto = new MealPlanDayDTO();
            dayDto.setDayOfWeek(day.getDayOfWeek());
            List<MealPlanMealDTO> meals = day.getMeals().stream().map(meal -> {
                MealPlanMealDTO mealDto = new MealPlanMealDTO();
                mealDto.setMealType(meal.getMealType());
                mealDto.setTitle(meal.getTitle() == null || meal.getTitle().isBlank() ? defaultMealTitle(meal.getMealType()) : meal.getTitle());
                mealDto.setTime(meal.getTime());
                return mealDto;
            }).collect(Collectors.toList());
            dayDto.setMeals(meals);
            return dayDto;
        }).collect(Collectors.toList());
        dto.setDays(days);
        return dto;
    }

    private String defaultMealTitle(String mealType) {
        return switch (mealType) {
            case "Breakfast" -> "Səhər yeməyi";
            case "Lunch" -> "Nahar";
            case "Dinner" -> "Şam yeməyi";
            default -> "Yemək";
        };
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
        String prompt = buildAiPrompt(user, weekStart);
        String content = callGroq(prompt);
        if (content == null || content.isBlank()) {
            return;
        }
        List<MealPlanDayDTO> days = parseAiDays(content);
        if (!isValidAiDays(days)) {
            return;
        }
        MealPlan plan = mealPlanRepository.findByUserIdAndWeekStartAndSource(user.getId(), weekStart, "ai")
                .orElseGet(() -> createDefaultPlan(user, weekStart, "ai"));
        plan.setSource("ai");
        plan.setCreatedAt(Instant.now());
        plan.getDays().clear();
        plan.getDays().addAll(mapDays(plan, days));
        mealPlanRepository.save(plan);
        List<ShoppingCategoryPayload> categories = parseAiShoppingList(content);
        if (categories != null && !categories.isEmpty()) {
            updateShoppingListFromCategories(user, weekStart, categories);
            return;
        }
        List<Ingredient> ingredients = parseAiIngredients(content);
        if (ingredients != null && !ingredients.isEmpty()) {
            updateShoppingListFromIngredients(user, weekStart, ingredients);
        }
    }

    private String buildAiPrompt(User user, LocalDate weekStart) {
        Integer age = user.getDateOfBirth() == null ? null : Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
        String gender = user.getGender();
        Double height = user.getHeight();
        Double weight = user.getWeight();
        String condition = user.getHealthCondition() != null ? user.getHealthCondition().getName() : null;
        String category = user.getHealthCondition() != null && user.getHealthCondition().getCategory() != null
                ? user.getHealthCondition().getCategory().getName() : null;
        String severity = user.getSeverity();
        StringBuilder builder = new StringBuilder();
        builder.append("Return ONLY raw JSON (no markdown, no code fences). ");
        builder.append("All human-readable text must be in Azerbaijani (meal titles, shopping list item names, quantities). ");
        builder.append("Use Azerbaijani characters (ə, ğ, ı, ö, ü, ç, ş). ");
        builder.append("Return EXACTLY 7 days using dayOfWeek: Mon,Tue,Wed,Thu,Fri,Sat,Sun. ");
        builder.append("Each day must have EXACTLY 3 meals: Breakfast,Lunch,Dinner. ");
        builder.append("Each meal MUST include: mealType, title (non-empty), time, ingredients. ");
        builder.append("Titles must be real dish names, not generic like 'Səhər yeməyi', 'Nahar', 'Şam'. ");
        builder.append("Ingredients MUST be an array of {name, quantity, category}. ");
        builder.append("Also return a day-specific shoppingListDaily for each day (Mon..Sun) based on ingredients. ");
        builder.append("Categories must be one of: Taxıllar, Meyvələr, Tərəvəzlər, Süd məhsulları, Ət/Balıq, Qoz-fındıq, İçkilər, Şirniyyatlar, Digər. ");
        builder.append("Keep everything short and compact. ");
        builder.append("Format: {\"days\":[{\"dayOfWeek\":\"Mon\",\"meals\":[{\"mealType\":\"Breakfast\",\"title\":\"...\",\"time\":\"07:00 AM\",\"ingredients\":[{\"name\":\"...\",\"quantity\":\"...\",\"category\":\"...\"}]}]}],")
                .append("\"shoppingListDaily\":[{\"dayOfWeek\":\"Mon\",\"categories\":[{\"name\":\"Taxıllar\",\"items\":[{\"name\":\"...\",\"quantity\":\"...\"}]}]}]}. ");
        builder.append("Week start: ").append(weekStart).append(". ");
        if (age != null) {
            builder.append("Age: ").append(age).append(". ");
        }
        if (gender != null && !gender.isBlank()) {
            builder.append("Gender: ").append(gender).append(". ");
        }
        if (height != null) {
            builder.append("HeightCm: ").append(height).append(". ");
        }
        if (weight != null) {
            builder.append("WeightKg: ").append(weight).append(". ");
        }
        if (condition != null && !condition.isBlank()) {
            builder.append("HealthCondition: ").append(condition).append(". ");
        }
        if (category != null && !category.isBlank()) {
            builder.append("ConditionCategory: ").append(category).append(". ");
        }
        if (severity != null && !severity.isBlank()) {
            builder.append("Severity: ").append(severity).append(". ");
        }
        String avoidList = buildAvoidList(condition, category, severity);
        if (!avoidList.isBlank()) {
            builder.append("Avoid or strictly limit these foods: ").append(avoidList).append(". ");
        }
        builder.append("Meals must be safe for the condition and avoid harmful foods. ");
        return builder.toString();
    }

    private String buildAvoidList(String condition, String category, String severity) {
        String base = (condition == null ? "" : condition) + " " + (category == null ? "" : category);
        String normalized = base.toLowerCase(Locale.ROOT);
        List<String> avoids = new ArrayList<>();
        if (normalized.contains("diabet")) {
            avoids.add("şəkərli içkilər");
            avoids.add("şirniyyatlar");
            avoids.add("ağ un və rafinə taxıllar");
            avoids.add("şirin desertlər");
        }
        if (normalized.contains("hipertoni") || normalized.contains("təzyiq") || normalized.contains("pressure")) {
            avoids.add("həddindən artıq duz");
            avoids.add("duzlu konservlər");
            avoids.add("kolbasa və emal olunmuş ətlər");
            avoids.add("çips və hazır qəlyanaltılar");
        }
        if (normalized.contains("kidney") || normalized.contains("böyrək") || normalized.contains("renal")) {
            avoids.add("çox duzlu qidalar");
            avoids.add("emal olunmuş ətlər");
            avoids.add("qazlı içkilər");
        }
        if (normalized.contains("cholesterol") || normalized.contains("lipid")) {
            avoids.add("qızartmalar");
            avoids.add("trans yağlar");
            avoids.add("yağlı fast-food");
        }
        if (normalized.contains("celiac") || normalized.contains("gluten")) {
            avoids.add("buğda");
            avoids.add("arpa");
            avoids.add("çovdar");
        }
        if (normalized.contains("gout") || normalized.contains("uric")) {
            avoids.add("sakatatlar");
            avoids.add("qırmızı ət çox");
            avoids.add("sardina və bəzi yağlı balıqlar");
        }
        if (normalized.contains("gastrit") || normalized.contains("ulcer") || normalized.contains("mədə")) {
            avoids.add("acılı qidalar");
            avoids.add("qızartmalar");
            avoids.add("qəhvə çox");
            avoids.add("alkoqol");
        }
        if (normalized.contains("liver") || normalized.contains("qaraciyər")) {
            avoids.add("yağlı qidalar");
            avoids.add("alkoqol");
            avoids.add("qızartmalar");
        }
        if (normalized.contains("obes") || normalized.contains("çəki") || normalized.contains("weight")) {
            avoids.add("şəkərli içkilər");
            avoids.add("fast-food");
            avoids.add("şirniyyatlar");
        }
        if (severity != null && !severity.isBlank() && severity.toLowerCase(Locale.ROOT).contains("severe")) {
            avoids.add("şəkər və duz yüksək qidalar");
        }
        return String.join(", ", avoids);
    }

    private List<ShoppingCategoryPayload> parseAiShoppingList(String content) {
        try {
            String json = extractJson(content);
            JsonNode root = objectMapper.readTree(json);
            List<ShoppingCategoryPayload> categories = new ArrayList<>();
            JsonNode dailyNode = root.get("shoppingListDaily");
            if (dailyNode != null && dailyNode.isArray()) {
                for (JsonNode dayNode : dailyNode) {
                    String day = normalizeDay(dayNode.path("dayOfWeek").asText(null));
                    JsonNode catsNode = dayNode.path("categories");
                    if (day == null || catsNode == null || !catsNode.isArray()) {
                        continue;
                    }
                    for (JsonNode catNode : catsNode) {
                        String name = catNode.path("name").asText(null);
                        String normalized = normalizeCategory(name);
                        JsonNode itemsNode = catNode.path("items");
                        if (itemsNode == null || !itemsNode.isArray()) {
                            continue;
                        }
                        List<ShoppingItemPayload> items = new ArrayList<>();
                        for (JsonNode itemNode : itemsNode) {
                            String itemName = itemNode.path("name").asText(null);
                            String quantity = itemNode.path("quantity").asText(null);
                            if (itemName == null || itemName.isBlank()) {
                                continue;
                            }
                            items.add(new ShoppingItemPayload(day, itemName.trim(), quantity == null ? "" : quantity.trim()));
                        }
                        if (!items.isEmpty()) {
                            categories.add(new ShoppingCategoryPayload(normalized, items));
                        }
                    }
                }
                return categories;
            }
            JsonNode categoriesNode = root.path("shoppingList").path("categories");
            if (categoriesNode != null && categoriesNode.isArray()) {
                for (JsonNode catNode : categoriesNode) {
                    String name = catNode.path("name").asText(null);
                    String normalized = normalizeCategory(name);
                    JsonNode itemsNode = catNode.path("items");
                    if (itemsNode == null || !itemsNode.isArray()) {
                        continue;
                    }
                    List<ShoppingItemPayload> items = new ArrayList<>();
                    for (JsonNode itemNode : itemsNode) {
                        String itemName = itemNode.path("name").asText(null);
                        String quantity = itemNode.path("quantity").asText(null);
                        if (itemName == null || itemName.isBlank()) {
                            continue;
                        }
                        items.add(new ShoppingItemPayload(null, itemName.trim(), quantity == null ? "" : quantity.trim()));
                    }
                    if (!items.isEmpty()) {
                        categories.add(new ShoppingCategoryPayload(normalized, items));
                    }
                }
            }
            return categories;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<Ingredient> parseAiIngredients(String content) {
        try {
            String json = extractJson(content);
            JsonNode root = objectMapper.readTree(json);
            JsonNode daysNode = root.get("days");
            if (daysNode == null || !daysNode.isArray()) {
                return List.of();
            }
            List<Ingredient> result = new ArrayList<>();
            for (JsonNode dayNode : daysNode) {
                String day = normalizeDay(dayNode.path("dayOfWeek").asText(null));
                JsonNode mealsNode = dayNode.get("meals");
                if (mealsNode != null && mealsNode.isArray()) {
                    for (JsonNode mealNode : mealsNode) {
                        JsonNode ingNode = mealNode.get("ingredients");
                        if (ingNode != null && ingNode.isArray()) {
                            for (JsonNode i : ingNode) {
                                String name = i.path("name").asText(null);
                                String quantity = i.path("quantity").asText(null);
                                String category = i.path("category").asText(null);
                                if (name != null && !name.isBlank()) {
                                    result.add(new Ingredient(day, name.trim(),
                                            quantity == null ? "" : quantity.trim(),
                                            normalizeCategory(category)));
                                }
                            }
                        }
                    }
                }
            }
            return result;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String normalizeCategory(String category) {
        if (category == null) return "Digər";
        String v = category.trim().toLowerCase();
        if (v.contains("grain") || v.contains("grains") || v.contains("cereal") || v.contains("bread")) return "Taxıllar";
        if (v.contains("fruit") || v.contains("fruits") || v.contains("berry")) return "Meyvələr";
        if (v.contains("vegetable") || v.contains("vegetables") || v.contains("greens")) return "Tərəvəzlər";
        if (v.contains("dairy") || v.contains("milk") || v.contains("yogurt") || v.contains("cheese")) return "Süd məhsulları";
        if (v.contains("protein") || v.contains("proteins") || v.contains("meat") || v.contains("fish") || v.contains("chicken") || v.contains("beef") || v.contains("turkey") || v.contains("salmon") || v.contains("egg")) return "Ət/Balıq";
        if (v.contains("nut") || v.contains("nuts") || v.contains("seed") || v.contains("seeds")) return "Qoz-fındıq";
        if (v.contains("drink") || v.contains("beverage") || v.contains("water") || v.contains("tea") || v.contains("coffee")) return "İçkilər";
        if (v.contains("sweet") || v.contains("sweetener") || v.contains("sweeteners") || v.contains("dessert") || v.contains("sugar") || v.contains("honey")) return "Şirniyyatlar";
        if (v.contains("tax")) return "Taxıllar";
        if (v.contains("meyv")) return "Meyvələr";
        if (v.contains("tərəv") || v.contains("terevez")) return "Tərəvəzlər";
        if (v.contains("süd")) return "Süd məhsulları";
        if (v.contains("ət") || v.contains("balıq")) return "Ət/Balıq";
        if (v.contains("qoz") || v.contains("fındıq")) return "Qoz-fındıq";
        if (v.contains("içki")) return "İçkilər";
        if (v.contains("şirn")) return "Şirniyyatlar";
        return "Digər";
    }

    private void updateShoppingListFromIngredients(User user, LocalDate weekStart, List<Ingredient> ingredients) {
        ShoppingList list = shoppingListRepository.findByUserIdAndWeekStart(user.getId(), weekStart)
                .orElseGet(() -> {
                    ShoppingList l = new ShoppingList();
                    l.setUser(user);
                    l.setWeekStart(weekStart);
                    l.setCreatedAt(Instant.now());
                    return l;
                });
        list.getCategories().clear();
        Map<String, Map<String, Map<String, String>>> agg = new java.util.LinkedHashMap<>();
        for (Ingredient ing : ingredients) {
            String day = normalizeDay(ing.dayOfWeek);
            agg.computeIfAbsent(ing.category, k -> new java.util.LinkedHashMap<>());
            Map<String, Map<String, String>> byDay = agg.get(ing.category);
            byDay.computeIfAbsent(day, k -> new java.util.LinkedHashMap<>());
            Map<String, String> items = byDay.get(day);
            if (items.containsKey(ing.name) && !ing.quantity.isBlank()) {
                String prev = items.get(ing.name);
                items.put(ing.name, prev.isBlank() ? ing.quantity : prev + " + " + ing.quantity);
            } else {
                items.putIfAbsent(ing.name, ing.quantity);
            }
        }
        for (Map.Entry<String, Map<String, Map<String, String>>> cat : agg.entrySet()) {
            ShoppingCategory sc = new ShoppingCategory();
            sc.setShoppingList(list);
            sc.setName(cat.getKey());
            for (Map.Entry<String, Map<String, String>> dayEntry : cat.getValue().entrySet()) {
                for (Map.Entry<String, String> item : dayEntry.getValue().entrySet()) {
                    if (item.getKey() == null || item.getKey().isBlank()) {
                        continue;
                    }
                    ShoppingItem si = new ShoppingItem();
                    si.setCategory(sc);
                    si.setName(item.getKey());
                    si.setQuantity(item.getValue() == null ? "" : item.getValue());
                    si.setDayOfWeek(dayEntry.getKey());
                    si.setChecked(false);
                    sc.getItems().add(si);
                }
            }
            list.getCategories().add(sc);
        }
        shoppingListRepository.save(list);
    }

    private void updateShoppingListFromCategories(User user, LocalDate weekStart, List<ShoppingCategoryPayload> categories) {
        ShoppingList list = shoppingListRepository.findByUserIdAndWeekStart(user.getId(), weekStart)
                .orElseGet(() -> {
                    ShoppingList l = new ShoppingList();
                    l.setUser(user);
                    l.setWeekStart(weekStart);
                    l.setCreatedAt(Instant.now());
                    return l;
                });
        list.getCategories().clear();
        for (ShoppingCategoryPayload cat : categories) {
            ShoppingCategory sc = new ShoppingCategory();
            sc.setShoppingList(list);
            sc.setName(normalizeCategory(cat.name));
            for (ShoppingItemPayload item : cat.items) {
                if (item.name == null || item.name.isBlank()) {
                    continue;
                }
                ShoppingItem si = new ShoppingItem();
                si.setCategory(sc);
                si.setName(item.name.trim());
                si.setQuantity(item.quantity == null ? "" : item.quantity.trim());
                si.setDayOfWeek(item.dayOfWeek);
                si.setChecked(false);
                sc.getItems().add(si);
            }
            if (!sc.getItems().isEmpty()) {
                list.getCategories().add(sc);
            }
        }
        shoppingListRepository.save(list);
    }

    private static class Ingredient {
        final String dayOfWeek;
        final String name;
        final String quantity;
        final String category;
        Ingredient(String dayOfWeek, String name, String quantity, String category) {
            this.dayOfWeek = dayOfWeek;
            this.name = name;
            this.quantity = quantity;
            this.category = category;
        }
    }

    private static class ShoppingCategoryPayload {
        final String name;
        final List<ShoppingItemPayload> items;
        ShoppingCategoryPayload(String name, List<ShoppingItemPayload> items) {
            this.name = name;
            this.items = items;
        }
    }

    private static class ShoppingItemPayload {
        final String dayOfWeek;
        final String name;
        final String quantity;
        ShoppingItemPayload(String dayOfWeek, String name, String quantity) {
            this.dayOfWeek = dayOfWeek;
            this.name = name;
            this.quantity = quantity;
        }
    }
    private String callGroq(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", aiModel,
                    "temperature", 0.4,
                    "max_tokens", aiMaxTokens,
                    "messages", List.of(
                            Map.of("role", "system", "content", "Return only raw JSON. No markdown. Use Azerbaijani for human-readable text and Azerbaijani characters (ə, ğ, ı, ö, ü, ç, ş)."),
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

    private List<MealPlanDayDTO> parseAiDays(String content) {
        try {
            String json = extractJson(content);
            JsonNode root = objectMapper.readTree(json);
            JsonNode daysNode = root.get("days");
            if (daysNode == null || !daysNode.isArray()) {
                return null;
            }
            List<MealPlanDayDTO> days = new ArrayList<>();
            for (JsonNode dayNode : daysNode) {
                String dayOfWeek = normalizeDay(dayNode.path("dayOfWeek").asText(null));
                if (dayOfWeek == null) {
                    continue;
                }
                List<MealPlanMealDTO> meals = new ArrayList<>();
                JsonNode mealsNode = dayNode.get("meals");
                if (mealsNode != null && mealsNode.isArray()) {
                    for (JsonNode mealNode : mealsNode) {
                        String mealType = normalizeMealType(mealNode.path("mealType").asText(null));
                        String title = mealNode.path("title").asText(null);
                        String time = mealNode.path("time").asText(null);
                        if (mealType == null || title == null || title.isBlank() || isGenericMealTitle(title)) {
                            continue;
                        }
                        MealPlanMealDTO meal = new MealPlanMealDTO();
                        meal.setMealType(mealType);
                        meal.setTitle(title.trim());
                        meal.setTime(time == null || time.isBlank() ? defaultTime(mealType) : time);
                        meals.add(meal);
                    }
                }
                MealPlanDayDTO day = new MealPlanDayDTO();
                day.setDayOfWeek(dayOfWeek);
                day.setMeals(meals);
                days.add(day);
            }
            return days;
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isValidAiDays(List<MealPlanDayDTO> days) {
        if (days == null || days.size() != 7) {
            return false;
        }
        Set<String> daySet = new HashSet<>();
        for (MealPlanDayDTO day : days) {
            if (day.getDayOfWeek() == null || day.getMeals() == null || day.getMeals().size() < 3) {
                return false;
            }
            for (MealPlanMealDTO meal : day.getMeals()) {
                if (meal.getTitle() == null || meal.getTitle().isBlank() || isGenericMealTitle(meal.getTitle())) {
                    return false;
                }
            }
            daySet.add(day.getDayOfWeek());
        }
        return daySet.size() == 7;
    }

    private boolean isGenericMealTitle(String title) {
        if (title == null) {
            return true;
        }
        String value = title.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return true;
        }
        return value.equals("səhər") || value.equals("seher")
                || value.equals("nahar") || value.equals("günorta") || value.equals("gunorta")
                || value.equals("şam") || value.equals("sam")
                || value.equals("yemək") || value.equals("yemek")
                || value.equals("səhər yeməyi") || value.equals("seher yemeyi")
                || value.equals("şam yeməyi") || value.equals("sam yemeyi");
    }

    private boolean isLikelyAzerbaijaniPlan(MealPlan plan) {
        if (plan == null || plan.getDays() == null) {
            return false;
        }
        for (MealPlanDay day : plan.getDays()) {
            if (day.getMeals() == null) {
                continue;
            }
            for (MealPlanMeal meal : day.getMeals()) {
                if (isLikelyAzerbaijaniText(meal.getTitle())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isLikelyAzerbaijaniText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String value = text.toLowerCase(Locale.ROOT);
        if (AZ_CHAR_PATTERN.matcher(value).find()) {
            return true;
        }
        for (String hint : AZ_HINTS) {
            if (value.contains(hint)) {
                return true;
            }
        }
        return false;
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

    private String normalizeMealType(String mealType) {
        if (mealType == null) {
            return null;
        }
        String value = mealType.trim().toLowerCase();
        if (value.contains("breakfast")) {
            return "Breakfast";
        }
        if (value.contains("lunch")) {
            return "Lunch";
        }
        if (value.contains("dinner")) {
            return "Dinner";
        }
        return null;
    }

    private String defaultTime(String mealType) {
        return switch (mealType) {
            case "Breakfast" -> "07:00 AM";
            case "Lunch" -> "12:00 PM";
            default -> "06:00 PM";
        };
    }

    private boolean isAiFresh(Instant createdAt) {
        if (createdAt == null || aiCacheTtlHours <= 0) {
            return false;
        }
        return createdAt.isAfter(Instant.now().minus(Duration.ofHours(aiCacheTtlHours)));
    }

    @PreDestroy
    public void shutdownAiExecutor() {
        aiExecutor.shutdown();
    }
}
