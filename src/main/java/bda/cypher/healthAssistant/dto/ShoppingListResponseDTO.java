package bda.cypher.healthAssistant.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ShoppingListResponseDTO {
    private Long id;
    private LocalDate weekStart;
    private List<ShoppingCategoryDTO> categories = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public List<ShoppingCategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<ShoppingCategoryDTO> categories) {
        this.categories = categories;
    }
}
