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
import bda.cypher.healthAssistant.service.MealPlanService;
import bda.cypher.healthAssistant.service.ShoppingListService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
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
    private final MealPlanService mealPlanService;

    public ShoppingListServiceImpl(ShoppingListRepository shoppingListRepository, UserRepository userRepository, MealPlanService mealPlanService) {
        this.shoppingListRepository = shoppingListRepository;
        this.userRepository = userRepository;
        this.mealPlanService = mealPlanService;
    }

    @Override
    @Transactional
    public ShoppingListResponseDTO getCurrentList(String userEmail) {
        User user = getUser(userEmail);
        LocalDate weekStart = currentWeekStart();
        ShoppingList list = shoppingListRepository.findByUserIdAndWeekStart(user.getId(), weekStart)
                .orElseGet(() -> shoppingListRepository.save(createDefaultList(user, weekStart)));
        ensureAiShoppingList(userEmail, weekStart, list);
        return mapToDTO(list, null);
    }

    @Override
    @Transactional
    public ShoppingListResponseDTO getListByWeekStart(String userEmail, LocalDate weekStart) {
        return getListByWeekStart(userEmail, weekStart, null);
    }

    @Override
    @Transactional
    public ShoppingListResponseDTO getListByWeekStart(String userEmail, LocalDate weekStart, String dayOfWeek) {
        User user = getUser(userEmail);
        ShoppingList list = shoppingListRepository.findByUserIdAndWeekStart(user.getId(), weekStart)
                .orElseGet(() -> shoppingListRepository.save(createDefaultList(user, weekStart)));
        ensureAiShoppingList(userEmail, weekStart, list);
        return mapToDTO(list, normalizeDay(dayOfWeek));
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
        return mapToDTO(saved, null);
    }

    @Override
    public byte[] exportList(String userEmail, Long id) {
        return exportList(userEmail, id, null);
    }

    @Override
    public byte[] exportList(String userEmail, Long id, String format) {
        User user = getUser(userEmail);
        ShoppingList list = shoppingListRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Siyahı tapılmadı"));
        String text = buildExportText(list);
        if (format != null && format.equalsIgnoreCase("pdf")) {
            return buildPdf(text);
        }
        return text.getBytes(StandardCharsets.UTF_8);
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

    private String buildExportText(ShoppingList list) {
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
        return builder.toString();
    }

    private byte[] buildPdf(String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").replace("\r", "\n");
        String[] rawLines = normalized.split("\n", -1);
        List<String> lines = new ArrayList<>();
        for (String line : rawLines) {
            lines.add(line);
        }
        StringBuilder content = new StringBuilder();
        content.append("BT\r\n/F1 12 Tf\r\n14 TL\r\n50 780 Td\r\n");
        for (int i = 0; i < lines.size(); i++) {
            String line = escapePdf(lines.get(i));
            content.append("(").append(line).append(") Tj\r\n");
            if (i < lines.size() - 1) {
                content.append("T*\r\n");
            }
        }
        content.append("ET");
        byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

        List<Integer> offsets = new ArrayList<>();
        String header = "%PDF-1.4\r\n%âãÏÓ\r\n";
        String obj1 = "1 0 obj\r\n<< /Type /Catalog /Pages 2 0 R >>\r\nendobj\r\n";
        String obj2 = "2 0 obj\r\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\r\nendobj\r\n";
        String obj3 = "3 0 obj\r\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\r\nendobj\r\n";
        String obj4Start = "4 0 obj\r\n<< /Length " + contentBytes.length + " >>\r\nstream\r\n";
        String obj4End = "\r\nendstream\r\nendobj\r\n";
        String obj5 = "5 0 obj\r\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\r\nendobj\r\n";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(header.getBytes(StandardCharsets.ISO_8859_1));
        offsets.add(0);
        offsets.add(out.size());
        out.writeBytes(obj1.getBytes(StandardCharsets.ISO_8859_1));
        offsets.add(out.size());
        out.writeBytes(obj2.getBytes(StandardCharsets.ISO_8859_1));
        offsets.add(out.size());
        out.writeBytes(obj3.getBytes(StandardCharsets.ISO_8859_1));
        offsets.add(out.size());
        out.writeBytes(obj4Start.getBytes(StandardCharsets.ISO_8859_1));
        out.writeBytes(contentBytes);
        out.writeBytes(obj4End.getBytes(StandardCharsets.ISO_8859_1));
        offsets.add(out.size());
        out.writeBytes(obj5.getBytes(StandardCharsets.ISO_8859_1));

        int xrefStart = out.size();
        StringBuilder xref = new StringBuilder();
        xref.append("xref\r\n0 6\r\n");
        xref.append(String.format("%010d 65535 f \r\n", 0));
        for (int i = 1; i <= 5; i++) {
            xref.append(String.format("%010d 00000 n \r\n", offsets.get(i)));
        }
        xref.append("trailer\r\n<< /Size 6 /Root 1 0 R >>\r\nstartxref\r\n");
        xref.append(xrefStart).append("\r\n%%EOF");
        out.writeBytes(xref.toString().getBytes(StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }

    private String escapePdf(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '(' || ch == ')' || ch == '\\') {
                builder.append('\\');
            }
            if (ch <= 0xFF) {
                builder.append(ch);
            } else {
                builder.append('?');
            }
        }
        return builder.toString();
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

    private void ensureAiShoppingList(String userEmail, LocalDate weekStart, ShoppingList list) {
        if (list == null || weekStart == null || hasItems(list)) {
            return;
        }
        mealPlanService.getAiPlanByWeekStart(userEmail, weekStart, false);
    }

    private boolean hasItems(ShoppingList list) {
        return list.getCategories().stream().anyMatch(category -> category.getItems() != null && !category.getItems().isEmpty());
    }

    private ShoppingListResponseDTO mapToDTO(ShoppingList list, String dayOfWeek) {
        ShoppingListResponseDTO dto = new ShoppingListResponseDTO();
        dto.setId(list.getId());
        dto.setWeekStart(list.getWeekStart());
        List<ShoppingCategoryDTO> categories = list.getCategories().stream().map(category -> {
            ShoppingCategoryDTO categoryDTO = new ShoppingCategoryDTO();
            categoryDTO.setName(category.getName());
            List<ShoppingItemDTO> items = category.getItems().stream()
                    .filter(item -> dayOfWeek == null || item.getDayOfWeek() == null || dayOfWeek.equalsIgnoreCase(item.getDayOfWeek()))
                    .map(item -> {
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

    private String normalizeDay(String day) {
        if (day == null) {
            return null;
        }
        String value = day.trim().toLowerCase();
        if (value.isBlank() || value.equals("all") || value.equals("week")) {
            return null;
        }
        return switch (value) {
            case "mon", "monday", "b.e", "be", "b.e." -> "Mon";
            case "tue", "tues", "tuesday", "ç.a", "ca", "ç.a." -> "Tue";
            case "wed", "wednesday", "ç", "c", "ç." -> "Wed";
            case "thu", "thur", "thurs", "thursday", "c.a", "ca.", "c.a." -> "Thu";
            case "fri", "friday", "cüm", "cum", "cüm." -> "Fri";
            case "sat", "saturday", "ş", "s", "ş." -> "Sat";
            case "sun", "sunday", "ba", "bazar" -> "Sun";
            default -> null;
        };
    }
}
