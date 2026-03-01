package bda.cypher.healthAssistant.service;

import bda.cypher.healthAssistant.dto.ShoppingListResponseDTO;
import bda.cypher.healthAssistant.dto.ShoppingListUpdateRequestDTO;

import java.time.LocalDate;

public interface ShoppingListService {
    ShoppingListResponseDTO getCurrentList(String userEmail);
    ShoppingListResponseDTO getListByWeekStart(String userEmail, LocalDate weekStart);
    ShoppingListResponseDTO getListByWeekStart(String userEmail, LocalDate weekStart, String dayOfWeek);
    ShoppingListResponseDTO updateItems(String userEmail, Long id, ShoppingListUpdateRequestDTO request);
    byte[] exportList(String userEmail, Long id);
    byte[] exportList(String userEmail, Long id, String format);
}
