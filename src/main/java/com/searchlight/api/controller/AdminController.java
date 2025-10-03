package com.searchlight.api.controller;

import com.searchlight.api.dto.IngestRequest;
import com.searchlight.api.dto.IngestResponse;
import com.searchlight.domain.ports.Indexer;
import com.searchlight.infra.ingest.RssIngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative operations")
public class AdminController {
    
    private final RssIngestService ingestService;
    private final Indexer indexer;
    
    @PostMapping("/ingest")
    @Operation(summary = "Ingest documents from URLs or RSS feeds")
    public ResponseEntity<IngestResponse> ingest(@Valid @RequestBody IngestRequest request) {
        log.info("Ingesting {} URLs with mode {}", request.getUrls().size(), request.getMode());
        
        long startTime = System.currentTimeMillis();
        int documentsIngested;
        
        try {
            if (request.getMode() == IngestRequest.IngestMode.RSS) {
                documentsIngested = ingestService.ingestRssFeeds(request.getUrls());
            } else {
                documentsIngested = 0;
                for (String url : request.getUrls()) {
                    ingestService.ingestUrl(url, request.getSource());
                    documentsIngested++;
                }
            }
            
            long timeMs = System.currentTimeMillis() - startTime;
            
            IngestResponse response = IngestResponse.builder()
                    .documentsIngested(documentsIngested)
                    .urlsProcessed(request.getUrls().size())
                    .timeMs(timeMs)
                    .status("success")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ingestion failed", e);
            long timeMs = System.currentTimeMillis() - startTime;
            
            IngestResponse response = IngestResponse.builder()
                    .documentsIngested(0)
                    .urlsProcessed(request.getUrls().size())
                    .timeMs(timeMs)
                    .status("error: " + e.getMessage())
                    .build();
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @PostMapping("/reindex")
    @Operation(summary = "Clear and rebuild the index")
    public ResponseEntity<Map<String, Object>> reindex() {
        log.info("Reindexing requested");
        
        try {
            long before = indexer.getDocumentCount();
            indexer.clearAll();
            long after = indexer.getDocumentCount();
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "documentsBefore", before,
                    "documentsAfter", after,
                    "message", "Index cleared. Use /ingest to add documents."
            ));
            
        } catch (Exception e) {
            log.error("Reindex failed", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get index statistics")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
                "documentCount", indexer.getDocumentCount(),
                "indexPath", "data/index"
        ));
    }
}
