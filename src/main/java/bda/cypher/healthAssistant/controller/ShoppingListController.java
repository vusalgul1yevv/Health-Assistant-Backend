package bda.cypher.healthAssistant.controller;

import bda.cypher.healthAssistant.dto.ShoppingListResponseDTO;
import bda.cypher.healthAssistant.dto.ShoppingListUpdateRequestDTO;
import bda.cypher.healthAssistant.service.ShoppingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/shopping-lists")
@RequiredArgsConstructor
public class ShoppingListController {
    private final ShoppingListService shoppingListService;

    @GetMapping("/current")
    public ResponseEntity<ShoppingListResponseDTO> getCurrent(Authentication authentication) {
        return ResponseEntity.ok(shoppingListService.getCurrentList(authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<ShoppingListResponseDTO> getByWeekStart(@RequestParam(required = false) LocalDate weekStart,
                                                                  Authentication authentication) {
        if (weekStart == null) {
            return ResponseEntity.ok(shoppingListService.getCurrentList(authentication.getName()));
        }
        return ResponseEntity.ok(shoppingListService.getListByWeekStart(authentication.getName(), weekStart));
    }

    @PutMapping("/{id}/items")
    public ResponseEntity<ShoppingListResponseDTO> updateItems(@PathVariable Long id,
                                                               @Valid @RequestBody ShoppingListUpdateRequestDTO request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(shoppingListService.updateItems(authentication.getName(), id, request));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id,
                                         @RequestParam(required = false) String format,
                                         Authentication authentication) {
        byte[] content = shoppingListService.exportList(authentication.getName(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shopping-list-" + id + ".txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }
}
