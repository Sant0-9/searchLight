package com.searchlight.api.controller;

import com.searchlight.api.dto.SearchRequest;
import com.searchlight.api.dto.SearchResponse;
import com.searchlight.domain.model.DocumentChunk;
import com.searchlight.domain.model.SearchQuery;
import com.searchlight.domain.model.SearchResult;
import com.searchlight.domain.ports.EmbeddingProvider;
import com.searchlight.domain.ports.Searcher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Search API endpoints")
public class SearchController {
    
    private final Searcher searcher;
    private final EmbeddingProvider embeddingProvider;
    private final MeterRegistry meterRegistry;
    
    @PostMapping("/search")
    @Operation(summary = "Search documents", description = "Hybrid keyword + vector search")
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Build query
            SearchQuery.SearchQueryBuilder queryBuilder = SearchQuery.builder()
                    .topK(request.getK())
                    .alpha(request.getAlpha())
                    .offset(request.getFrom());
            
            // Add text query
            if (request.getQ() != null && !request.getQ().isBlank()) {
                queryBuilder.queryText(request.getQ());
                
                // Generate vector from text if no vector provided and alpha > 0
                if (request.getVector() == null && request.getAlpha() > 0) {
                    float[] vector = embeddingProvider.embed(request.getQ());
                    queryBuilder.queryVector(vector);
                }
            }
            
            // Add explicit vector
            if (request.getVector() != null) {
                queryBuilder.queryVector(request.getVector());
            }
            
            // Add filters
            if (request.getFilters() != null) {
                if (request.getFilters().getSource() != null) {
                    queryBuilder.sourceFilter(request.getFilters().getSource());
                }
                if (request.getFilters().getAfter() != null) {
                    queryBuilder.afterDate(request.getFilters().getAfter());
                }
            }
            
            SearchQuery query = queryBuilder.build();
            
            // Execute search
            List<SearchResult> results = searcher.search(query);
            
            long queryTimeMs = sample.stop(meterRegistry.timer("search.latency"));
            meterRegistry.counter("search.requests").increment();
            
            SearchResponse response = SearchResponse.builder()
                    .results(results)
                    .total(results.size())
                    .offset(request.getFrom())
                    .limit(request.getK())
                    .queryTimeMs(queryTimeMs / 1_000_000) // Convert to ms
                    .query(SearchResponse.QueryInfo.builder()
                            .text(request.getQ())
                            .hasVector(request.getVector() != null || request.getQ() != null)
                            .alpha(request.getAlpha())
                            .build())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Search failed", e);
            meterRegistry.counter("search.errors").increment();
            throw e;
        }
    }
    
    @GetMapping("/docs/{id}")
    @Operation(summary = "Get document by ID")
    public ResponseEntity<DocumentChunk> getDocument(@PathVariable String id) {
        Optional<DocumentChunk> doc = searcher.getById(id);
        return doc.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/docs/source/{sourceId}")
    @Operation(summary = "Get all chunks from a source document")
    public ResponseEntity<List<DocumentChunk>> getDocumentsBySource(@PathVariable String sourceId) {
        List<DocumentChunk> chunks = searcher.getBySourceId(sourceId);
        return ResponseEntity.ok(chunks);
    }
}
