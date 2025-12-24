package com.searchlight.infra.index;

import com.searchlight.domain.model.DocumentChunk;
import com.searchlight.domain.model.SearchQuery;
import com.searchlight.domain.model.SearchResult;
import com.searchlight.fixtures.SampleDocs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LuceneIndexerSearcherTest {
    
    private static final int DIMENSION = 384;
    
    @TempDir
    Path tempDir;
    
    private LuceneIndexer indexer;
    private LuceneSearcher searcher;
    
    @BeforeEach
    void setUp() throws Exception {
        String indexPath = tempDir.resolve("test-index").toString();
        
        indexer = new LuceneIndexer(indexPath, DIMENSION, SimilarityMode.COSINE, 16, 100);
        indexer.initialize();
        
        searcher = new LuceneSearcher(indexPath, DIMENSION);
        searcher.initialize();
    }
    
    @AfterEach
    void tearDown() {
        indexer.close();
        searcher.close();
    }
    
    @Test
    void testIndexAndRetrieve() {
        DocumentChunk chunk = SampleDocs.createSampleChunk(
                "test-1",
                "This is a test document",
                SampleDocs.createNormalizedVector(DIMENSION, 1)
        );
        
        indexer.index(chunk);
        indexer.commit();
        
        Optional<DocumentChunk> retrieved = searcher.getById("test-1");
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo("test-1");
        assertThat(retrieved.get().getContent()).isEqualTo("This is a test document");
    }
    
    @Test
    void testKeywordSearch() {
        List<DocumentChunk> docs = SampleDocs.createSampleDocuments(DIMENSION);
        indexer.indexBatch(docs);
        
        SearchQuery query = SearchQuery.builder()
                .queryText("machine learning")
                .topK(10)
                .alpha(0.0f) // Pure keyword search
                .offset(0)
                .build();
        
        List<SearchResult> results = searcher.search(query);
        
        assertThat(results).isNotEmpty();
        // Document with "machine learning" should be in results
        assertThat(results.stream()
                .anyMatch(r -> r.getSnippet().contains("Machine learning")))
                .isTrue();
    }
    
    @Test
    void testVectorSearch() {
        List<DocumentChunk> docs = SampleDocs.createSampleDocuments(DIMENSION);
        indexer.indexBatch(docs);
        
        // Search with a vector similar to doc 1
        float[] queryVector = SampleDocs.createNormalizedVector(DIMENSION, 1);
        
        SearchQuery query = SearchQuery.builder()
                .queryVector(queryVector)
                .topK(5)
                .alpha(1.0f) // Pure vector search
                .offset(0)
                .build();
        
        List<SearchResult> results = searcher.search(query);
        
        assertThat(results).isNotEmpty();
    }
    
    @Test
    void testHybridSearch() {
        List<DocumentChunk> docs = SampleDocs.createSampleDocuments(DIMENSION);
        indexer.indexBatch(docs);
        
        SearchQuery query = SearchQuery.builder()
                .queryText("neural networks")
                .queryVector(SampleDocs.createNormalizedVector(DIMENSION, 2))
                .topK(5)
                .alpha(0.5f) // Hybrid search
                .offset(0)
                .build();
        
        List<SearchResult> results = searcher.search(query);
        
        assertThat(results).isNotEmpty();
    }
    
    @Test
    void testDelete() {
        DocumentChunk chunk = SampleDocs.createSampleChunk(
                "delete-me",
                "This will be deleted",
                SampleDocs.createNormalizedVector(DIMENSION, 99)
        );
        
        indexer.index(chunk);
        indexer.commit();
        
        assertThat(searcher.getById("delete-me")).isPresent();
        
        indexer.delete("delete-me");
        indexer.commit();
        
        assertThat(searcher.getById("delete-me")).isEmpty();
    }
    
    @Test
    void testDocumentCount() {
        assertThat(indexer.getDocumentCount()).isZero();
        
        indexer.indexBatch(SampleDocs.createSampleDocuments(DIMENSION));
        
        assertThat(indexer.getDocumentCount()).isEqualTo(5);
    }
}
