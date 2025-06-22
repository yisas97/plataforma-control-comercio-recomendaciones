package pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto;

import pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType;

public class InteractionRequest
{
    private Long userId;
    private Long productId;
    private InteractionType actionType;
    private String sessionId;

    public InteractionRequest()
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

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }
}
