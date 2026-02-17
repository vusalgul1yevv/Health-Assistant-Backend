package bda.cypher.healthAssistant.dto;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListUpdateRequestDTO {
    private List<ShoppingItemUpdateDTO> items = new ArrayList<>();

    public List<ShoppingItemUpdateDTO> getItems() {
        return items;
    }

    public void setItems(List<ShoppingItemUpdateDTO> items) {
        this.items = items;
    }
}
