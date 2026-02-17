package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.ShoppingCategoryDTO;
import bda.cypher.healthAssistant.dto.ShoppingItemDTO;
import bda.cypher.healthAssistant.dto.ShoppingItemUpdateDTO;
import bda.cypher.healthAssistant.dto.ShoppingListResponseDTO;
import bda.cypher.healthAssistant.dto.ShoppingListUpdateRequestDTO;
import bda.cypher.healthAssistant.entity.ShoppingCategory;
import bda.cypher.healthAssistant.entity.ShoppingItem;
import bda.cypher.healthAssistant.entity.ShoppingList;
import bda.cypher.healthAssistant.entity.User;
import bda.cypher.healthAssistant.repository.ShoppingListRepository;
import bda.cypher.healthAssistant.repository.UserRepository;
import bda.cypher.healthAssistant.service.ShoppingListService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShoppingListServiceImpl implements ShoppingListService {
    private final ShoppingListRepository shoppingListRepository;
    private final UserRepository userRepository;

    public ShoppingListServiceImpl(ShoppingListRepository shoppingListRepository, UserRepository userRepository) {
        this.shoppingListRepository = shoppingListRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ShoppingListResponseDTO getCurrentList(String userEmail) {
        User user = getUser(userEmail);
        LocalDate weekStart = currentWeekStart();
        ShoppingList list = shoppingListRepository.findByUserIdAndWeekStart(user.getId(), weekStart)
                .orElseGet(() -> shoppingListRepository.save(createDefaultList(user, weekStart)));
        return mapToDTO(list);
    }

    @Override
    @Transactional
    public ShoppingListResponseDTO getListByWeekStart(String userEmail, LocalDate weekStart) {
        User user = getUser(userEmail);
        ShoppingList list = shoppingListRepository.findByUserIdAndWeekStart(user.getId(), weekStart)
                .orElseGet(() -> shoppingListRepository.save(createDefaultList(user, weekStart)));
        return mapToDTO(list);
    }

    @Override
    @Transactional
    public ShoppingListResponseDTO updateItems(String userEmail, Long id, ShoppingListUpdateRequestDTO request) {
        User user = getUser(userEmail);
        ShoppingList list = shoppingListRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Siyahı tapılmadı"));
        if (request.getItems() != null) {
            for (ShoppingItemUpdateDTO update : request.getItems()) {
                ShoppingItem item = findItem(list, update.getId())
                        .orElseThrow(() -> new RuntimeException("Məhsul tapılmadı"));
                if (update.getChecked() != null) {
                    item.setChecked(update.getChecked());
                }
                if (update.getQuantity() != null) {
                    item.setQuantity(update.getQuantity());
                }
            }
        }
        ShoppingList saved = shoppingListRepository.save(list);
        return mapToDTO(saved);
    }

    @Override
    public byte[] exportList(String userEmail, Long id) {
        User user = getUser(userEmail);
        ShoppingList list = shoppingListRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Siyahı tapılmadı"));
        StringBuilder builder = new StringBuilder();
        builder.append("WeekStart: ").append(list.getWeekStart()).append("\n");
        for (ShoppingCategory category : list.getCategories()) {
            builder.append(category.getName()).append("\n");
            for (ShoppingItem item : category.getItems()) {
                builder.append(" - ").append(item.getName());
                if (item.getQuantity() != null && !item.getQuantity().isBlank()) {
                    builder.append(" | ").append(item.getQuantity());
                }
                builder.append(" | ").append(item.isChecked() ? "checked" : "unchecked");
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

    private ShoppingList createDefaultList(User user, LocalDate weekStart) {
        ShoppingList list = new ShoppingList();
        list.setUser(user);
        list.setWeekStart(weekStart);
        list.setCreatedAt(Instant.now());
        list.getCategories().add(createCategory(list, "Taxıllar"));
        list.getCategories().add(createCategory(list, "Meyvələr"));
        list.getCategories().add(createCategory(list, "Qoz-fındıq"));
        list.getCategories().add(createCategory(list, "Şirniyyatlar"));
        return list;
    }

    private ShoppingCategory createCategory(ShoppingList list, String name) {
        ShoppingCategory category = new ShoppingCategory();
        category.setShoppingList(list);
        category.setName(name);
        return category;
    }

    private Optional<ShoppingItem> findItem(ShoppingList list, Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return list.getCategories().stream()
                .flatMap(category -> category.getItems().stream())
                .filter(item -> id.equals(item.getId()))
                .findFirst();
    }

    private ShoppingListResponseDTO mapToDTO(ShoppingList list) {
        ShoppingListResponseDTO dto = new ShoppingListResponseDTO();
        dto.setId(list.getId());
        dto.setWeekStart(list.getWeekStart());
        List<ShoppingCategoryDTO> categories = list.getCategories().stream().map(category -> {
            ShoppingCategoryDTO categoryDTO = new ShoppingCategoryDTO();
            categoryDTO.setName(category.getName());
            List<ShoppingItemDTO> items = category.getItems().stream().map(item -> {
                ShoppingItemDTO itemDTO = new ShoppingItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setName(item.getName());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setChecked(item.isChecked());
                return itemDTO;
            }).collect(Collectors.toList());
            categoryDTO.setItems(items);
            return categoryDTO;
        }).collect(Collectors.toList());
        dto.setCategories(categories);
        return dto;
    }
}
