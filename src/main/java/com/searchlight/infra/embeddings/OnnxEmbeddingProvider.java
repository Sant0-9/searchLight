package com.searchlight.infra.embeddings;

import com.searchlight.domain.ports.EmbeddingProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ONNX Runtime-based embedding provider (stub implementation).
 * TODO: Load actual MiniLM-L6-v2 ONNX model and implement proper inference.
 * For now, returns deterministic random vectors for offline testing.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "searchlight.embedding.provider", havingValue = "onnx")
public class OnnxEmbeddingProvider implements EmbeddingProvider {
    
    private final int dimension;
    private final Timer embeddingTimer;
    private final boolean stubMode;
    
    public OnnxEmbeddingProvider(
            @Value("${searchlight.embedding.dimension:384}") int dimension,
            @Value("${searchlight.embedding.onnx.model-path:}") String modelPath,
            @Value("${searchlight.embedding.onnx.stub-mode:true}") boolean stubMode,
            MeterRegistry meterRegistry) {
        this.dimension = dimension;
        this.stubMode = stubMode;
        this.embeddingTimer = meterRegistry.timer("embedding.latency", "provider", "onnx");
        
        if (stubMode) {
            log.warn("ONNX provider running in STUB MODE - returning deterministic random vectors");
            log.warn("To use real ONNX model, set searchlight.embedding.onnx.model-path and stub-mode=false");
        } else {
            // TODO: Load ONNX model
            log.error("Real ONNX mode not yet implemented. Model path: {}", modelPath);
            throw new UnsupportedOperationException("Real ONNX inference not yet implemented");
        }
        
        log.info("Initialized ONNX embedding provider: dimension={}, stubMode={}", dimension, stubMode);
    }
    
    @Override
    public float[] embed(String text) {
        return embeddingTimer.record(() -> {
            if (stubMode) {
                return generateDeterministicVector(text);
            } else {
                // TODO: Implement actual ONNX inference
                // 1. Tokenize text
                // 2. Run through ONNX model
                // 3. Pool embeddings (mean pooling)
                // 4. Normalize vector
                throw new UnsupportedOperationException("Real ONNX inference not yet implemented");
            }
        });
    }
    
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }
    
    @Override
    public int getDimension() {
        return dimension;
    }
    
    @Override
    public String getProviderName() {
        return stubMode ? "onnx-stub" : "onnx";
    }
    
    /**
     * Generate a deterministic vector based on text hash.
     * This ensures tests are reproducible while simulating embeddings.
     */
    private float[] generateDeterministicVector(String text) {
        Random random = new Random(text.hashCode());
        float[] vector = new float[dimension];
        
        // Generate random vector
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) (random.nextGaussian() * 0.1);
        }
        
        // Normalize to unit length (for cosine similarity)
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < dimension; i++) {
                vector[i] /= norm;
            }
        }
        
        return vector;
    }
}
