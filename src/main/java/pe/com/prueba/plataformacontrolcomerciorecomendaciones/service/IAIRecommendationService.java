package pe.com.prueba.plataformacontrolcomerciorecomendaciones.service;

import pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.InteractionRequest;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation;

import java.util.List;
import java.util.Map;

public interface IAIRecommendationService
{
    public List<ProductRecommendation> generateRecommendations(Long userId,
            int limit);

    public List<ProductRecommendation> getCollaborativeFilteringRecommendations(
            Long userId, int limit);

    public List<ProductRecommendation> getContentBasedRecommendations(
            Long userId, int limit);

    public List<ProductRecommendation> getLocalProducerRecommendations(
            Long userId, int limit);

    public List<ProductRecommendation> getPopularityBasedRecommendations(
            int limit);

    public void trackUserInteraction(InteractionRequest request);

    public Map<String, Object> analyzeUserProfile(Long userId);

}
