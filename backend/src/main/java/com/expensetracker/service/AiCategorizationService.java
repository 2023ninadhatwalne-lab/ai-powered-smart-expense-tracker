package com.expensetracker.service;

import com.expensetracker.dto.AiCategorizeRequest;
import com.expensetracker.dto.AiCategorizeResponse;
import com.expensetracker.model.Category;
import com.expensetracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiCategorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AiCategorizationService.class);

    private final CategoryRepository categoryRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ai-service.base-url:http://localhost:8000/ai/v1}")
    private String aiServiceBaseUrl;

    // Deterministic semantic rules dictionary for fallback
    private static final Map<String, List<String>> KEYWORD_CATEGORY_MAP = new LinkedHashMap<>();

    static {
        KEYWORD_CATEGORY_MAP.put("Food & Dining", Arrays.asList(
                "swiggy", "zomato", "mcdonald", "starbucks", "dominos", "pizza", "burger", "dinner", "lunch", "breakfast",
                "restaurant", "cafe", "coffee", "grocery", "groceries", "supermarket", "subway", "kfc", "dine", "bistro",
                "food", "biryani", "snack", "bakery", "blinkit", "zepto", "instamart", "doordash", "ubereats"
        ));
        KEYWORD_CATEGORY_MAP.put("Transportation", Arrays.asList(
                "uber", "ola", "rapido", "cab", "taxi", "petrol", "diesel", "fuel", "gas", "shell", "metro", "subway",
                "train", "flight", "indigo", "bus", "toll", "parking", "auto", "railway", "irctc"
        ));
        KEYWORD_CATEGORY_MAP.put("Housing & Rent", Arrays.asList(
                "rent", "maintenance", "landlord", "mortgage", "housing", "apartment", "brokerage", "society"
        ));
        KEYWORD_CATEGORY_MAP.put("Utilities & Bills", Arrays.asList(
                "electricity", "water bill", "wifi", "internet", "broadband", "jio", "airtel", "vi", "vodafone", "recharge",
                "gas cylinder", "utility", "bill", "power", "dth"
        ));
        KEYWORD_CATEGORY_MAP.put("Entertainment", Arrays.asList(
                "netflix", "spotify", "prime video", "disney", "hotstar", "cinema", "movie", "pvr", "inox", "bookmyshow",
                "gaming", "steam", "playstation", "xbox", "concert", "club"
        ));
        KEYWORD_CATEGORY_MAP.put("Healthcare", Arrays.asList(
                "pharmacy", "medicine", "apollo", "medplus", "hospital", "doctor", "clinic", "dental", "dentist",
                "pharmeasy", "1mg", "health", "consultation", "lab test", "diagnostics"
        ));
        KEYWORD_CATEGORY_MAP.put("Shopping", Arrays.asList(
                "amazon", "flipkart", "myntra", "zara", "h&m", "nike", "adidas", "clothes", "shoes", "mall", "electronics",
                "apple", "croma", "reliance digital", "retail"
        ));
        KEYWORD_CATEGORY_MAP.put("Education", Arrays.asList(
                "udemy", "coursera", "book", "books", "tuition", "course", "college", "school", "exam", "certification", "training"
        ));
        KEYWORD_CATEGORY_MAP.put("Travel", Arrays.asList(
                "hotel", "airbnb", "makemytrip", "goibibo", "booking.com", "flight", "resort", "vacation", "trip", "tour"
        ));
        KEYWORD_CATEGORY_MAP.put("Investments", Arrays.asList(
                "zerodha", "groww", "mutual fund", "stocks", "sip", "crypto", "binance", "coinbase", "gold", "shares"
        ));
    }

    public AiCategorizeResponse categorize(AiCategorizeRequest request) {
        String inputText = request.getText() != null ? request.getText().trim() : "";
        if (inputText.isEmpty() && request.getMerchant() != null) {
            inputText = request.getMerchant();
        }

        // 1. Try to invoke local Python ML model service (TF-IDF + Logistic Regression)
        try {
            String endpoint = aiServiceBaseUrl + "/categorize";
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", inputText);
            payload.put("merchant", request.getMerchant());
            payload.put("amount", request.getAmount());

            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, payload, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String predictedCategory = (String) body.get("category");
                Double confidence = body.get("confidence") != null ? Double.valueOf(body.get("confidence").toString()) : 0.85;
                String model = body.get("model_name") != null ? (String) body.get("model_name") : "TF-IDF + Logistic Regression (ML Service)";
                String merchant = body.get("merchant") != null ? (String) body.get("merchant") : extractMerchantFallback(inputText);

                Category cat = resolveCategory(predictedCategory);

                return AiCategorizeResponse.builder()
                        .category(cat.getName())
                        .categoryId(cat.getId())
                        .confidence(confidence)
                        .modelName(model)
                        .extractedMerchant(merchant)
                        .build();
            }
        } catch (Exception e) {
            logger.debug("AI micro-service not reachable ({}), engaging intelligent rule fallback classifier.", e.getMessage());
        }

        // 2. Intelligent Rule Classifier (Deterministic Baseline)
        return classifyWithSemanticRules(inputText);
    }

    public AiCategorizeResponse classifyWithSemanticRules(String text) {
        String lower = text.toLowerCase();
        String matchedCategoryName = "Other Expense";
        double highestConfidence = 0.50;
        String matchedKeyword = null;

        for (Map.Entry<String, List<String>> entry : KEYWORD_CATEGORY_MAP.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    matchedCategoryName = entry.getKey();
                    highestConfidence = 0.92;
                    matchedKeyword = keyword;
                    break;
                }
            }
            if (matchedKeyword != null) break;
        }

        String merchant = extractMerchantFallback(text);
        Category category = resolveCategory(matchedCategoryName);

        return AiCategorizeResponse.builder()
                .category(category.getName())
                .categoryId(category.getId())
                .confidence(highestConfidence)
                .modelName("Intelligent Pattern Classifier (Semantic Baseline)")
                .extractedMerchant(merchant)
                .build();
    }

    private String extractMerchantFallback(String text) {
        String[] tokens = text.split("\\s+");
        for (String token : tokens) {
            String clean = token.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            for (List<String> keywords : KEYWORD_CATEGORY_MAP.values()) {
                for (String kw : keywords) {
                    if (clean.equalsIgnoreCase(kw) && kw.length() >= 3) {
                        return capitalize(kw);
                    }
                }
            }
        }
        return tokens.length > 0 ? tokens[0] : "Direct";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private Category resolveCategory(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepository.findByNameIgnoreCase("Other Expense")
                        .orElseGet(() -> categoryRepository.findAll().stream().findFirst()
                                .orElseGet(() -> categoryRepository.save(Category.builder().name(name).type(com.expensetracker.model.CategoryType.EXPENSE).build()))));
    }
}
