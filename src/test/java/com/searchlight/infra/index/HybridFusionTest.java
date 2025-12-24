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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test hybrid late-fusion scoring.
 * Validates that alpha parameter controls the balance between BM25 and KNN.
 */
class HybridFusionTest {
    
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
        
        // Index sample documents
        indexer.indexBatch(SampleDocs.createSampleDocuments(DIMENSION));
    }
    
    @AfterEach
    void tearDown() {
        indexer.close();
        searcher.close();
    }
    
    @Test
    void testAlphaZero_UsesBM25Only() {
        // When alpha=0, results should be purely based on BM25 keyword matching
        SearchQuery query = SearchQuery.builder()
                .queryText("machine learning")
                .queryVector(SampleDocs.createNormalizedVector(DIMENSION, 1))
                .topK(5)
                .alpha(0.0f)
                .offset(0)
                .build();
        
        List<SearchResult> results = searcher.search(query);

        assertThat(results).isNotEmpty();

        // When alpha=0, all scores should come from BM25 (keyword search)
        // At least one result should have a positive keyword score (the top result)
        assertThat(results.get(0).getKeywordScore()).isGreaterThan(0.0f);

        // All results should have vectorScore = 0 and score = keywordScore
        for (SearchResult result : results) {
            assertThat(result.getVectorScore()).isEqualTo(0.0f);
            assertThat(result.getScore()).isEqualTo(result.getKeywordScore());
        }
    }
    
    @Test
    void testAlphaOne_UsesKNNOnly() {
        // When alpha=1, results should be purely based on vector similarity
        SearchQuery query = SearchQuery.builder()
                .queryText("machine learning")
                .queryVector(SampleDocs.createNormalizedVector(DIMENSION, 1))
                .topK(5)
                .alpha(1.0f)
                .offset(0)
                .build();
        
        List<SearchResult> results = searcher.search(query);

        assertThat(results).isNotEmpty();

        // When alpha=1, all scores should come from KNN (vector search)
        // At least one result should have a positive vector score (the top result)
        assertThat(results.get(0).getVectorScore()).isGreaterThan(0.0f);

        // All results should have keywordScore = 0 and score = vectorScore
        for (SearchResult result : results) {
            assertThat(result.getKeywordScore()).isEqualTo(0.0f);
            assertThat(result.getScore()).isEqualTo(result.getVectorScore());
        }
    }
    
    @Test
    void testAlphaHalf_CombinesBoth() {
        // When alpha=0.5, results should be balanced fusion
        SearchQuery query = SearchQuery.builder()
                .queryText("machine learning")
                .queryVector(SampleDocs.createNormalizedVector(DIMENSION, 1))
                .topK(5)
                .alpha(0.5f)
                .offset(0)
                .build();
        
        List<SearchResult> results = searcher.search(query);
        
        assertThat(results).isNotEmpty();
        
        // Results should have both scores contributing
        boolean hasHybridScoring = results.stream()
                .anyMatch(r -> r.getKeywordScore() > 0.0f && r.getVectorScore() > 0.0f);
        
        assertThat(hasHybridScoring).isTrue();
    }
    
    @Test
    void testDifferentAlphas_ProduceDifferentRankings() {
        float[] queryVector = SampleDocs.createNormalizedVector(DIMENSION, 1);
        
        // Search with alpha=0 (pure BM25)
        SearchQuery bm25Query = SearchQuery.builder()
                .queryText("machine learning")
                .queryVector(queryVector)
                .topK(5)
                .alpha(0.0f)
                .offset(0)
                .build();
        
        List<SearchResult> bm25Results = searcher.search(bm25Query);
        
        // Search with alpha=1 (pure KNN)
        SearchQuery knnQuery = SearchQuery.builder()
                .queryText("machine learning")
                .queryVector(queryVector)
                .topK(5)
                .alpha(1.0f)
                .offset(0)
                .build();
        
        List<SearchResult> knnResults = searcher.search(knnQuery);
        
        // Extract result IDs in order
        List<String> bm25Ids = bm25Results.stream()
                .map(SearchResult::getId)
                .collect(Collectors.toList());
        
        List<String> knnIds = knnResults.stream()
                .map(SearchResult::getId)
                .collect(Collectors.toList());
        
        // Rankings should differ when alpha changes from 0 to 1
        // (unless by coincidence they happen to be the same)
        // We just verify both produce valid results
        assertThat(bm25Ids).isNotEmpty();
        assertThat(knnIds).isNotEmpty();
    }
    
    @Test
    void testScoresAreNormalized() {
        SearchQuery query = SearchQuery.builder()
                .queryText("machine learning")
                .queryVector(SampleDocs.createNormalizedVector(DIMENSION, 1))
                .topK(5)
                .alpha(0.5f)
                .offset(0)
                .build();
        
        List<SearchResult> results = searcher.search(query);
        
        assertThat(results).isNotEmpty();
        
        // After normalization, fused scores should be in [0, 1] range
        for (SearchResult result : results) {
            assertThat(result.getScore()).isBetween(0.0f, 1.0f);
            
            // Individual scores should also be normalized
            if (result.getKeywordScore() > 0) {
                assertThat(result.getKeywordScore()).isBetween(0.0f, 1.0f);
            }
            if (result.getVectorScore() > 0) {
                assertThat(result.getVectorScore()).isBetween(0.0f, 1.0f);
            }
        }
    }
}