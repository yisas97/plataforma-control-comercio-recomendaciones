package pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_interactions")
public class UserInteraction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private InteractionType actionType;

    @Column(name = "interaction_score", nullable = false)
    private Double interactionScore;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UserInteraction()
    {
    }

    public UserInteraction(Long userId, Long productId,
            InteractionType actionType, Double interactionScore)
    {
        this.userId = userId;
        this.productId = productId;
        this.actionType = actionType;
        this.interactionScore = interactionScore;
    }

    @PrePersist
    protected void onCreate()
    {
        createdAt = LocalDateTime.now();
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public InteractionType getActionType()
    {
        return actionType;
    }

    public void setActionType(InteractionType actionType)
    {
        this.actionType = actionType;
    }

    public Double getInteractionScore()
    {
        return interactionScore;
    }

    public void setInteractionScore(Double interactionScore)
    {
        this.interactionScore = interactionScore;
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public LocalDateTime getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt)
    {
        this.createdAt = createdAt;
    }
}