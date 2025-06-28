package pe.com.prueba.plataformacontrolcomerciorecomendaciones.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.InteractionRequest;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.RecommendationRequest;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.service.IAIRecommendationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIRecommendationController
{

    private final IAIRecommendationService aiRecommendationService;

    @Autowired
    public AIRecommendationController(
            IAIRecommendationService aiRecommendationService)
    {
        this.aiRecommendationService = aiRecommendationService;
    }

    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<ProductRecommendation>> getRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit)
    {

        List<ProductRecommendation> recommendations = aiRecommendationService.generateRecommendations(
                userId, limit);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/recommendations")
    public ResponseEntity<List<ProductRecommendation>> getCustomRecommendations(
            @RequestBody RecommendationRequest request)
    {

        List<ProductRecommendation> recommendations = aiRecommendationService.generateRecommendations(
                request.getUserId(), request.getLimit());
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/interactions")
    public ResponseEntity<String> trackInteraction(
            @RequestBody InteractionRequest request)
    {
        aiRecommendationService.trackUserInteraction(request);
        return ResponseEntity.ok("Interaction tracked successfully");
    }

    @GetMapping("/recommendations/popular")
    public ResponseEntity<List<ProductRecommendation>> getPopularRecommendations(
            @RequestParam(defaultValue = "10") int limit)
    {

        List<ProductRecommendation> recommendations = aiRecommendationService.getPopularityBasedRecommendations(
                limit);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck()
    {
        return ResponseEntity.ok("AI Recommendation Service is running");
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable Long userId) {
        Map<String, Object> profile = aiRecommendationService.analyzeUserProfile(userId);

        // Enriquecer con estadísticas adicionales
        Long totalViews = aiRecommendationService.getTotalInteractionsByType(userId, "VIEW");
        Long totalPurchases = aiRecommendationService.getTotalInteractionsByType(userId, "PURCHASE");
        Long totalCartAdds = aiRecommendationService.getTotalInteractionsByType(userId, "ADD_TO_CART");
        String favoriteCategory = aiRecommendationService.getMostFrequentCategory(userId);

        profile.put("totalViews", totalViews);
        profile.put("totalPurchases", totalPurchases);
        profile.put("totalCartAdds", totalCartAdds);
        profile.put("favoriteCategory", favoriteCategory);

        return ResponseEntity.ok(profile);
    }

}