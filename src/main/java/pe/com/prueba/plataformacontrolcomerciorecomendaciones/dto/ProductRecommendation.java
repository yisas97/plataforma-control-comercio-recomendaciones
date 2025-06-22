package pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRecommendation
{
    private Long productId;
    private String productName;
    private String description;
    private Double price;
    private String producerName;
    private String category;
    private Double recommendationScore;
    private String reason;

    // Constructor para JPQL
    public ProductRecommendation(Long productId, String productName,
            Double price, Double recommendationScore, String reason)
    {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.recommendationScore = recommendationScore;
        this.reason = reason;
    }

    // Constructor para COUNT() queries
    public ProductRecommendation(Long productId, String productName,
            Double price, Long recommendationScore, String reason)
    {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.recommendationScore = recommendationScore.doubleValue();
        this.reason = reason;
    }
}