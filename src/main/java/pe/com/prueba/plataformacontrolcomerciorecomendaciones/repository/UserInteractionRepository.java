package pe.com.prueba.plataformacontrolcomerciorecomendaciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.UserInteraction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserInteractionRepository
        extends JpaRepository<UserInteraction, Long>
{

    List<UserInteraction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserInteraction> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<UserInteraction> findByUserIdAndActionType(Long userId,
            InteractionType actionType);

    List<UserInteraction> findByUserIdAndCreatedAtAfter(Long userId,
            LocalDateTime since);

    // IA: Filtrado Colaborativo - CAST para convertir AVG a Double
    @Query("""
            SELECT new pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation(
                p.id, 
                p.name, 
                p.price, 
                CAST(AVG(ui2.interactionScore) AS double),
                'Usuarios con gustos similares también compraron esto'
            )
            FROM UserInteraction ui1
            JOIN UserInteraction ui2 ON ui1.productId = ui2.productId
            JOIN Product p ON ui2.productId = p.id
            WHERE ui1.userId = :userId 
            AND ui2.userId != :userId
            AND ui1.actionType IN (pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.PURCHASE, pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.ADD_TO_CART)
            AND ui2.actionType IN (pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.PURCHASE, pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.ADD_TO_CART)
            AND p.id NOT IN (
                SELECT DISTINCT ui3.productId FROM UserInteraction ui3 
                WHERE ui3.userId = :userId AND ui3.actionType IN (pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.PURCHASE, pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.ADD_TO_CART)
            )
            GROUP BY p.id, p.name, p.price
            HAVING COUNT(*) >= 2
            ORDER BY AVG(ui2.interactionScore) DESC
            """)
    List<ProductRecommendation> findCollaborativeRecommendations(
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    // IA: Filtrado basado en contenido - CAST para SUM
    @Query("""
            SELECT new pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation(
                p2.id,
                p2.name,
                p2.price,
                CAST(SUM(ui.interactionScore) AS double),
                'Basado en tus compras anteriores'
            )
            FROM UserInteraction ui
            JOIN Product p1 ON ui.productId = p1.id
            JOIN ProductCategory pc1 ON p1.id = pc1.productId
            JOIN ProductCategory pc2 ON pc1.categoryId = pc2.categoryId
            JOIN Product p2 ON pc2.productId = p2.id
            WHERE ui.userId = :userId 
            AND p2.id != p1.id
            AND p2.id NOT IN (
                SELECT DISTINCT ui2.productId FROM UserInteraction ui2 
                WHERE ui2.userId = :userId AND ui2.actionType IN (pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.PURCHASE, pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.ADD_TO_CART)
            )
            GROUP BY p2.id, p2.name, p2.price
            ORDER BY SUM(ui.interactionScore) DESC
            """)
    List<ProductRecommendation> findContentBasedRecommendations(
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    // IA: Productores locales - COUNT retorna Long, usar constructor apropiado
    @Query("""
            SELECT new pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation(
                p2.id,
                p2.name,
                p2.price,
                COUNT(ui.id),
                'De productores locales en tu área'
            )
            FROM UserInteraction ui
            JOIN Product p1 ON ui.productId = p1.id
            JOIN Producer pr1 ON p1.producerId = pr1.id
            JOIN Producer pr2 ON pr1.location = pr2.location
            JOIN Product p2 ON pr2.id = p2.producerId
            WHERE ui.userId = :userId 
            AND p2.id != p1.id
            AND pr2.approved = true
            AND p2.id NOT IN (
                SELECT DISTINCT ui2.productId FROM UserInteraction ui2 
                WHERE ui2.userId = :userId AND ui2.actionType IN (pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.PURCHASE, pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.ADD_TO_CART)
            )
            GROUP BY p2.id, p2.name, p2.price
            ORDER BY COUNT(ui.id) DESC
            """)
    List<ProductRecommendation> findLocalProducerRecommendations(
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    // IA: Productos populares - AVG puede ser null, usar COALESCE
    @Query("""
            SELECT new pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation(
                p.id,
                p.name,
                p.price,
                COALESCE(CAST(AVG(ui.interactionScore) AS double), 0.0),
                'Productos populares'
            )
            FROM Product p
            JOIN Producer pr ON p.producerId = pr.id
            LEFT JOIN UserInteraction ui ON p.id = ui.productId
            WHERE pr.approved = true
            GROUP BY p.id, p.name, p.price
            HAVING COUNT(ui.id) > 0
            ORDER BY COUNT(ui.id) DESC, AVG(ui.interactionScore) DESC
            """)
    List<ProductRecommendation> findPopularRecommendations(
            org.springframework.data.domain.Pageable pageable);

    // Análisis de perfil
    @Query("""
            SELECT c.name
            FROM UserInteraction ui
            JOIN Product p ON ui.productId = p.id
            JOIN ProductCategory pc ON p.id = pc.productId
            JOIN Category c ON pc.categoryId = c.id
            WHERE ui.userId = :userId AND ui.actionType IN (pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.PURCHASE, pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.ADD_TO_CART)
            GROUP BY c.name
            ORDER BY COUNT(*) DESC
            """)
    List<String> findPreferredCategories(@Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT AVG(p.price)
            FROM UserInteraction ui
            JOIN Product p ON ui.productId = p.id
            WHERE ui.userId = :userId AND ui.actionType = pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.PURCHASE
            """)
    Double findAverageSpending(@Param("userId") Long userId);

    // Buscar usuarios similares
    @Query("""
            SELECT DISTINCT ui2.userId
            FROM UserInteraction ui1
            JOIN UserInteraction ui2 ON ui1.productId = ui2.productId
            WHERE ui1.userId = :userId AND ui2.userId != :userId
            AND ui1.actionType IN (pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.PURCHASE, pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.ADD_TO_CART)
            AND ui2.actionType IN (pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.PURCHASE, pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType.ADD_TO_CART)
            GROUP BY ui2.userId
            HAVING COUNT(DISTINCT ui1.productId) >= 2
            """)
    List<Long> findSimilarUsers(@Param("userId") Long userId);
}
