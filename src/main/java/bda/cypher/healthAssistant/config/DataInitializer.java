package bda.cypher.healthAssistant.config;

import bda.cypher.healthAssistant.entity.ConditionCategory;
import bda.cypher.healthAssistant.entity.HealthCondition;
import bda.cypher.healthAssistant.repository.ConditionCategoryRepository;
import bda.cypher.healthAssistant.repository.HealthConditionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ConditionCategoryRepository categoryRepository;
    private final HealthConditionRepository conditionRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            List<String> categories = Arrays.asList(
                "Ürək–damar sistemi", 
                "Tənəffüs sistemi", 
                "Endokrin və maddələr mübadiləsi",
                "Sinir sistemi", 
                "Əzələ–skelet sistemi", 
                "Həzm sistemi (mədə–bağırsaq)",
                "İmmun və autoimmun xəstəliklər", 
                "Psixi pozuntular",
                "Qan və qan yaradan orqanlar", 
                "Böyrək və qaraciyər xəstəlikləri"
            );

            for (String catName : categories) {
                ConditionCategory category = new ConditionCategory();
                category.setName(catName);
                ConditionCategory savedCategory = categoryRepository.save(category);
                
                // Add a default 'Digər' condition for each category so the dropdown isn't empty
                HealthCondition condition = new HealthCondition();
                condition.setName("Digər");
                condition.setCategory(savedCategory);
                conditionRepository.save(condition);
            }
            System.out.println("✅ Initial health data seeded successfully.");
        }
    }
}
