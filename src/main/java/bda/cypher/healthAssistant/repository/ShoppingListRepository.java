package bda.cypher.healthAssistant.repository;

import bda.cypher.healthAssistant.entity.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
    Optional<ShoppingList> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);
    Optional<ShoppingList> findByIdAndUserId(Long id, Long userId);
}
