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
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.service.MealPlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MealPlanServiceImpl implements MealPlanService {
    private final MealPlanRepository mealPlanRepository;
    private final UserRepository userRepository;

    public MealPlanServiceImpl(MealPlanRepository mealPlanRepository, UserRepository userRepository) {
        this.mealPlanRepository = mealPlanRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public MealPlanResponseDTO getCurrentPlan(String userEmail) {
        User user = getUser(userEmail);
        LocalDate weekStart = currentWeekStart();
        MealPlan plan = mealPlanRepository.findByUserIdAndWeekStart(user.getId(), weekStart)
                .orElseGet(() -> mealPlanRepository.save(createDefaultPlan(user, weekStart, "default")));
        return mapToDTO(plan);
    }

    @Override
    @Transactional
    public MealPlanResponseDTO getPlanByWeekStart(String userEmail, LocalDate weekStart) {
        User user = getUser(userEmail);
        MealPlan plan = mealPlanRepository.findByUserIdAndWeekStart(user.getId(), weekStart)
                .orElseGet(() -> mealPlanRepository.save(createDefaultPlan(user, weekStart, "default")));
        return mapToDTO(plan);
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
        MealPlan plan = mealPlanRepository.findByUserIdAndWeekStart(user.getId(), weekStart)
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
}
