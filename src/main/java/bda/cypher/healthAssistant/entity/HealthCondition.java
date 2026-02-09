package bda.cypher.healthAssistant.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "health_conditions")
public class HealthCondition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private ConditionCategory category;

    public HealthCondition() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ConditionCategory getCategory() { return category; }
    public void setCategory(ConditionCategory category) { this.category = category; }
}
