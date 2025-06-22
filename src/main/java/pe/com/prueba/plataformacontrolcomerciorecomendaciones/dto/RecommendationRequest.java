package pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto;

public class RecommendationRequest
{
    private Long userId;
    private Integer limit = 10;
    private String recommendationType = "HYBRID"; // COLLABORATIVE, CONTENT_BASED, HYBRID

    // Constructors, getters, setters
    public RecommendationRequest()
    {
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Integer getLimit()
    {
        return limit;
    }

    public void setLimit(Integer limit)
    {
        this.limit = limit;
    }

    public String getRecommendationType()
    {
        return recommendationType;
    }

    public void setRecommendationType(String recommendationType)
    {
        this.recommendationType = recommendationType;
    }
}