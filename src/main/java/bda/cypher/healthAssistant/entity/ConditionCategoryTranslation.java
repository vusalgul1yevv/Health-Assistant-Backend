package bda.cypher.healthAssistant.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "condition_category_translations")
public class ConditionCategoryTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false, unique = true)
    private ConditionCategory category;

    private String nameEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ConditionCategory getCategory() { return category; }
    public void setCategory(ConditionCategory category) { this.category = category; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
}
