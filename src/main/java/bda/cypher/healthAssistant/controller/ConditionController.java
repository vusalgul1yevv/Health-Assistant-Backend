package bda.cypher.healthAssistant.controller;

import bda.cypher.healthAssistant.dto.ConditionCategoryDTO;
import bda.cypher.healthAssistant.service.ConditionCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/conditions")
@RequiredArgsConstructor
public class ConditionController {

    private final ConditionCategoryService conditionService;

    @GetMapping
    public ResponseEntity<List<ConditionCategoryDTO>> getAllConditions() {
        return ResponseEntity.ok(conditionService.getAllCategories());
    }
}
