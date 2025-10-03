package com.searchlight.infra.embeddings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class HttpEmbeddingProviderTest {
    
    private WireMockServer wireMockServer;
    private HttpEmbeddingProvider provider;
    
    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        
        provider = new HttpEmbeddingProvider(
                "http://localhost:8089/embed",
                384,
                5000,
                new SimpleMeterRegistry(),
                new ObjectMapper()
        );
    }
    
    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }
    
    @Test
    void testEmbedReturnsCorrectDimension() {
        // Mock response
        wireMockServer.stubFor(post(urlEqualTo("/embed"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createEmbeddingResponse(384))));
        
        float[] result = provider.embed("test text");
        
        assertThat(result).hasSize(384);
        assertThat(provider.getDimension()).isEqualTo(384);
        assertThat(provider.getProviderName()).isEqualTo("http");
    }
    
    @Test
    void testEmbedBatch() {
        wireMockServer.stubFor(post(urlEqualTo("/embed/batch"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createBatchEmbeddingResponse(384, 3))));
        
        List<String> texts = List.of("text1", "text2", "text3");
        List<float[]> results = provider.embedBatch(texts);
        
        assertThat(results).hasSize(3);
        assertThat(results.get(0)).hasSize(384);
    }
    
    private String createEmbeddingResponse(int dimension) {
        StringBuilder sb = new StringBuilder("{\"embedding\":[");
        for (int i = 0; i < dimension; i++) {
            if (i > 0) sb.append(",");
            sb.append(Math.random());
        }
        sb.append("]}");
        return sb.toString();
    }
    
    private String createBatchEmbeddingResponse(int dimension, int count) {
        StringBuilder sb = new StringBuilder("{\"embeddings\":[");
        for (int j = 0; j < count; j++) {
            if (j > 0) sb.append(",");
            sb.append("[");
            for (int i = 0; i < dimension; i++) {
                if (i > 0) sb.append(",");
                sb.append(Math.random());
            }
            sb.append("]");
        }
        sb.append("]}");
        return sb.toString();
    }
}
