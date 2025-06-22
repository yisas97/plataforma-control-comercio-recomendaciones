package pe.com.prueba.plataformacontrolcomerciorecomendaciones.repository;

import org.springframework.data.domain.Pageable;
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
    List<UserInteraction> findByUserIdAndActionType(Long userId, InteractionType actionType);
    List<UserInteraction> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

    // 🔥 QUERY 1: Productos más interactuados por el usuario (7 parámetros)
    @Query("""
        SELECT new pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation(
            p.id,
            p.name,
            p.description,
            p.price,
            pr.businessName,
            COALESCE(c.name, 'Sin categoría'),
            SUM(ui.interactionScore),
            'Basado en tu actividad reciente'
        )
        FROM UserInteraction ui
        JOIN Product p ON ui.productId = p.id
        JOIN Producer pr ON p.producerId = pr.id
        LEFT JOIN ProductCategory pc ON p.id = pc.productId
        LEFT JOIN Category c ON pc.categoryId = c.id
        WHERE ui.userId = :userId
        AND p.quantity > 0
        AND pr.approved = true
        GROUP BY p.id, p.name, p.description, p.price, pr.businessName, c.name
        ORDER BY SUM(ui.interactionScore) DESC, COUNT(ui.id) DESC
        """)
    List<ProductRecommendation> findUserMostInteractedProducts(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT new pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation(
            p.id, 
            p.name, 
            p.description,
            p.price, 
            pr.businessName,
            COALESCE(c.name, 'Sin categoría'),
            AVG(ui_other.interactionScore),
            'Usuarios con gustos similares también compraron esto'
        )
        FROM UserInteraction ui_user
        JOIN UserInteraction ui_other ON ui_user.productId = ui_other.productId
        JOIN UserInteraction ui_other_prods ON ui_other.userId = ui_other_prods.userId
        JOIN Product p ON ui_other_prods.productId = p.id
        JOIN Producer pr ON p.producerId = pr.id
        LEFT JOIN ProductCategory pc ON p.id = pc.productId
        LEFT JOIN Category c ON pc.categoryId = c.id
        WHERE ui_user.userId = :userId 
        AND ui_other.userId != :userId
        AND ui_user.actionType IN ('PURCHASE', 'ADD_TO_CART')
        AND ui_other.actionType IN ('PURCHASE', 'ADD_TO_CART')
        AND ui_other_prods.actionType IN ('PURCHASE', 'ADD_TO_CART')
        AND p.id NOT IN (
            SELECT DISTINCT ui_exclude.productId FROM UserInteraction ui_exclude 
            WHERE ui_exclude.userId = :userId 
            AND ui_exclude.actionType IN ('PURCHASE', 'ADD_TO_CART')
        )
        AND p.quantity > 0
        AND pr.approved = true
        GROUP BY p.id, p.name, p.description, p.price, pr.businessName, c.name
        HAVING COUNT(DISTINCT ui_other.userId) >= 1
        ORDER BY AVG(ui_other.interactionScore) DESC, COUNT(ui_other_prods.id) DESC
        """)
    List<ProductRecommendation> findCollaborativeRecommendations(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT new pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation(
            p2.id,
            p2.name,
            p2.description,
            p2.price,
            pr2.businessName,
            COALESCE(c2.name, 'Sin categoría'),
            SUM(ui.interactionScore),
            'Basado en tus categorías favoritas'
        )
        FROM UserInteraction ui
        JOIN Product p1 ON ui.productId = p1.id
        JOIN ProductCategory pc1 ON p1.id = pc1.productId
        JOIN ProductCategory pc2 ON pc1.categoryId = pc2.categoryId
        JOIN Product p2 ON pc2.productId = p2.id
        JOIN Producer pr2 ON p2.producerId = pr2.id
        LEFT JOIN Category c2 ON pc2.categoryId = c2.id
        WHERE ui.userId = :userId 
        AND p2.id != p1.id
        AND p2.id NOT IN (
            SELECT DISTINCT ui2.productId FROM UserInteraction ui2 
            WHERE ui2.userId = :userId
        )
        AND p2.quantity > 0
        AND pr2.approved = true
        GROUP BY p2.id, p2.name, p2.description, p2.price, pr2.businessName, c2.name
        ORDER BY SUM(ui.interactionScore) DESC
        """)
    List<ProductRecommendation> findContentBasedRecommendations(@Param("userId") Long userId, Pageable pageable);

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
        AND p2.quantity > 0
        AND p2.id NOT IN (
            SELECT DISTINCT ui2.productId FROM UserInteraction ui2 
            WHERE ui2.userId = :userId AND ui2.actionType IN ('PURCHASE', 'ADD_TO_CART')
        )
        GROUP BY p2.id, p2.name, p2.price
        ORDER BY COUNT(ui.id) DESC
        """)
    List<ProductRecommendation> findLocalProducerRecommendations(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT new pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation(
            p.id,
            p.name,
            p.description,
            p.price,
            pr.businessName,
            COALESCE(c.name, 'Sin categoría'),
            (COUNT(ui.id) * 0.3 + COALESCE(AVG(ui.interactionScore), 0.0) * 0.7),
            'Producto popular'
        )
        FROM Product p
        JOIN Producer pr ON p.producerId = pr.id
        LEFT JOIN ProductCategory pc ON p.id = pc.productId
        LEFT JOIN Category c ON pc.categoryId = c.id
        LEFT JOIN UserInteraction ui ON p.id = ui.productId
        WHERE pr.approved = true
        AND p.quantity > 0
        GROUP BY p.id, p.name, p.description, p.price, pr.businessName, c.name
        HAVING COUNT(ui.id) > 0
        ORDER BY (COUNT(ui.id) * 0.3 + COALESCE(AVG(ui.interactionScore), 0.0) * 0.7) DESC, COUNT(ui.id) DESC
        """)
    List<ProductRecommendation> findPopularRecommendations(Pageable pageable);

    // Queries para estadísticas (mantener)
    @Query("SELECT COUNT(ui) FROM UserInteraction ui WHERE ui.userId = :userId AND ui.actionType = :actionType")
    Long countInteractionsByUserAndType(@Param("userId") Long userId, @Param("actionType") InteractionType actionType);

    @Query("""
        SELECT c.name FROM UserInteraction ui
        JOIN Product p ON ui.productId = p.id
        JOIN ProductCategory pc ON p.id = pc.productId
        JOIN Category c ON pc.categoryId = c.id
        WHERE ui.userId = :userId
        GROUP BY c.name
        ORDER BY COUNT(ui.id) DESC
        """)
    List<String> findTopCategoriesByUser(@Param("userId") Long userId, Pageable pageable);

    // Otras queries útiles (mantener)
    @Query("""
        SELECT AVG(p.price)
        FROM UserInteraction ui
        JOIN Product p ON ui.productId = p.id
        WHERE ui.userId = :userId AND ui.actionType = 'PURCHASE'
        """)
    Double findAverageSpending(@Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT ui2.userId
        FROM UserInteraction ui1
        JOIN UserInteraction ui2 ON ui1.productId = ui2.productId
        WHERE ui1.userId = :userId AND ui2.userId != :userId
        AND ui1.actionType IN ('PURCHASE', 'ADD_TO_CART')
        AND ui2.actionType IN ('PURCHASE', 'ADD_TO_CART')
        GROUP BY ui2.userId
        HAVING COUNT(DISTINCT ui1.productId) >= 2
        """)
    List<Long> findSimilarUsers(@Param("userId") Long userId);

}
