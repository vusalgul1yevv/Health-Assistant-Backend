package bda.cypher.healthAssistant.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "condition_categories")
public class ConditionCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<HealthCondition> conditions;

    public ConditionCategory() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<HealthCondition> getConditions() { return conditions; }
    public void setConditions(List<HealthCondition> conditions) { this.conditions = conditions; }
}
