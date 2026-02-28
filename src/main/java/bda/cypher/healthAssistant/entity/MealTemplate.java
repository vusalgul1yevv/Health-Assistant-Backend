package bda.cypher.healthAssistant.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "meal_templates")
public class MealTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mealType;

    @OneToMany(mappedBy = "mealTemplate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MealTemplateIngredient> ingredients = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "condition_meal_templates",
            joinColumns = @JoinColumn(name = "meal_template_id"),
            inverseJoinColumns = @JoinColumn(name = "condition_id")
    )
    private Set<HealthCondition> conditions = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public List<MealTemplateIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<MealTemplateIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    public Set<HealthCondition> getConditions() {
        return conditions;
    }

    public void setConditions(Set<HealthCondition> conditions) {
        this.conditions = conditions;
    }
}
