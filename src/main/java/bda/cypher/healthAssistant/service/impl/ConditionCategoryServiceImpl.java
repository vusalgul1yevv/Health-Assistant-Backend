package bda.cypher.healthAssistant.service.impl;

import bda.cypher.healthAssistant.dto.ConditionCategoryDTO;
import bda.cypher.healthAssistant.dto.HealthConditionDTO;
import bda.cypher.healthAssistant.entity.ConditionCategory;
import bda.cypher.healthAssistant.repository.ConditionCategoryRepository;
import bda.cypher.healthAssistant.service.ConditionCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConditionCategoryServiceImpl implements ConditionCategoryService {
    private final ConditionCategoryRepository categoryRepository;

    public ConditionCategoryServiceImpl(ConditionCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<ConditionCategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private ConditionCategoryDTO mapToDTO(ConditionCategory category) {
        ConditionCategoryDTO dto = new ConditionCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        
        if (category.getConditions() != null) {
            List<HealthConditionDTO> conditionDTOs = category.getConditions().stream().map(cond -> {
                HealthConditionDTO condDto = new HealthConditionDTO();
                condDto.setId(cond.getId());
                condDto.setName(cond.getName());
                return condDto;
            }).collect(Collectors.toList());
            dto.setConditions(conditionDTOs);
        }
        
        return dto;
    }
}
