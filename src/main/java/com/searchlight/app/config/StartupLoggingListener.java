package com.searchlight.app.config;

import com.searchlight.domain.ports.Indexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs index statistics and configuration on application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupLoggingListener {
    
    private final Indexer indexer;
    
    @Value("${searchlight.embedding.dimension:384}")
    private int vectorDimension;
    
    @Value("${searchlight.index.hnsw.m:16}")
    private int hnswM;
    
    @Value("${searchlight.index.hnsw.ef-construction:100}")
    private int hnswEfConstruction;
    
    @Value("${searchlight.index.similarity:COSINE}")
    private String similarity;
    
    @EventListener(ApplicationReadyEvent.class)
    public void logStartupInfo() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔦 Searchlight API Ready");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📊 Index Statistics:");
        log.info("   Documents in index: {}", indexer.getDocumentCount());
        log.info("   Vector dimension: {}", vectorDimension);
        log.info("   Similarity function: {}", similarity);
        log.info("");
        log.info("⚙️  HNSW Configuration:");
        log.info("   M (max connections): {}", hnswM);
        log.info("   efConstruction: {}", hnswEfConstruction);
        log.info("");
        log.info("🔍 Hybrid Search:");
        log.info("   BM25 + KNN late-fusion enabled");
        log.info("   Use alpha parameter to control fusion (0=BM25, 1=KNN)");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}