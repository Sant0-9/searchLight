package com.searchlight.fixtures;

import com.searchlight.domain.model.DocumentChunk;

import java.time.Instant;
import java.util.List;

/**
 * Sample documents for testing.
 */
public class SampleDocs {
    
    public static DocumentChunk createSampleChunk(String id, String content, float[] vector) {
        return DocumentChunk.builder()
                .id(id)
                .sourceId("source-" + id)
                .title("Sample Document " + id)
                .url("https://example.com/doc/" + id)
                .content(content)
                .vector(vector)
                .keywords(content.split("\\s+"))
                .timestamp(Instant.now())
                .source("test")
                .chunkIndex(0)
                .contentHash(Integer.toString(content.hashCode()))
                .build();
    }
    
    public static float[] createNormalizedVector(int dimension, int seed) {
        float[] vector = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) Math.sin(seed + i);
        }
        
        // Normalize
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        for (int i = 0; i < dimension; i++) {
            vector[i] /= norm;
        }
        
        return vector;
    }
    
    public static List<DocumentChunk> createSampleDocuments(int dimension) {
        return List.of(
                createSampleChunk("1", 
                        "Machine learning is a subset of artificial intelligence that focuses on algorithms.", 
                        createNormalizedVector(dimension, 1)),
                createSampleChunk("2", 
                        "Deep learning uses neural networks with multiple layers for pattern recognition.", 
                        createNormalizedVector(dimension, 2)),
                createSampleChunk("3", 
                        "Natural language processing enables computers to understand human language.", 
                        createNormalizedVector(dimension, 3)),
                createSampleChunk("4", 
                        "Computer vision allows machines to interpret and understand visual information.", 
                        createNormalizedVector(dimension, 4)),
                createSampleChunk("5", 
                        "Reinforcement learning trains agents through trial and error with rewards.", 
                        createNormalizedVector(dimension, 5))
        );
    }
}
