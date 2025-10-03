package com.searchlight.api.controller;

import com.searchlight.domain.ports.EmbeddingProvider;
import com.searchlight.domain.ports.Indexer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {
    
    private final Indexer indexer;
    private final EmbeddingProvider embeddingProvider;
    
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "searchlight",
                "version", "0.1.0",
                "documentCount", indexer.getDocumentCount(),
                "embeddingProvider", embeddingProvider.getProviderName(),
                "embeddingDimension", embeddingProvider.getDimension()
        ));
    }
}
