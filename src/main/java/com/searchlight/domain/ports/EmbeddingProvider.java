package com.searchlight.domain.ports;

import java.util.List;

/**
 * Port for embedding text into vector representations.
 * Implementations can use HTTP services, ONNX models, or other providers.
 */
public interface EmbeddingProvider {
    
    /**
     * Embed a single text into a vector.
     */
    float[] embed(String text);
    
    /**
     * Batch embed multiple texts (more efficient for some providers).
     */
    List<float[]> embedBatch(List<String> texts);
    
    /**
     * Get the dimension of the embedding vectors.
     */
    int getDimension();
    
    /**
     * Get the name/type of this provider.
     */
    String getProviderName();
}
