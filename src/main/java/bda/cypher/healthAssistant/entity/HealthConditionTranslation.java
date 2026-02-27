package bda.cypher.healthAssistant.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "health_condition_translations")
public class HealthConditionTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "condition_id", nullable = false, unique = true)
    private HealthCondition condition;

    private String nameEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public HealthCondition getCondition() { return condition; }
    public void setCondition(HealthCondition condition) { this.condition = condition; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
}
