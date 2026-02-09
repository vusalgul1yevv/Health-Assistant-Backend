package bda.cypher.healthAssistant.service;

import bda.cypher.healthAssistant.dto.ConditionCategoryDTO;
import java.util.List;

public interface ConditionCategoryService {
    List<ConditionCategoryDTO> getAllCategories();
}
