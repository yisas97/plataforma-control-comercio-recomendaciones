package pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ProductRecommendation implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long productId;
    private String productName;
    private String description;
    private Double price;
    private String producerName;
    private String category;
    private Double recommendationScore;
    private String reason;

    public ProductRecommendation(Long productId, String productName, String description,
            Double price, String producerName, String category,
            Double recommendationScore, String reason) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.producerName = producerName;
        this.category = category;
        this.recommendationScore = recommendationScore;
        this.reason = reason;
    }

    public ProductRecommendation(Long productId, String productName, String description,
            Double price, String producerName, String category,
            int recommendationScore, String reason) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.producerName = producerName;
        this.category = category;
        this.recommendationScore = (double) recommendationScore;
        this.reason = reason;
    }

    public ProductRecommendation(Long productId, String productName, Double price,
            Double recommendationScore, String reason) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.recommendationScore = recommendationScore;
        this.reason = reason;
    }

    public ProductRecommendation(Long productId, String productName, Double price,
            Long recommendationScore, String reason) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.recommendationScore = recommendationScore.doubleValue();
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "ProductRecommendation{" +
                "productId=" + productId +
                ", productName='" + productName + '\'' +
                ", price=" + price +
                ", producerName='" + producerName + '\'' +
                ", recommendationScore=" + recommendationScore +
                ", reason='" + reason + '\'' +
                '}';
    }
}