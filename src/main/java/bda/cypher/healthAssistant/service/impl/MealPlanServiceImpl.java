package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.MealPlanDayDTO;
import bda.cypher.healthAssistant.dto.MealPlanGenerateRequestDTO;
import bda.cypher.healthAssistant.dto.MealPlanMealDTO;
import bda.cypher.healthAssistant.dto.MealPlanResponseDTO;
import bda.cypher.healthAssistant.dto.MealPlanUpdateRequestDTO;
import bda.cypher.healthAssistant.entity.MealPlan;
import bda.cypher.healthAssistant.entity.MealPlanDay;
import bda.cypher.healthAssistant.entity.MealPlanMeal;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.repository.MealPlanRepository;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class MealPlanServiceImpl implements MealPlanService {
    private final MealPlanRepository mealPlanRepository;
    private final UserRepository userRepository;
    private final ShoppingListRepository shoppingListRepository;
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
        User user = getUser(userEmail);
        LocalDate targetWeek = weekStart != null ? weekStart : currentWeekStart();
        MealPlan aiPlan = mealPlanRepository.findByUserIdAndWeekStartAndSource(user.getId(), targetWeek, "ai")
                .filter(plan -> isAiFresh(plan.getCreatedAt()))
                .orElse(null);
        if (aiPlan != null) {
            return mapToDTO(aiPlan);
        }
        triggerAiGeneration(user, targetWeek);
        MealPlan corePlan = getOrCreateCorePlan(user, targetWeek);
        return mapToDTO(corePlan);
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
                        .append(meal.getTitle());
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
        day.getMeals().add(createMeal(day, "Breakfast", "Seçilməyib", "07:00 AM"));
        day.getMeals().add(createMeal(day, "Lunch", "Seçilməyib", "12:00 PM"));
        day.getMeals().add(createMeal(day, "Dinner", "Seçilməyib", "06:00 PM"));
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
                mealDto.setTitle(meal.getTitle());
                mealDto.setTime(meal.getTime());
                return mealDto;
            }).collect(Collectors.toList());
            dayDto.setMeals(meals);
            return dayDto;
        }).collect(Collectors.toList());
        dto.setDays(days);
        return dto;
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
        builder.append("Return EXACTLY 7 days using dayOfWeek: Mon,Tue,Wed,Thu,Fri,Sat,Sun. ");
        builder.append("Each day must have EXACTLY 3 meals: Breakfast,Lunch,Dinner. ");
        builder.append("Each meal MUST include: mealType, title (non-empty), time. ");
        builder.append("Also return a weekly shoppingList with categories and items (name, quantity). ");
        builder.append("Categories must be one of: Taxıllar, Meyvələr, Tərəvəzlər, Süd məhsulları, Ət/Balıq, Qoz-fındıq, İçkilər, Şirniyyatlar, Digər. ");
        builder.append("Keep everything short and compact. ");
        builder.append("Format: {\"days\":[{\"dayOfWeek\":\"Mon\",\"meals\":[{\"mealType\":\"Breakfast\",\"title\":\"...\",\"time\":\"07:00 AM\"}]}],")
                .append("\"shoppingList\":{\"categories\":[{\"name\":\"Taxıllar\",\"items\":[{\"name\":\"...\",\"quantity\":\"...\"}]}]}}. ");
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
        return builder.toString();
    }

    private List<ShoppingCategoryPayload> parseAiShoppingList(String content) {
        try {
            String json = extractJson(content);
            JsonNode root = objectMapper.readTree(json);
            JsonNode categoriesNode = root.path("shoppingList").path("categories");
            if (categoriesNode == null || !categoriesNode.isArray()) {
                return List.of();
            }
            List<ShoppingCategoryPayload> categories = new ArrayList<>();
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
                    items.add(new ShoppingItemPayload(itemName.trim(), quantity == null ? "" : quantity.trim()));
                }
                if (!items.isEmpty()) {
                    categories.add(new ShoppingCategoryPayload(normalized, items));
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
                                    result.add(new Ingredient(name.trim(),
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
        if (v.contains("tax")) return "Taxıllar";
        if (v.contains("meyv")) return "Meyvələr";
        if (v.contains("tərəv") || v.contains("terevez")) return "Tərəvəzlər";
        if (v.contains("süd") || v.contains("milk") || v.contains("dairy")) return "Süd məhsulları";
        if (v.contains("ət") || v.contains("balıq") || v.contains("meat") || v.contains("fish")) return "Ət/Balıq";
        if (v.contains("qoz") || v.contains("fındıq") || v.contains("nut")) return "Qoz-fındıq";
        if (v.contains("içki") || v.contains("drink")) return "İçkilər";
        if (v.contains("şirn") || v.contains("sweet") || v.contains("dessert")) return "Şirniyyatlar";
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
        Map<String, Map<String, String>> agg = new java.util.LinkedHashMap<>();
        for (Ingredient ing : ingredients) {
            agg.computeIfAbsent(ing.category, k -> new java.util.LinkedHashMap<>());
            Map<String, String> items = agg.get(ing.category);
            if (items.containsKey(ing.name) && !ing.quantity.isBlank()) {
                String prev = items.get(ing.name);
                items.put(ing.name, prev.isBlank() ? ing.quantity : prev + " + " + ing.quantity);
            } else {
                items.putIfAbsent(ing.name, ing.quantity);
            }
        }
        for (Map.Entry<String, Map<String, String>> cat : agg.entrySet()) {
            ShoppingCategory sc = new ShoppingCategory();
            sc.setShoppingList(list);
            sc.setName(cat.getKey());
            for (Map.Entry<String, String> item : cat.getValue().entrySet()) {
                ShoppingItem si = new ShoppingItem();
                si.setCategory(sc);
                si.setName(item.getKey());
                si.setQuantity(item.getValue());
                si.setChecked(false);
                sc.getItems().add(si);
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
        final String name;
        final String quantity;
        final String category;
        Ingredient(String name, String quantity, String category) {
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
        final String name;
        final String quantity;
        ShoppingItemPayload(String name, String quantity) {
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
                            Map.of("role", "system", "content", "Return only JSON without markdown."),
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
                        if (mealType == null || title == null || title.isBlank()) {
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
            daySet.add(day.getDayOfWeek());
        }
        return daySet.size() == 7;
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
