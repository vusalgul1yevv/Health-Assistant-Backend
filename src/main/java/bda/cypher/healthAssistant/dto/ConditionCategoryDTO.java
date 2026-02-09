package bda.cypher.healthAssistant.dto;

import java.util.List;

public class ConditionCategoryDTO {
    private Long id;
    private String name;
    private List<HealthConditionDTO> conditions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<HealthConditionDTO> getConditions() { return conditions; }
    public void setConditions(List<HealthConditionDTO> conditions) { this.conditions = conditions; }
}
