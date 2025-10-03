package com.searchlight.infra.embeddings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchlight.domain.ports.EmbeddingProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP-based embedding provider that calls an external embedding service.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "searchlight.embedding.provider", havingValue = "http", matchIfMissing = true)
public class HttpEmbeddingProvider implements EmbeddingProvider {
    
    private final String embeddingUrl;
    private final int dimension;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Timer embeddingTimer;
    
    public HttpEmbeddingProvider(
            @Value("${searchlight.embedding.url:http://localhost:8000/embed}") String embeddingUrl,
            @Value("${searchlight.embedding.dimension:384}") int dimension,
            @Value("${searchlight.embedding.timeout:30000}") int timeoutMs,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        this.embeddingUrl = embeddingUrl;
        this.dimension = dimension;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
        this.embeddingTimer = meterRegistry.timer("embedding.latency", "provider", "http");
        
        log.info("Initialized HTTP embedding provider: url={}, dimension={}", embeddingUrl, dimension);
    }
    
    @Override
    public float[] embed(String text) {
        return embeddingTimer.record(() -> {
            try {
                Map<String, String> requestBody = Map.of("text", text);
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(embeddingUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    log.error("Embedding service returned status {}: {}", response.statusCode(), response.body());
                    throw new RuntimeException("Embedding service error: " + response.statusCode());
                }
                
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
                List<Double> embedding = (List<Double>) responseBody.get("embedding");
                
                if (embedding == null || embedding.size() != dimension) {
                    throw new RuntimeException("Invalid embedding response: expected dimension " + dimension);
                }
                
                float[] result = new float[dimension];
                for (int i = 0; i < dimension; i++) {
                    result[i] = embedding.get(i).floatValue();
                }
                
                return result;
                
            } catch (IOException | InterruptedException e) {
                log.error("Failed to get embedding for text", e);
                throw new RuntimeException("Embedding failed", e);
            }
        });
    }
    
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        return embeddingTimer.record(() -> {
            try {
                Map<String, List<String>> requestBody = Map.of("texts", texts);
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(embeddingUrl + "/batch"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    log.warn("Batch embedding failed, falling back to individual embeds");
                    // Fallback to individual embeds
                    List<float[]> results = new ArrayList<>();
                    for (String text : texts) {
                        results.add(embed(text));
                    }
                    return results;
                }
                
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
                List<List<Double>> embeddings = (List<List<Double>>) responseBody.get("embeddings");
                
                List<float[]> results = new ArrayList<>();
                for (List<Double> embedding : embeddings) {
                    float[] result = new float[dimension];
                    for (int i = 0; i < dimension; i++) {
                        result[i] = embedding.get(i).floatValue();
                    }
                    results.add(result);
                }
                
                return results;
                
            } catch (IOException | InterruptedException e) {
                log.error("Batch embedding failed", e);
                // Fallback to individual embeds
                List<float[]> results = new ArrayList<>();
                for (String text : texts) {
                    results.add(embed(text));
                }
                return results;
            }
        });
    }
    
    @Override
    public int getDimension() {
        return dimension;
    }
    
    @Override
    public String getProviderName() {
        return "http";
    }
}
