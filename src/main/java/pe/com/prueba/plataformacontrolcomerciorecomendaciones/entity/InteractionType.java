package pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity;

public enum InteractionType
{
    VIEW(0.1), ADD_TO_CART(0.5), PURCHASE(1.0), FAVORITE(0.3);

    private final double defaultScore;

    InteractionType(double defaultScore)
    {
        this.defaultScore = defaultScore;
    }

    public double getDefaultScore()
    {
        return defaultScore;
    }
}