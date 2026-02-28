package bda.cypher.healthAssistant.config;

import bda.cypher.healthAssistant.entity.ConditionCategory;
import bda.cypher.healthAssistant.entity.ConditionCategoryTranslation;
import bda.cypher.healthAssistant.entity.HealthCondition;
import bda.cypher.healthAssistant.entity.HealthConditionTranslation;
import bda.cypher.healthAssistant.entity.MealTemplate;
import bda.cypher.healthAssistant.entity.MealTemplateIngredient;
import bda.cypher.healthAssistant.repository.ConditionCategoryRepository;
import bda.cypher.healthAssistant.repository.ConditionCategoryTranslationRepository;
import bda.cypher.healthAssistant.repository.HealthConditionRepository;
import bda.cypher.healthAssistant.repository.HealthConditionTranslationRepository;
import bda.cypher.healthAssistant.repository.MealTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ConditionCategoryRepository categoryRepository;
    private final HealthConditionRepository conditionRepository;
    private final ConditionCategoryTranslationRepository categoryTranslationRepository;
    private final HealthConditionTranslationRepository conditionTranslationRepository;
    private final MealTemplateRepository mealTemplateRepository;

    @Override
    public void run(String... args) throws Exception {
        Map<String, List<ConditionPair>> conditionsByCategory = new LinkedHashMap<>();
        conditionsByCategory.put("Ürək–damar sistemi", Arrays.asList(
                new ConditionPair("Arterial hipertoniya", "Hypertension"),
                new ConditionPair("İskemik ürək xəstəliyi", "Ischemic heart disease"),
                new ConditionPair("Ürək çatışmazlığı", "Heart failure"),
                new ConditionPair("Aritmiya", "Arrhythmia"),
                new ConditionPair("Ateroskleroz", "Atherosclerosis")
        ));
        conditionsByCategory.put("Tənəffüs sistemi", Arrays.asList(
                new ConditionPair("Bronxial astma", "Bronchial asthma"),
                new ConditionPair("Xroniki bronxit", "Chronic bronchitis"),
                new ConditionPair("XOAX", "COPD"),
                new ConditionPair("Allergik rinit", "Allergic rhinitis")
        ));
        conditionsByCategory.put("Endokrin və maddələr mübadiləsi", Arrays.asList(
                new ConditionPair("Şəkərli diabet", "Diabetes mellitus"),
                new ConditionPair("Hipotireoz", "Hypothyroidism"),
                new ConditionPair("Hipertireoz", "Hyperthyroidism"),
                new ConditionPair("Piylənmə", "Obesity"),
                new ConditionPair("Metabolik sindrom", "Metabolic syndrome")
        ));
        conditionsByCategory.put("Sinir sistemi", Arrays.asList(
                new ConditionPair("Epilepsiya", "Epilepsy"),
                new ConditionPair("Migren", "Migraine"),
                new ConditionPair("Parkinson", "Parkinson's disease"),
                new ConditionPair("Dağınıq skleroz", "Multiple sclerosis")
        ));
        conditionsByCategory.put("Əzələ–skelet sistemi", Arrays.asList(
                new ConditionPair("Osteoxondroz", "Osteochondrosis"),
                new ConditionPair("Artroz", "Osteoarthritis"),
                new ConditionPair("Artrit", "Arthritis"),
                new ConditionPair("Revmatoid artrit", "Rheumatoid arthritis"),
                new ConditionPair("Osteoporoz", "Osteoporosis")
        ));
        conditionsByCategory.put("Həzm sistemi (mədə–bağırsaq)", Arrays.asList(
                new ConditionPair("Xroniki qastrit", "Chronic gastritis"),
                new ConditionPair("Xora", "Peptic ulcer"),
                new ConditionPair("Pankreatit", "Pancreatitis"),
                new ConditionPair("Qıcıqlanmış bağırsaq", "Irritable bowel syndrome")
        ));
        conditionsByCategory.put("İmmun və autoimmun xəstəliklər", Arrays.asList(
                new ConditionPair("Psoriaz", "Psoriasis"),
                new ConditionPair("Qurdeşənəyi", "Urticaria"),
                new ConditionPair("Kron", "Crohn's disease"),
                new ConditionPair("Çölyak", "Celiac disease")
        ));
        conditionsByCategory.put("Psixi pozuntular", Arrays.asList(
                new ConditionPair("Depressiya", "Depression"),
                new ConditionPair("Anksiyete", "Anxiety"),
                new ConditionPair("Bipolyar", "Bipolar disorder")
        ));
        conditionsByCategory.put("Qan və qan yaradan orqanlar", Arrays.asList(
                new ConditionPair("Anemiya", "Anemia"),
                new ConditionPair("Talassemiya", "Thalassemia")
        ));
        conditionsByCategory.put("Böyrək və qaraciyər xəstəlikləri", Arrays.asList(
                new ConditionPair("Böyrək çatışmazlığı", "Kidney failure"),
                new ConditionPair("Hepatit", "Hepatitis"),
                new ConditionPair("Sirroz", "Cirrhosis")
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
        seedMealTemplates();
    }

    private record ConditionPair(String az, String en) {}

    private void seedMealTemplates() {
        if (mealTemplateRepository.count() > 0) {
            return;
        }
        List<HealthCondition> conditions = conditionRepository.findAll();
        if (conditions.isEmpty()) {
            return;
        }
        Set<HealthCondition> allConditions = new HashSet<>(conditions);
        List<MealSeed> meals = new ArrayList<>();

        meals.add(meal("Qarabaşaq sıyığı", "Breakfast",
                ing("Qarabaşaq", "60 q", "Taxıllar"),
                ing("Badam südü", "200 ml", "Süd məhsulları"),
                ing("Alma", "1 ədəd", "Meyvələr"),
                ing("Darçın", "1 ç.q.", "Digər"),
                ing("Qoz", "15 q", "Qoz-fındıq")
        ));
        meals.add(meal("Quinoa sıyığı", "Breakfast",
                ing("Quinoa", "60 q", "Taxıllar"),
                ing("Qatıq", "150 q", "Süd məhsulları"),
                ing("Yaban mersini", "60 q", "Meyvələr"),
                ing("Kətan toxumu", "1 x.q.", "Digər")
        ));
        meals.add(meal("Darı sıyığı", "Breakfast",
                ing("Darı", "60 q", "Taxıllar"),
                ing("Qatıq", "120 q", "Süd məhsulları"),
                ing("Armud", "1 ədəd", "Meyvələr"),
                ing("Badam", "10 q", "Qoz-fındıq")
        ));
        meals.add(meal("Qəhvəyi düyü sıyığı", "Breakfast",
                ing("Qəhvəyi düyü", "60 q", "Taxıllar"),
                ing("Badam südü", "200 ml", "Süd məhsulları"),
                ing("Kivi", "1 ədəd", "Meyvələr"),
                ing("Çia toxumu", "1 x.q.", "Digər")
        ));
        meals.add(meal("Amarant sıyığı", "Breakfast",
                ing("Amarant", "60 q", "Taxıllar"),
                ing("Qatıq", "120 q", "Süd məhsulları"),
                ing("Nar dənələri", "50 q", "Meyvələr"),
                ing("Fındıq", "10 q", "Qoz-fındıq")
        ));
        meals.add(meal("Qarğıdalı yarması sıyığı", "Breakfast",
                ing("Qarğıdalı yarması", "60 q", "Taxıllar"),
                ing("Badam südü", "200 ml", "Süd məhsulları"),
                ing("Çiyələk", "80 q", "Meyvələr"),
                ing("Balqabaq tumu", "1 x.q.", "Qoz-fındıq")
        ));
        meals.add(meal("Ispanaqlı omlet", "Breakfast",
                ing("Yumurta", "2 ədəd", "Ət/Balıq"),
                ing("İspanaq", "70 q", "Tərəvəzlər"),
                ing("Pomidor", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 ç.q.", "Digər")
        ));
        meals.add(meal("Göbələkli omlet", "Breakfast",
                ing("Yumurta", "2 ədəd", "Ət/Balıq"),
                ing("Göbələk", "80 q", "Tərəvəzlər"),
                ing("Bolqar bibəri", "1/2 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 ç.q.", "Digər")
        ));
        meals.add(meal("Kəsmik və giləmeyvə", "Breakfast",
                ing("Kəsmik", "150 q", "Süd məhsulları"),
                ing("Moruq", "60 q", "Meyvələr"),
                ing("Qoz", "10 q", "Qoz-fındıq")
        ));
        meals.add(meal("Qatıq və çia", "Breakfast",
                ing("Qatıq", "180 q", "Süd məhsulları"),
                ing("Çia toxumu", "1 x.q.", "Digər"),
                ing("Çiyələk", "60 q", "Meyvələr"),
                ing("Badam", "10 q", "Qoz-fındıq")
        ));
        meals.add(meal("Avokadolu yumurta", "Breakfast",
                ing("Yumurta", "2 ədəd", "Ət/Balıq"),
                ing("Avokado", "1/2 ədəd", "Meyvələr"),
                ing("Xiyar", "1 ədəd", "Tərəvəzlər"),
                ing("Pomidor", "1 ədəd", "Tərəvəzlər")
        ));
        meals.add(meal("Quinoa salatı", "Breakfast",
                ing("Quinoa", "60 q", "Taxıllar"),
                ing("Xiyar", "1 ədəd", "Tərəvəzlər"),
                ing("Pomidor", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Kətanlı qatıq", "Breakfast",
                ing("Qatıq", "170 q", "Süd məhsulları"),
                ing("Kətan toxumu", "1 x.q.", "Digər"),
                ing("Alma", "1 ədəd", "Meyvələr"),
                ing("Darçın", "1 ç.q.", "Digər")
        ));
        meals.add(meal("Tərəvəzli qayğanaq", "Breakfast",
                ing("Yumurta", "2 ədəd", "Ət/Balıq"),
                ing("Brokkoli", "60 q", "Tərəvəzlər"),
                ing("Kök", "1/2 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 ç.q.", "Digər")
        ));
        meals.add(meal("Kefir və qoz", "Breakfast",
                ing("Kefir", "200 ml", "Süd məhsulları"),
                ing("Qoz", "15 q", "Qoz-fındıq"),
                ing("Armud", "1 ədəd", "Meyvələr")
        ));
        meals.add(meal("Noxudlu salat", "Breakfast",
                ing("Noxud", "120 q", "Digər"),
                ing("Xiyar", "1 ədəd", "Tərəvəzlər"),
                ing("Pomidor", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Qarabaşaq salatı", "Breakfast",
                ing("Qarabaşaq", "60 q", "Taxıllar"),
                ing("İspanaq", "60 q", "Tərəvəzlər"),
                ing("Pomidor", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Tofu və tərəvəz", "Breakfast",
                ing("Tofu", "120 q", "Ət/Balıq"),
                ing("Bolqar bibəri", "1 ədəd", "Tərəvəzlər"),
                ing("Ispanaq", "60 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 ç.q.", "Digər")
        ));

        meals.add(meal("Toyuq tərəvəz şorbası", "Lunch",
                ing("Toyuq filesi", "120 q", "Ət/Balıq"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Kərəviz", "60 q", "Tərəvəzlər"),
                ing("Soğan", "1/2 ədəd", "Tərəvəzlər")
        ));
        meals.add(meal("Mərcimək şorbası", "Lunch",
                ing("Qırmızı mərcimək", "80 q", "Digər"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Soğan", "1/2 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 ç.q.", "Digər")
        ));
        meals.add(meal("Qızılbalıq və quinoa", "Lunch",
                ing("Qızılbalıq", "150 q", "Ət/Balıq"),
                ing("Quinoa", "70 q", "Taxıllar"),
                ing("Brokkoli", "100 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Hinduşka kotleti", "Lunch",
                ing("Hinduşka filesi", "150 q", "Ət/Balıq"),
                ing("Qarabaşaq", "70 q", "Taxıllar"),
                ing("Kələm", "80 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Buxarda balıq və tərəvəz", "Lunch",
                ing("Balıq filesi", "150 q", "Ət/Balıq"),
                ing("Gülkələm", "100 q", "Tərəvəzlər"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 ç.q.", "Digər")
        ));
        meals.add(meal("Toyuq sote və qara düyü", "Lunch",
                ing("Toyuq filesi", "140 q", "Ət/Balıq"),
                ing("Qara düyü", "70 q", "Taxıllar"),
                ing("Balqabaq", "80 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Noxudlu tərəvəz güveci", "Lunch",
                ing("Noxud", "120 q", "Digər"),
                ing("Badımcan", "80 q", "Tərəvəzlər"),
                ing("Pomidor", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Kinoa və tərəvəz salatı", "Lunch",
                ing("Quinoa", "70 q", "Taxıllar"),
                ing("Xiyar", "1 ədəd", "Tərəvəzlər"),
                ing("Pomidor", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Dana filesi və tərəvəz", "Lunch",
                ing("Dana filesi", "150 q", "Ət/Balıq"),
                ing("Brokkoli", "100 q", "Tərəvəzlər"),
                ing("Gülkələm", "100 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Tərəvəzli düyü plovu", "Lunch",
                ing("Qəhvəyi düyü", "80 q", "Taxıllar"),
                ing("Noxud", "80 q", "Digər"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Soğan", "1/2 ədəd", "Tərəvəzlər")
        ));
        meals.add(meal("Tofu və brokkoli", "Lunch",
                ing("Tofu", "150 q", "Ət/Balıq"),
                ing("Brokkoli", "120 q", "Tərəvəzlər"),
                ing("Qarabaşaq", "60 q", "Taxıllar"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Balıq şorbası", "Lunch",
                ing("Balıq filesi", "150 q", "Ət/Balıq"),
                ing("Kərəviz", "60 q", "Tərəvəzlər"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Soğan", "1/2 ədəd", "Tərəvəzlər")
        ));
        meals.add(meal("Toyuq salatı", "Lunch",
                ing("Toyuq filesi", "140 q", "Ət/Balıq"),
                ing("Salat yarpağı", "80 q", "Tərəvəzlər"),
                ing("Avokado", "1/2 ədəd", "Meyvələr"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Zülallı mərcimək salatı", "Lunch",
                ing("Yaşıl mərcimək", "80 q", "Digər"),
                ing("Xiyar", "1 ədəd", "Tərəvəzlər"),
                ing("Pomidor", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Hinduşka tərəvəz güveci", "Lunch",
                ing("Hinduşka filesi", "150 q", "Ət/Balıq"),
                ing("Balqabaq", "80 q", "Tərəvəzlər"),
                ing("Badımcan", "80 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Qarabaşaq və tərəvəz", "Lunch",
                ing("Qarabaşaq", "80 q", "Taxıllar"),
                ing("Gülkələm", "100 q", "Tərəvəzlər"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Somon salatı", "Lunch",
                ing("Somon", "140 q", "Ət/Balıq"),
                ing("Salat yarpağı", "80 q", "Tərəvəzlər"),
                ing("Xiyar", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Fasulyeli tərəvəz şorbası", "Lunch",
                ing("Fasulye", "100 q", "Digər"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Kərəviz", "60 q", "Tərəvəzlər"),
                ing("Soğan", "1/2 ədəd", "Tərəvəzlər")
        ));
        meals.add(meal("Balqabaq şorbası", "Lunch",
                ing("Balqabaq", "150 q", "Tərəvəzlər"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Soğan", "1/2 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 ç.q.", "Digər")
        ));

        meals.add(meal("Fırında balıq və tərəvəz", "Dinner",
                ing("Balıq filesi", "160 q", "Ət/Balıq"),
                ing("Balqabaq", "100 q", "Tərəvəzlər"),
                ing("Gülkələm", "100 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Toyuq və tərəvəz salatı", "Dinner",
                ing("Toyuq filesi", "140 q", "Ət/Balıq"),
                ing("Salat yarpağı", "80 q", "Tərəvəzlər"),
                ing("Pomidor", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Tərəvəzli güveç", "Dinner",
                ing("Badımcan", "80 q", "Tərəvəzlər"),
                ing("Balqabaq", "80 q", "Tərəvəzlər"),
                ing("Pomidor", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Qarabaşaq və toyuq", "Dinner",
                ing("Qarabaşaq", "70 q", "Taxıllar"),
                ing("Toyuq filesi", "140 q", "Ət/Balıq"),
                ing("Brokkoli", "100 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Quinoa və tərəvəz", "Dinner",
                ing("Quinoa", "70 q", "Taxıllar"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Gülkələm", "100 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Buxarda hinduşka", "Dinner",
                ing("Hinduşka filesi", "150 q", "Ət/Balıq"),
                ing("Kərəviz", "60 q", "Tərəvəzlər"),
                ing("Brokkoli", "100 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 ç.q.", "Digər")
        ));
        meals.add(meal("Mərcimək və tərəvəz", "Dinner",
                ing("Yaşıl mərcimək", "80 q", "Digər"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Gülkələm", "100 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Tofu tərəvəz sote", "Dinner",
                ing("Tofu", "150 q", "Ət/Balıq"),
                ing("Balqabaq", "80 q", "Tərəvəzlər"),
                ing("Bolqar bibəri", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Dana filesi və kələm", "Dinner",
                ing("Dana filesi", "150 q", "Ət/Balıq"),
                ing("Kələm", "100 q", "Tərəvəzlər"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Balıq və ispanaq", "Dinner",
                ing("Balıq filesi", "160 q", "Ət/Balıq"),
                ing("İspanaq", "80 q", "Tərəvəzlər"),
                ing("Pomidor", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Noxud və tərəvəz", "Dinner",
                ing("Noxud", "120 q", "Digər"),
                ing("Badımcan", "80 q", "Tərəvəzlər"),
                ing("Balqabaq", "80 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Qara düyü və tərəvəz", "Dinner",
                ing("Qara düyü", "70 q", "Taxıllar"),
                ing("Brokkoli", "100 q", "Tərəvəzlər"),
                ing("Gülkələm", "100 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Somon və kinoa", "Dinner",
                ing("Somon", "150 q", "Ət/Balıq"),
                ing("Quinoa", "60 q", "Taxıllar"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Toyuq tərəvəz güveci", "Dinner",
                ing("Toyuq filesi", "150 q", "Ət/Balıq"),
                ing("Badımcan", "80 q", "Tərəvəzlər"),
                ing("Balqabaq", "80 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Balqabaq və kərəviz güveci", "Dinner",
                ing("Balqabaq", "120 q", "Tərəvəzlər"),
                ing("Kərəviz", "60 q", "Tərəvəzlər"),
                ing("Pomidor", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Hinduşka və gülkələm", "Dinner",
                ing("Hinduşka filesi", "150 q", "Ət/Balıq"),
                ing("Gülkələm", "120 q", "Tərəvəzlər"),
                ing("Kök", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Kinoa və göbələk", "Dinner",
                ing("Quinoa", "70 q", "Taxıllar"),
                ing("Göbələk", "100 q", "Tərəvəzlər"),
                ing("İspanaq", "60 q", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));
        meals.add(meal("Tofu və kələm salatı", "Dinner",
                ing("Tofu", "120 q", "Ət/Balıq"),
                ing("Kələm", "100 q", "Tərəvəzlər"),
                ing("Xiyar", "1 ədəd", "Tərəvəzlər"),
                ing("Zeytun yağı", "1 x.q.", "Digər")
        ));

        for (MealSeed seed : meals) {
            MealTemplate template = new MealTemplate();
            template.setName(seed.name);
            template.setMealType(seed.mealType);
            template.getConditions().addAll(allConditions);
            for (IngredientSeed ing : seed.ingredients) {
                MealTemplateIngredient ingredient = new MealTemplateIngredient();
                ingredient.setMealTemplate(template);
                ingredient.setName(ing.name);
                ingredient.setQuantity(ing.quantity);
                ingredient.setCategory(ing.category);
                template.getIngredients().add(ingredient);
            }
            mealTemplateRepository.save(template);
        }
    }

    private static MealSeed meal(String name, String mealType, IngredientSeed... ingredients) {
        return new MealSeed(name, mealType, Arrays.asList(ingredients));
    }

    private static IngredientSeed ing(String name, String quantity, String category) {
        return new IngredientSeed(name, quantity, category);
    }

    private record MealSeed(String name, String mealType, List<IngredientSeed> ingredients) {}

    private record IngredientSeed(String name, String quantity, String category) {}
}
