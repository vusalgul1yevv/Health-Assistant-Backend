package bda.cypher.healthAssistant.dto;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCategoryDTO {
    private String name;
    private List<ShoppingItemDTO> items = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ShoppingItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ShoppingItemDTO> items) {
        this.items = items;
    }
}
