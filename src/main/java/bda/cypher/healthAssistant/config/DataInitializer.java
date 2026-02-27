package bda.cypher.healthAssistant.config;

import bda.cypher.healthAssistant.entity.ConditionCategory;
import bda.cypher.healthAssistant.entity.ConditionCategoryTranslation;
import bda.cypher.healthAssistant.entity.HealthCondition;
import bda.cypher.healthAssistant.entity.HealthConditionTranslation;
import bda.cypher.healthAssistant.repository.ConditionCategoryRepository;
import bda.cypher.healthAssistant.repository.ConditionCategoryTranslationRepository;
import bda.cypher.healthAssistant.repository.HealthConditionRepository;
import bda.cypher.healthAssistant.repository.HealthConditionTranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ConditionCategoryRepository categoryRepository;
    private final HealthConditionRepository conditionRepository;
    private final ConditionCategoryTranslationRepository categoryTranslationRepository;
    private final HealthConditionTranslationRepository conditionTranslationRepository;

    @Override
    public void run(String... args) throws Exception {
        Map<String, List<ConditionPair>> conditionsByCategory = new LinkedHashMap<>();
        conditionsByCategory.put("Ürək–damar sistemi", Arrays.asList(
                new ConditionPair("Arterial hipertoniya", "Hypertension"),
                new ConditionPair("İskemik ürək xəstəliyi", "Ischemic heart disease"),
                new ConditionPair("Ürək çatışmazlığı", "Heart failure"),
                new ConditionPair("Aritmiya", "Arrhythmia"),
                new ConditionPair("Ateroskleroz", "Atherosclerosis"),
                new ConditionPair("Digər", "Other")
        ));
        conditionsByCategory.put("Tənəffüs sistemi", Arrays.asList(
                new ConditionPair("Bronxial astma", "Bronchial asthma"),
                new ConditionPair("Xroniki bronxit", "Chronic bronchitis"),
                new ConditionPair("XOAX", "COPD"),
                new ConditionPair("Allergik rinit", "Allergic rhinitis"),
                new ConditionPair("Digər", "Other")
        ));
        conditionsByCategory.put("Endokrin və maddələr mübadiləsi", Arrays.asList(
                new ConditionPair("Şəkərli diabet", "Diabetes mellitus"),
                new ConditionPair("Hipotireoz", "Hypothyroidism"),
                new ConditionPair("Hipertireoz", "Hyperthyroidism"),
                new ConditionPair("Piylənmə", "Obesity"),
                new ConditionPair("Metabolik sindrom", "Metabolic syndrome"),
                new ConditionPair("Digər", "Other")
        ));
        conditionsByCategory.put("Sinir sistemi", Arrays.asList(
                new ConditionPair("Epilepsiya", "Epilepsy"),
                new ConditionPair("Migren", "Migraine"),
                new ConditionPair("Parkinson", "Parkinson's disease"),
                new ConditionPair("Dağınıq skleroz", "Multiple sclerosis"),
                new ConditionPair("Digər", "Other")
        ));
        conditionsByCategory.put("Əzələ–skelet sistemi", Arrays.asList(
                new ConditionPair("Osteoxondroz", "Osteochondrosis"),
                new ConditionPair("Artroz", "Osteoarthritis"),
                new ConditionPair("Artrit", "Arthritis"),
                new ConditionPair("Revmatoid artrit", "Rheumatoid arthritis"),
                new ConditionPair("Osteoporoz", "Osteoporosis"),
                new ConditionPair("Digər", "Other")
        ));
        conditionsByCategory.put("Həzm sistemi (mədə–bağırsaq)", Arrays.asList(
                new ConditionPair("Xroniki qastrit", "Chronic gastritis"),
                new ConditionPair("Xora", "Peptic ulcer"),
                new ConditionPair("Pankreatit", "Pancreatitis"),
                new ConditionPair("Qıcıqlanmış bağırsaq", "Irritable bowel syndrome"),
                new ConditionPair("Digər", "Other")
        ));
        conditionsByCategory.put("İmmun və autoimmun xəstəliklər", Arrays.asList(
                new ConditionPair("Psoriaz", "Psoriasis"),
                new ConditionPair("Qurdeşənəyi", "Urticaria"),
                new ConditionPair("Kron", "Crohn's disease"),
                new ConditionPair("Çölyak", "Celiac disease"),
                new ConditionPair("Digər", "Other")
        ));
        conditionsByCategory.put("Psixi pozuntular", Arrays.asList(
                new ConditionPair("Depressiya", "Depression"),
                new ConditionPair("Anksiyete", "Anxiety"),
                new ConditionPair("Bipolyar", "Bipolar disorder"),
                new ConditionPair("Digər", "Other")
        ));
        conditionsByCategory.put("Qan və qan yaradan orqanlar", Arrays.asList(
                new ConditionPair("Anemiya", "Anemia"),
                new ConditionPair("Talassemiya", "Thalassemia"),
                new ConditionPair("Digər", "Other")
        ));
        conditionsByCategory.put("Böyrək və qaraciyər xəstəlikləri", Arrays.asList(
                new ConditionPair("Böyrək çatışmazlığı", "Kidney failure"),
                new ConditionPair("Hepatit", "Hepatitis"),
                new ConditionPair("Sirroz", "Cirrhosis"),
                new ConditionPair("Digər", "Other")
        ));

        Map<String, String> categoryTranslations = new LinkedHashMap<>();
        categoryTranslations.put("Ürək–damar sistemi", "Cardiovascular system");
        categoryTranslations.put("Tənəffüs sistemi", "Respiratory system");
        categoryTranslations.put("Endokrin və maddələr mübadiləsi", "Endocrine and metabolism");
        categoryTranslations.put("Sinir sistemi", "Nervous system");
        categoryTranslations.put("Əzələ–skelet sistemi", "Musculoskeletal system");
        categoryTranslations.put("Həzm sistemi (mədə–bağırsaq)", "Digestive system (gastrointestinal)");
        categoryTranslations.put("İmmun və autoimmun xəstəliklər", "Immune and autoimmune diseases");
        categoryTranslations.put("Psixi pozuntular", "Mental disorders");
        categoryTranslations.put("Qan və qan yaradan orqanlar", "Blood and hematopoietic organs");
        categoryTranslations.put("Böyrək və qaraciyər xəstəlikləri", "Kidney and liver diseases");

        for (Map.Entry<String, List<ConditionPair>> entry : conditionsByCategory.entrySet()) {
            ConditionCategory category = categoryRepository.findByName(entry.getKey())
                    .orElseGet(() -> {
                        ConditionCategory created = new ConditionCategory();
                        created.setName(entry.getKey());
                        return categoryRepository.save(created);
                    });
            categoryTranslationRepository.findByCategoryId(category.getId()).orElseGet(() -> {
                ConditionCategoryTranslation translation = new ConditionCategoryTranslation();
                translation.setCategory(category);
                translation.setNameEn(categoryTranslations.get(entry.getKey()));
                return categoryTranslationRepository.save(translation);
            });
            for (ConditionPair pair : entry.getValue()) {
                List<HealthCondition> matches = conditionRepository.findAllByNameAndCategoryId(pair.az(), category.getId());
                if (matches.isEmpty()) {
                    HealthCondition condition = new HealthCondition();
                    condition.setName(pair.az());
                    condition.setCategory(category);
                    conditionRepository.save(condition);
                }
                HealthCondition condition = matches.isEmpty()
                        ? conditionRepository.findAllByNameAndCategoryId(pair.az(), category.getId()).stream().findFirst().orElse(null)
                        : matches.get(0);
                if (condition != null) {
                    conditionTranslationRepository.findByConditionId(condition.getId()).orElseGet(() -> {
                        HealthConditionTranslation translation = new HealthConditionTranslation();
                        translation.setCondition(condition);
                        translation.setNameEn(pair.en());
                        return conditionTranslationRepository.save(translation);
                    });
                }
            }
        }
    }

    private record ConditionPair(String az, String en) {}
}
