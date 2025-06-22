package pe.com.prueba.plataformacontrolcomerciorecomendaciones.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.InteractionRequest;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.UserInteraction;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.repository.UserInteractionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AIRecommendationService implements IAIRecommendationService
{

    private final UserInteractionRepository interactionRepository;

    private final JdbcTemplate jdbcTemplate;

    @Value("${ai.recommendation.min-interactions:3}")
    private int minInteractionsForPersonalized;

    public AIRecommendationService(
            UserInteractionRepository interactionRepository,
            JdbcTemplate jdbcTemplate)
    {
        this.interactionRepository = interactionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProductRecommendation> generateRecommendations(Long userId,
            int limit)
    {
        List<UserInteraction> userInteractions = interactionRepository.findByUserIdOrderByCreatedAtDesc(
                userId);

        log.info("Generating Recommendations for " + userInteractions.size() + " users");
        for (UserInteraction userInteraction : userInteractions) {
            log.info("UserInteraction: " + userInteraction.getId());
        }

        if (userInteractions.size() < minInteractionsForPersonalized)
        {
            return getPopularityBasedRecommendations(limit);
        }

        List<ProductRecommendation> collaborativeRecs = getCollaborativeFilteringRecommendations(
                userId, limit);
        List<ProductRecommendation> contentRecs = getContentBasedRecommendations(
                userId, limit);
        List<ProductRecommendation> localRecs = getLocalProducerRecommendations(
                userId, limit);

        return combineRecommendations(collaborativeRecs, contentRecs, localRecs,
                limit);
    }

    public List<ProductRecommendation> getCollaborativeFilteringRecommendations(
            Long userId, int limit)
    {
        Pageable pageable = PageRequest.of(0, limit);
        return interactionRepository.findCollaborativeRecommendations(userId,
                pageable);
    }

    public List<ProductRecommendation> getContentBasedRecommendations(
            Long userId, int limit)
    {
        Pageable pageable = PageRequest.of(0, limit);
        return interactionRepository.findContentBasedRecommendations(userId,
                pageable);
    }

    public List<ProductRecommendation> getLocalProducerRecommendations(
            Long userId, int limit)
    {
        Pageable pageable = PageRequest.of(0, limit);
        return interactionRepository.findLocalProducerRecommendations(userId,
                pageable);
    }

    public List<ProductRecommendation> getPopularityBasedRecommendations(
            int limit)
    {
        Pageable pageable = PageRequest.of(0, limit);
        return interactionRepository.findPopularRecommendations(pageable);
    }

    // IA: Combinar múltiples algoritmos con pesos
    private List<ProductRecommendation> combineRecommendations(
            List<ProductRecommendation> collaborative,
            List<ProductRecommendation> contentBased,
            List<ProductRecommendation> localProducer, int limit)
    {

        Map<Long, ProductRecommendation> combinedMap = new HashMap<>();

        // Peso para filtrado colaborativo: 0.4
        for (ProductRecommendation rec : collaborative)
        {
            rec.setRecommendationScore(rec.getRecommendationScore() * 0.4);
            combinedMap.put(rec.getProductId(), rec);
        }

        // Peso para filtrado basado en contenido: 0.4
        for (ProductRecommendation rec : contentBased)
        {
            if (combinedMap.containsKey(rec.getProductId()))
            {
                ProductRecommendation existing = combinedMap.get(
                        rec.getProductId());
                existing.setRecommendationScore(
                        existing.getRecommendationScore() + (rec.getRecommendationScore() * 0.4));
                existing.setReason(
                        existing.getReason() + " y " + rec.getReason()
                                .toLowerCase());
            } else
            {
                rec.setRecommendationScore(rec.getRecommendationScore() * 0.4);
                combinedMap.put(rec.getProductId(), rec);
            }
        }

        // Peso para productores locales: 0.2
        for (ProductRecommendation rec : localProducer)
        {
            if (combinedMap.containsKey(rec.getProductId()))
            {
                ProductRecommendation existing = combinedMap.get(
                        rec.getProductId());
                existing.setRecommendationScore(
                        existing.getRecommendationScore() + (rec.getRecommendationScore() * 0.2));
                existing.setReason(existing.getReason() + " (productor local)");
            } else
            {
                rec.setRecommendationScore(rec.getRecommendationScore() * 0.2);
                combinedMap.put(rec.getProductId(), rec);
            }
        }

        return combinedMap.values().stream()
                .sorted((r1, r2) -> Double.compare(r2.getRecommendationScore(),
                        r1.getRecommendationScore())).limit(limit)
                .collect(Collectors.toList());
    }

    // Registrar interacción del usuario
    public void trackUserInteraction(InteractionRequest request)
    {
        UserInteraction interaction = new UserInteraction(request.getUserId(),
                request.getProductId(), request.getActionType(),
                request.getActionType().getDefaultScore());
        interaction.setSessionId(request.getSessionId());

        interactionRepository.save(interaction);
    }

    public Map<String, Object> analyzeUserProfile(Long userId)
    {
        List<UserInteraction> interactions = interactionRepository.findByUserIdOrderByCreatedAtDesc(
                userId);

        Map<String, Object> profile = new HashMap<>();
        profile.put("totalInteractions", interactions.size());
        profile.put("purchaseFrequency",
                calculatePurchaseFrequency(interactions));
        profile.put("preferredCategories", getPreferredCategories(userId));
        profile.put("averageSpending", calculateAverageSpending(userId));
        profile.put("lastActivity", interactions.isEmpty() ?
                null :
                interactions.get(0).getCreatedAt());

        return profile;
    }

    private double calculatePurchaseFrequency(
            List<UserInteraction> interactions)
    {
        long purchases = interactions.stream()
                .filter(i -> i.getActionType() == InteractionType.PURCHASE)
                .count();
        return purchases > 0 ? (double) interactions.size() / purchases : 0;
    }

    private List<String> getPreferredCategories(Long userId)
    {
        Pageable pageable = PageRequest.of(0, 5);
        return interactionRepository.findTopCategoriesByUser(userId, pageable);
    }

    private double calculateAverageSpending(Long userId)
    {
        Double result = interactionRepository.findAverageSpending(userId);
        return result != null ? result : 0.0;
    }


    public Long getTotalInteractionsByType(Long userId, String actionType) {
        String sql = "SELECT COUNT(*) FROM user_interactions WHERE user_id = ? AND action_type = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, userId, actionType);
    }

    public String getMostFrequentCategory(Long userId) {
        String sql = """
        SELECT c.name
        FROM user_interactions ui
        JOIN product p ON ui.product_id = p.id
        JOIN product_categories pc ON p.id = pc.product_id
        JOIN categories c ON pc.category_id = c.id
        WHERE ui.user_id = ?
        GROUP BY c.name
        ORDER BY COUNT(*) DESC
        LIMIT 1
        """;

        try {
            return jdbcTemplate.queryForObject(sql, String.class, userId);
        } catch (Exception e) {
            return "Sin datos";
        }
    }
}
