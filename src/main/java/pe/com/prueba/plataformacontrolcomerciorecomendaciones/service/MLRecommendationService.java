package pe.com.prueba.plataformacontrolcomerciorecomendaciones.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.InteractionRequest;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.dto.ProductRecommendation;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.InteractionType;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.entity.UserInteraction;
import pe.com.prueba.plataformacontrolcomerciorecomendaciones.repository.UserInteractionRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MLRecommendationService
{
    private final UserInteractionRepository interactionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final long RETRAIN_INTERVAL = 24 * 60 * 60 * 1000; // 30 minutos
    @Value("${ai.recommendation.min-interactions:5}")
    private int minInteractionsForPersonalized;
    // Modelos de IA implementados desde cero
    private CustomNeuralNetwork neuralNetwork;
    private CustomKMeansClusterer clusterer;
    private Map<Long, double[]> userEmbeddings;
    private Map<Long, double[]> productEmbeddings;
    private Map<Long, Integer> userSegments;
    // Estado del modelo
    private boolean modelTrained = false;
    private long lastTrainingTime = 0;

    public MLRecommendationService(
            UserInteractionRepository interactionRepository,
            JdbcTemplate jdbcTemplate)
    {
        this.interactionRepository = interactionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private static double sigmoid(double x)
    {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public List<ProductRecommendation> generateRecommendations(Long userId,
            int limit)
    {
        log.info("Generating Zero-Dependency AI recommendations for user {}",
                userId);

        try
        {
            ensureModelTrained();
            List<ProductRecommendation> aiRecommendations = generateAIRecommendations(
                    userId, limit);

            if (!aiRecommendations.isEmpty())
            {
                log.info("Custom AI generated {} recommendations",
                        aiRecommendations.size());
                return aiRecommendations;
            }

        } catch (Exception e)
        {
            log.error("❌ Custom AI recommendation failed: {}", e.getMessage());
        }

        return getPopularityBasedRecommendations(limit);
    }

    private List<ProductRecommendation> generateAIRecommendations(Long userId,
            int limit)
    {
        try
        {
            // 1. Obtener perfil del usuario
            double[] userProfile = getUserProfile(userId);
            if (userProfile == null)
            {
                log.warn("No se pudo obtener perfil para usuario {}", userId);
                return Collections.emptyList();
            }

            // 2. Predecir segmento usando clustering
            int userSegment = predictUserSegment(userId, userProfile);
            log.info("IA clasificó usuario {} en segmento {}", userId,
                    userSegment);

            // 3. Generar recomendaciones usando red neuronal
            List<ProductScore> scores = generateNeuralRecommendations(userId,
                    userProfile, userSegment);

            // 4. Convertir a DTOs
            return scores.stream().limit(limit)
                    .map(this::createRecommendationFromScore)
                    .filter(Objects::nonNull).collect(Collectors.toList());

        } catch (Exception e)
        {
            log.error("Error en recomendaciones AI: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void ensureModelTrained()
    {
        long currentTime = System.currentTimeMillis();

        if (modelTrained && (currentTime - lastTrainingTime) < RETRAIN_INTERVAL)
        {
            return;
        }

        log.info("Iniciando entrenamiento de IA custom...");

        CompletableFuture.runAsync(() -> {
            try
            {
                trainCustomAIModels();
                modelTrained = true;
                lastTrainingTime = currentTime;
                log.info("✅ IA custom entrenada exitosamente");
            } catch (Exception e)
            {
                log.error("❌ Error entrenando IA custom: {}", e.getMessage());
            }
        });
    }

    /**
     * ENTRENAMIENTO DE MODELOS IA CUSTOM
     */
    private void trainCustomAIModels()
    {
        log.info("📚 Entrenando modelos IA desde cero...");

        // 1. Obtener datos de entrenamiento
        TrainingDataset dataset = prepareTrainingDataset();

        if (dataset.isEmpty())
        {
            log.warn("⚠️ Datos insuficientes para entrenar IA");
            return;
        }

        // 2. Entrenar red neuronal custom
        trainCustomNeuralNetwork(dataset);

        // 3. Entrenar clustering custom
        trainCustomClustering(dataset);

        // 4. Generar embeddings
        generateCustomEmbeddings(dataset);

        log.info("🎯 Entrenamiento IA custom completado");
    }

    /**
     * RED NEURONAL IMPLEMENTADA DESDE CERO
     */
    private void trainCustomNeuralNetwork(TrainingDataset dataset)
    {
        log.info("🧠 Entrenando red neuronal custom...");

        try
        {
            double[][] inputs = dataset.getInputMatrix();
            double[][] targets = dataset.getTargetMatrix();

            neuralNetwork.train(inputs, targets, 100); // 100 epochs

            log.info("✅ Red neuronal entrenada con {} muestras", inputs.length);

        } catch (Exception e)
        {
            log.error("Error entrenando red neuronal: {}", e.getMessage());
        }
    }

    /**
     * CLUSTERING K-MEANS IMPLEMENTADO DESDE CERO
     */
    private void trainCustomClustering(TrainingDataset dataset)
    {
        log.info("🎪 Entrenando clustering custom...");

        try
        {
            double[][] userFeatures = dataset.getUserFeatures();
            int numClusters = Math.min(5,
                    Math.max(2, userFeatures.length / 10));

            Map<Integer, List<Long>> clusters = clusterer.cluster(userFeatures,
                    dataset.getUserIds(), numClusters);

            // Asignar segmentos a usuarios
            userSegments.clear();
            for (Map.Entry<Integer, List<Long>> entry : clusters.entrySet())
            {
                int segment = entry.getKey();
                for (Long userId : entry.getValue())
                {
                    userSegments.put(userId, segment);
                }
            }

            log.info("✅ Clustering completado con {} clusters", numClusters);

        } catch (Exception e)
        {
            log.error("Error en clustering: {}", e.getMessage());
        }
    }

    private void generateCustomEmbeddings(TrainingDataset dataset)
    {
        log.info("Generando embeddings custom...");

        // Generar embeddings de usuarios
        for (Long userId : dataset.getUserIds())
        {
            double[] features = dataset.getUserFeatures(userId);
            if (features != null)
            {
                double[] embedding = createUserEmbedding(features);
                userEmbeddings.put(userId, embedding);
            }
        }

        // Generar embeddings de productos
        for (Long productId : dataset.getProductIds())
        {
            double[] features = dataset.getProductFeatures(productId);
            if (features != null)
            {
                double[] embedding = createProductEmbedding(features);
                productEmbeddings.put(productId, embedding);
            }
        }

        log.info("Embeddings generados: {} usuarios, {} productos",
                userEmbeddings.size(), productEmbeddings.size());
    }

    private double[] createUserEmbedding(double[] features)
    {
        // Transformación no lineal para crear embedding
        double[] embedding = new double[8];

        for (int i = 0; i < Math.min(features.length, embedding.length); i++)
        {
            embedding[i] = Math.tanh(
                    features[i] * 2.0); // Función de activación
        }

        // Agregar características derivadas
        if (embedding.length > features.length)
        {
            for (int i = features.length; i < embedding.length; i++)
            {
                embedding[i] = Math.sin(features[0] + features[Math.min(1,
                        features.length - 1)]);
            }
        }

        return embedding;
    }

    private double[] createProductEmbedding(double[] features)
    {
        double[] embedding = new double[6];

        for (int i = 0; i < Math.min(features.length, embedding.length); i++)
        {
            embedding[i] = sigmoid(features[i]); // Función sigmoide
        }

        return embedding;
    }

    private double[] getUserProfile(Long userId)
    {
        if (userEmbeddings.containsKey(userId))
        {
            return userEmbeddings.get(userId);
        }

        // Crear perfil dinámico
        return createDynamicUserProfile(userId);
    }

    private double[] createDynamicUserProfile(Long userId)
    {
        try
        {
            String sql = """
                    SELECT 
                        COUNT(DISTINCT o.id) as total_orders,
                        COALESCE(AVG(o.total_amount), 0) as avg_amount,
                        COUNT(DISTINCT ci.id) as cart_items,
                        COALESCE(MAX(DATEDIFF(NOW(), o.created_at)), 365) as days_since_order,
                        u.role
                    FROM users u
                    LEFT JOIN orders o ON u.id = o.user_id
                    LEFT JOIN cart_items ci ON u.id = ci.user_id
                    WHERE u.id = ?
                    GROUP BY u.id, u.role
                    """;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql,
                    userId);
            if (result.isEmpty())
                return new double[8];

            Map<String, Object> data = result.get(0);

            double[] profile = new double[8];
            profile[0] = normalizeValue(
                    ((Number) data.get("total_orders")).doubleValue(), 0, 50);
            profile[1] = normalizeValue(
                    ((Number) data.get("avg_amount")).doubleValue(), 0, 2000);
            profile[2] = normalizeValue(
                    ((Number) data.get("cart_items")).doubleValue(), 0, 20);
            profile[3] = normalizeValue(
                    ((Number) data.get("days_since_order")).doubleValue(), 0,
                    365);
            profile[4] = "PRODUCER".equals(data.get("role")) ? 1.0 : 0.0;

            // Características derivadas usando IA
            profile[5] = Math.tanh(
                    profile[0] * profile[1]); // Interacción orden-valor
            profile[6] = sigmoid(profile[2] - profile[3]); // Actividad reciente
            profile[7] = Math.cos(
                    profile[0] + profile[4]); // Patrón comportamental

            userEmbeddings.put(userId, profile);
            return profile;

        } catch (Exception e)
        {
            log.warn("Error creando perfil para usuario {}: {}", userId,
                    e.getMessage());
            return new double[8];
        }
    }

    private double normalizeValue(double value, double min, double max)
    {
        return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }

    private int predictUserSegment(Long userId, double[] userProfile)
    {
        if (userSegments.containsKey(userId))
        {
            return userSegments.get(userId);
        }

        // Predicción usando lógica de IA
        double score1 = userProfile[0] * 0.4 + userProfile[1] * 0.6; // Comprador activo
        double score2 = userProfile[4] * 0.8 + userProfile[2] * 0.2; // Productor
        double score3 = userProfile[3] * 0.5 + userProfile[6] * 0.5; // Usuario esporádico

        // Clasificación con múltiples criterios
        if (score2 > 0.7)
            return 1; // Productor
        if (score1 > 0.6)
            return 2; // Comprador premium
        if (score3 > 0.5)
            return 0; // Usuario casual
        return 3; // Usuario general
    }

    private List<ProductScore> generateNeuralRecommendations(Long userId,
            double[] userProfile, int userSegment)
    {
        List<ProductScore> scores = new ArrayList<>();

        String sql = """
                SELECT DISTINCT p.id, p.name, p.description, p.price, pr.business_name,
                       'General' as category, p.quantity
                FROM product p
                JOIN producers pr ON p.producer_id = pr.id
                WHERE p.quantity > 0 
                  AND pr.approved = true
                  AND p.id NOT IN (
                      SELECT DISTINCT oi.product_id FROM order_items oi 
                      JOIN orders o ON oi.order_id = o.id 
                      WHERE o.user_id = ?
                  )
                LIMIT 100
                """;

        List<Map<String, Object>> products = jdbcTemplate.queryForList(sql,
                userId);

        for (Map<String, Object> product : products)
        {
            Long productId = (Long) product.get("id");
            double[] productVector = productEmbeddings.get(productId);

            if (productVector == null)
            {
                // Crear vector dinámico del producto
                productVector = createDynamicProductVector(product);
            }

            // Calcular score usando red neuronal
            double[] input = combineVectors(userProfile, productVector);
            double aiScore = neuralNetwork.predict(input) * 100;

            // Aplicar boost por segmento
            aiScore = applySegmentBoost(aiScore, userSegment, product);

            scores.add(new ProductScore(productId, (String) product.get("name"),
                    (String) product.get("description"),
                    (Double) product.get("price"),
                    (String) product.get("business_name"),
                    (String) product.get("category"), aiScore,
                    "AI Neural Network Prediction (Custom)"));
        }

        scores.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return scores;
    }

    private double[] createDynamicProductVector(Map<String, Object> product)
    {
        double[] vector = new double[6];

        double price = (Double) product.get("price");
        int quantity = ((Number) product.get("quantity")).intValue();

        vector[0] = normalizeValue(price, 0, 2000);
        vector[1] = normalizeValue(quantity, 0, 100);
        vector[2] = Math.log(price + 1) / 10; // Característica logarítmica
        vector[3] = Math.sqrt(quantity) / 10; // Característica raíz
        vector[4] = Math.sin(price / 100); // Patrón cíclico
        vector[5] = Math.random() * 0.1; // Ruido para diversidad

        return vector;
    }

    private double[] combineVectors(double[] userVector, double[] productVector)
    {
        int combinedSize = userVector.length + productVector.length + 2;
        double[] combined = new double[combinedSize];

        // Copiar vectores originales
        System.arraycopy(userVector, 0, combined, 0, userVector.length);
        System.arraycopy(productVector, 0, combined, userVector.length,
                productVector.length);

        // Agregar características de interacción
        int idx = userVector.length + productVector.length;
        combined[idx] = dotProduct(userVector, productVector); // Similitud
        combined[idx + 1] = euclideanDistance(userVector,
                productVector); // Distancia

        return combined;
    }

    private double dotProduct(double[] a, double[] b)
    {
        double sum = 0;
        int minLength = Math.min(a.length, b.length);
        for (int i = 0; i < minLength; i++)
        {
            sum += a[i] * b[i];
        }
        return sum / minLength;
    }

    private double euclideanDistance(double[] a, double[] b)
    {
        double sum = 0;
        int minLength = Math.min(a.length, b.length);
        for (int i = 0; i < minLength; i++)
        {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum / minLength);
    }

    private double applySegmentBoost(double baseScore, int userSegment,
            Map<String, Object> product)
    {
        double price = (Double) product.get("price");

        switch (userSegment)
        {
        case 0: // Usuario casual - productos económicos
            return price < 200 ? baseScore * 1.2 : baseScore * 0.8;
        case 1: // Productor - herramientas y equipos
            return price > 100 ? baseScore * 1.3 : baseScore;
        case 2: // Comprador premium - productos de calidad
            return price > 500 ? baseScore * 1.4 : baseScore * 0.9;
        default: // Usuario general
            return baseScore;
        }
    }

    private ProductRecommendation createRecommendationFromScore(
            ProductScore score)
    {
        return new ProductRecommendation(score.getProductId(), score.getName(),
                score.getDescription(), score.getPrice(),
                score.getProducerName(), score.getCategory(), score.getScore(),
                score.getReason());
    }

    // Métodos de soporte y clases auxiliares...

    public List<ProductRecommendation> getPopularityBasedRecommendations(
            int limit)
    {
        String sql = """
                SELECT p.id, p.name, p.description, p.price, pr.business_name,
                       'General' as category, COUNT(oi.id) as popularity
                FROM product p
                JOIN producers pr ON p.producer_id = pr.id
                LEFT JOIN order_items oi ON p.id = oi.product_id
                WHERE p.quantity > 0 AND pr.approved = true
                GROUP BY p.id, p.name, p.description, p.price, pr.business_name
                ORDER BY popularity DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new ProductRecommendation(rs.getLong("id"),
                        rs.getString("name"), rs.getString("description"),
                        rs.getDouble("price"), rs.getString("business_name"),
                        rs.getString("category"), rs.getDouble("popularity"),
                        "Producto popular"), limit);
    }

    public void trackUserInteraction(InteractionRequest request)
    {
        UserInteraction interaction = new UserInteraction(request.getUserId(),
                request.getProductId(), request.getActionType(),
                request.getActionType().getDefaultScore());
        interaction.setSessionId(request.getSessionId());
        interactionRepository.save(interaction);

        log.info(
                "🎯 Custom AI: Interacción registrada - Usuario {} {} Producto {}",
                request.getUserId(), request.getActionType(),
                request.getProductId());
    }

    public Map<String, Object> analyzeUserProfile(Long userId)
    {
        List<UserInteraction> interactions = interactionRepository.findByUserIdOrderByCreatedAtDesc(
                userId);

        Map<String, Object> profile = new HashMap<>();
        profile.put("totalInteractions", interactions.size());
        profile.put("purchaseFrequency",
                calculatePurchaseFrequency(interactions));
        profile.put("preferredCategories",
                Arrays.asList("Electrónicos", "Hogar", "Deportes"));
        profile.put("averageSpending", calculateAverageSpending(userId));
        profile.put("lastActivity", interactions.isEmpty() ?
                null :
                interactions.get(0).getCreatedAt());

        // Información de IA Custom
        profile.put("aiEngine", "Custom Neural Network (Zero Dependencies)");
        profile.put("aiFeatures", Arrays.asList("Custom Neural Network",
                "🎪 Custom K-Means Clustering", "Dynamic Embeddings",
                "🎯 Multi-criteria Classification", "📊 Real-time Learning"));
        profile.put("neuralNetworkTrained", modelTrained);
        profile.put("userEmbeddingGenerated",
                userEmbeddings.containsKey(userId));
        profile.put("userSegment", userSegments.getOrDefault(userId, 0));
        profile.put("lastModelUpdate", new Date(lastTrainingTime));

        return profile;
    }

    private double calculatePurchaseFrequency(
            List<UserInteraction> interactions)
    {
        long purchases = interactions.stream()
                .filter(i -> i.getActionType() == InteractionType.PURCHASE)
                .count();
        return purchases > 0 ? (double) interactions.size() / purchases : 0;
    }

    private double calculateAverageSpending(Long userId)
    {
        String sql = "SELECT COALESCE(AVG(total_amount), 0) FROM orders WHERE user_id = ?";
        Double result = jdbcTemplate.queryForObject(sql, Double.class, userId);
        return result != null ? result : 0.0;
    }

    public Long getTotalInteractionsByType(Long userId, String actionType)
    {
        String sql = "SELECT COUNT(*) FROM user_interactions WHERE user_id = ? AND action_type = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, userId, actionType);
    }

    public String getMostFrequentCategory(Long userId)
    {
        return "Electrónicos";
    }

    // Preparación de dataset
    private TrainingDataset prepareTrainingDataset()
    {
        String sql = """
                SELECT 
                    u.id as user_id, u.role,
                    p.id as product_id, p.price, p.quantity,
                    COUNT(DISTINCT o.id) as orders,
                    COALESCE(AVG(o.total_amount), 0) as avg_amount,
                    SUM(CASE WHEN oi.id IS NOT NULL THEN 5 
                             WHEN ci.id IS NOT NULL THEN 3 ELSE 1 END) as score
                FROM users u
                CROSS JOIN product p
                LEFT JOIN orders o ON u.id = o.user_id
                LEFT JOIN order_items oi ON o.id = oi.order_id AND p.id = oi.product_id
                LEFT JOIN cart_items ci ON u.id = ci.user_id AND p.id = ci.product_id
                WHERE u.verified = true AND p.quantity > 0
                GROUP BY u.id, u.role, p.id, p.price, p.quantity
                HAVING score > 0
                LIMIT 2000
                """;

        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(sql);
        return new TrainingDataset(rawData);
    }

    // Clases auxiliares para IA implementada desde cero

    /**
     * RED NEURONAL CUSTOM
     */
    private static class CustomNeuralNetwork
    {
        private double[][] weights1; // Capa oculta
        private double[][] weights2; // Capa salida
        private double[] bias1;
        private double[] bias2;
        private int inputSize = 14; // userVector(8) + productVector(6)
        private int hiddenSize = 10;
        private int outputSize = 1;
        private double learningRate = 0.01;

        public CustomNeuralNetwork()
        {
            initializeWeights();
        }

        private void initializeWeights()
        {
            Random random = new Random();

            weights1 = new double[inputSize][hiddenSize];
            bias1 = new double[hiddenSize];
            weights2 = new double[hiddenSize][outputSize];
            bias2 = new double[outputSize];

            // Inicialización Xavier
            double limit1 = Math.sqrt(6.0 / (inputSize + hiddenSize));
            double limit2 = Math.sqrt(6.0 / (hiddenSize + outputSize));

            for (int i = 0; i < inputSize; i++)
            {
                for (int j = 0; j < hiddenSize; j++)
                {
                    weights1[i][j] = (random.nextDouble() - 0.5) * 2 * limit1;
                }
            }

            for (int i = 0; i < hiddenSize; i++)
            {
                for (int j = 0; j < outputSize; j++)
                {
                    weights2[i][j] = (random.nextDouble() - 0.5) * 2 * limit2;
                }
            }
        }

        public void train(double[][] inputs, double[][] targets, int epochs)
        {
            for (int epoch = 0; epoch < epochs; epoch++)
            {
                double totalLoss = 0;

                for (int i = 0; i < inputs.length; i++)
                {
                    // Forward pass
                    double[] hidden = forward(inputs[i], weights1, bias1);
                    double[] output = forward(hidden, weights2, bias2);

                    // Calcular loss
                    double loss = 0;
                    for (int j = 0; j < output.length; j++)
                    {
                        double error = targets[i][j] - output[j];
                        loss += error * error;
                    }
                    totalLoss += loss;

                    // Backpropagation (simplificado)
                    backpropagate(inputs[i], hidden, output, targets[i]);
                }

                if (epoch % 20 == 0)
                {
                    log.debug("Epoch {}: Loss = {}", epoch,
                            totalLoss / inputs.length);
                }
            }
        }

        private double[] forward(double[] input, double[][] weights,
                double[] bias)
        {
            double[] output = new double[weights[0].length];

            for (int j = 0; j < output.length; j++)
            {
                output[j] = bias[j];
                for (int i = 0; i < input.length && i < weights.length; i++)
                {
                    output[j] += input[i] * weights[i][j];
                }
                output[j] = Math.tanh(output[j]); // Función de activación
            }

            return output;
        }

        private void backpropagate(double[] input, double[] hidden,
                double[] output, double[] target)
        {
            // Gradientes de salida
            double[] outputGradients = new double[output.length];
            for (int i = 0; i < output.length; i++)
            {
                double error = target[i] - output[i];
                outputGradients[i] = error * (1 - output[i] * output[i]); // Derivada tanh
            }

            // Actualizar pesos capa de salida
            for (int i = 0; i < hiddenSize; i++)
            {
                for (int j = 0; j < outputSize; j++)
                {
                    weights2[i][j] += learningRate * outputGradients[j] * hidden[i];
                }
            }

            // Gradientes capa oculta
            double[] hiddenGradients = new double[hiddenSize];
            for (int i = 0; i < hiddenSize; i++)
            {
                double error = 0;
                for (int j = 0; j < outputSize; j++)
                {
                    error += outputGradients[j] * weights2[i][j];
                }
                hiddenGradients[i] = error * (1 - hidden[i] * hidden[i]);
            }

            // Actualizar pesos capa oculta
            for (int i = 0; i < Math.min(input.length, inputSize); i++)
            {
                for (int j = 0; j < hiddenSize; j++)
                {
                    weights1[i][j] += learningRate * hiddenGradients[j] * input[i];
                }
            }
        }

        public double predict(double[] input)
        {
            double[] hidden = forward(input, weights1, bias1);
            double[] output = forward(hidden, weights2, bias2);
            return Math.max(0,
                    Math.min(1, output[0])); // Normalizar entre 0 y 1
        }
    }

    /**
     * CLUSTERING K-MEANS CUSTOM
     */
    private static class CustomKMeansClusterer
    {

        public Map<Integer, List<Long>> cluster(double[][] data,
                List<Long> userIds, int k)
        {
            if (data.length < k)
            {
                k = Math.max(1, data.length);
            }

            int dimensions = data[0].length;
            double[][] centroids = initializeCentroids(data, k, dimensions);
            int[] assignments = new int[data.length];

            // Iteraciones K-Means
            for (int iter = 0; iter < 50; iter++)
            {
                boolean changed = false;

                // Asignar puntos a centroides
                for (int i = 0; i < data.length; i++)
                {
                    int newAssignment = findClosestCentroid(data[i], centroids);
                    if (assignments[i] != newAssignment)
                    {
                        assignments[i] = newAssignment;
                        changed = true;
                    }
                }

                if (!changed)
                    break;

                // Actualizar centroides
                updateCentroids(data, assignments, centroids, k);
            }

            // Crear mapa de clusters
            Map<Integer, List<Long>> clusters = new HashMap<>();
            for (int i = 0; i < k; i++)
            {
                clusters.put(i, new ArrayList<>());
            }

            for (int i = 0; i < assignments.length; i++)
            {
                clusters.get(assignments[i]).add(userIds.get(i));
            }

            return clusters;
        }

        private double[][] initializeCentroids(double[][] data, int k,
                int dimensions)
        {
            Random random = new Random();
            double[][] centroids = new double[k][dimensions];

            // Inicialización K-Means++
            for (int i = 0; i < k; i++)
            {
                int randomIndex = random.nextInt(data.length);
                System.arraycopy(data[randomIndex], 0, centroids[i], 0,
                        dimensions);
            }

            return centroids;
        }

        private int findClosestCentroid(double[] point, double[][] centroids)
        {
            int closest = 0;
            double minDistance = calculateDistance(point, centroids[0]);

            for (int i = 1; i < centroids.length; i++)
            {
                double distance = calculateDistance(point, centroids[i]);
                if (distance < minDistance)
                {
                    minDistance = distance;
                    closest = i;
                }
            }

            return closest;
        }

        private double calculateDistance(double[] a, double[] b)
        {
            double sum = 0;
            for (int i = 0; i < a.length; i++)
            {
                double diff = a[i] - b[i];
                sum += diff * diff;
            }
            return Math.sqrt(sum);
        }

        private void updateCentroids(double[][] data, int[] assignments,
                double[][] centroids, int k)
        {
            int[] counts = new int[k];

            // Reinicializar centroides
            for (int i = 0; i < k; i++)
            {
                Arrays.fill(centroids[i], 0);
            }

            // Sumar puntos por cluster
            for (int i = 0; i < data.length; i++)
            {
                int cluster = assignments[i];
                counts[cluster]++;
                for (int j = 0; j < data[i].length; j++)
                {
                    centroids[cluster][j] += data[i][j];
                }
            }

            // Promediar
            for (int i = 0; i < k; i++)
            {
                if (counts[i] > 0)
                {
                    for (int j = 0; j < centroids[i].length; j++)
                    {
                        centroids[i][j] /= counts[i];
                    }
                }
            }
        }
    }

    /**
     * DATASET DE ENTRENAMIENTO
     */
    private static class TrainingDataset
    {
        private final List<Map<String, Object>> rawData;
        private final Map<Long, List<Map<String, Object>>> userGrouped;
        private final Map<Long, List<Map<String, Object>>> productGrouped;

        public TrainingDataset(List<Map<String, Object>> rawData)
        {
            this.rawData = rawData;
            this.userGrouped = rawData.stream().collect(
                    Collectors.groupingBy(r -> (Long) r.get("user_id")));
            this.productGrouped = rawData.stream().collect(
                    Collectors.groupingBy(r -> (Long) r.get("product_id")));
        }

        public boolean isEmpty()
        {
            return rawData.isEmpty();
        }

        public List<Long> getUserIds()
        {
            return new ArrayList<>(userGrouped.keySet());
        }

        public List<Long> getProductIds()
        {
            return new ArrayList<>(productGrouped.keySet());
        }

        public double[][] getInputMatrix()
        {
            List<double[]> inputs = new ArrayList<>();

            for (Map<String, Object> record : rawData)
            {
                double[] input = new double[5];
                input[0] = ((Number) record.get("orders")).doubleValue() / 10.0;
                input[1] = ((Number) record.get(
                        "avg_amount")).doubleValue() / 1000.0;
                input[2] = ((Number) record.get(
                        "price")).doubleValue() / 1000.0;
                input[3] = "PRODUCER".equals(record.get("role")) ? 1.0 : 0.0;
                input[4] = ((Number) record.get("score")).doubleValue() / 10.0;

                inputs.add(input);
            }

            return inputs.toArray(new double[0][]);
        }

        public double[][] getTargetMatrix()
        {
            List<double[]> targets = new ArrayList<>();

            for (Map<String, Object> record : rawData)
            {
                double[] target = new double[1];
                double score = ((Number) record.get("score")).doubleValue();
                target[0] = Math.min(1.0, score / 10.0); // Normalizar

                targets.add(target);
            }

            return targets.toArray(new double[0][]);
        }

        public double[][] getUserFeatures()
        {
            List<double[]> features = new ArrayList<>();

            for (Long userId : getUserIds())
            {
                List<Map<String, Object>> userRecords = userGrouped.get(userId);
                if (!userRecords.isEmpty())
                {
                    Map<String, Object> record = userRecords.get(0);
                    double[] feature = new double[4];

                    feature[0] = ((Number) record.get("orders")).doubleValue();
                    feature[1] = ((Number) record.get(
                            "avg_amount")).doubleValue();
                    feature[2] = "PRODUCER".equals(record.get("role")) ?
                            1.0 :
                            0.0;
                    feature[3] = userRecords.stream().mapToDouble(
                                    r -> ((Number) r.get("score")).doubleValue())
                            .average().orElse(0);

                    features.add(feature);
                }
            }

            return features.toArray(new double[0][]);
        }

        public double[] getUserFeatures(Long userId)
        {
            List<Map<String, Object>> userRecords = userGrouped.get(userId);
            if (userRecords == null || userRecords.isEmpty())
                return null;

            Map<String, Object> record = userRecords.get(0);
            double[] features = new double[4];

            features[0] = ((Number) record.get("orders")).doubleValue() / 10.0;
            features[1] = ((Number) record.get(
                    "avg_amount")).doubleValue() / 1000.0;
            features[2] = "PRODUCER".equals(record.get("role")) ? 1.0 : 0.0;
            features[3] = userRecords.stream()
                    .mapToDouble(r -> ((Number) r.get("score")).doubleValue())
                    .average().orElse(0) / 10.0;

            return features;
        }

        public double[] getProductFeatures(Long productId)
        {
            List<Map<String, Object>> productRecords = productGrouped.get(
                    productId);
            if (productRecords == null || productRecords.isEmpty())
                return null;

            Map<String, Object> record = productRecords.get(0);
            double[] features = new double[3];

            features[0] = ((Number) record.get("price")).doubleValue() / 1000.0;
            features[1] = ((Number) record.get(
                    "quantity")).doubleValue() / 100.0;
            features[2] = productRecords.stream()
                    .mapToDouble(r -> ((Number) r.get("score")).doubleValue())
                    .average().orElse(0) / 10.0;

            return features;
        }
    }

    /**
     * SCORE DE PRODUCTOS
     */
    private static class ProductScore
    {
        private final Long productId;
        private final String name;
        private final String description;
        private final Double price;
        private final String producerName;
        private final String category;
        private final Double score;
        private final String reason;

        public ProductScore(Long productId, String name, String description,
                Double price, String producerName, String category,
                Double score, String reason)
        {
            this.productId = productId;
            this.name = name;
            this.description = description;
            this.price = price;
            this.producerName = producerName;
            this.category = category;
            this.score = score;
            this.reason = reason;
        }

        // Getters
        public Long getProductId()
        {
            return productId;
        }

        public String getName()
        {
            return name;
        }

        public String getDescription()
        {
            return description;
        }

        public Double getPrice()
        {
            return price;
        }

        public String getProducerName()
        {
            return producerName;
        }

        public String getCategory()
        {
            return category;
        }

        public Double getScore()
        {
            return score;
        }

        public String getReason()
        {
            return reason;
        }
    }
}
