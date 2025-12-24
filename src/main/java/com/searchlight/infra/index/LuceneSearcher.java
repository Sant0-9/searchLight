package com.searchlight.infra.index;

import com.searchlight.domain.model.DocumentChunk;
import com.searchlight.domain.model.SearchQuery;
import com.searchlight.domain.model.SearchResult;
import com.searchlight.domain.ports.Searcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Lucene-based searcher with hybrid keyword + vector search.
 */
@Slf4j
@Component
public class LuceneSearcher implements Searcher {
    
    private final Path indexPath;
    private final int vectorDimension;
    
    private Directory directory;
    private IndexReader reader;
    private IndexSearcher searcher;
    private final StandardAnalyzer analyzer;
    
    public LuceneSearcher(
            @Value("${searchlight.index.path:data/index}") String indexPath,
            @Value("${searchlight.embedding.dimension:384}") int vectorDimension) {
        this.indexPath = Path.of(indexPath);
        this.vectorDimension = vectorDimension;
        this.analyzer = new StandardAnalyzer();
    }
    
    @PostConstruct
    public void initialize() throws IOException {
        log.info("Initializing Lucene searcher at {}", indexPath);
        this.directory = FSDirectory.open(this.indexPath);
        reopenReader();
    }
    
    private void reopenReader() throws IOException {
        if (DirectoryReader.indexExists(directory)) {
            IndexReader newReader = DirectoryReader.open(directory);
            if (reader != null) {
                reader.close();
            }
            reader = newReader;
            searcher = new IndexSearcher(reader);
            log.debug("Reader reopened, {} documents in index", reader.numDocs());
        } else {
            log.warn("Index does not exist yet at {}", indexPath);
        }
    }
    
    @Override
    public List<SearchResult> search(SearchQuery query) {
        try {
            // Reopen reader to get latest changes
            reopenReader();
            
            if (searcher == null) {
                return Collections.emptyList();
            }
            
            // Perform late-fusion hybrid search
            return hybridLateFusion(query);
            
        } catch (IOException e) {
            log.error("Search failed", e);
            throw new RuntimeException("Search failed", e);
        }
    }
    
    /**
     * Hybrid late-fusion scoring: run BM25 and KNN separately, normalize, and fuse.
     */
    private List<SearchResult> hybridLateFusion(SearchQuery query) throws IOException {
        Map<String, SearchResult> resultsMap = new HashMap<>();
        
        // Run BM25 query if alpha < 1.0
        if (query.getQueryText() != null && !query.getQueryText().isBlank() && query.getAlpha() < 1.0f) {
            List<SearchResult> bm25Results = runBM25Search(query);
            normalizeScores(bm25Results);
            
            for (SearchResult result : bm25Results) {
                result.setKeywordScore(result.getScore());
                result.setVectorScore(0.0f);
                resultsMap.put(result.getId(), result);
            }
        }
        
        // Run KNN query if alpha > 0 and vector is present
        if (query.getQueryVector() != null && query.getQueryVector().length == vectorDimension && query.getAlpha() > 0) {
            List<SearchResult> knnResults = runKNNSearch(query);
            normalizeScores(knnResults);
            
            for (SearchResult result : knnResults) {
                if (resultsMap.containsKey(result.getId())) {
                    // Merge with existing BM25 result
                    SearchResult existing = resultsMap.get(result.getId());
                    existing.setVectorScore(result.getScore());
                } else {
                    // New result from KNN only
                    result.setVectorScore(result.getScore());
                    result.setKeywordScore(0.0f);
                    resultsMap.put(result.getId(), result);
                }
            }
        }
        
        // Apply fusion: score = (1-alpha) * bm25 + alpha * knn
        List<SearchResult> fusedResults = new ArrayList<>(resultsMap.values());
        for (SearchResult result : fusedResults) {
            float fusedScore = (1.0f - query.getAlpha()) * result.getKeywordScore() + 
                              query.getAlpha() * result.getVectorScore();
            result.setScore(fusedScore);
        }
        
        // Sort by fused score descending
        fusedResults.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        
        // Apply offset and limit
        int start = Math.min(query.getOffset(), fusedResults.size());
        int end = Math.min(query.getOffset() + query.getTopK(), fusedResults.size());
        
        List<SearchResult> finalResults = fusedResults.subList(start, end);
        log.debug("Hybrid search returned {} results (alpha={})", finalResults.size(), query.getAlpha());
        
        return finalResults;
    }
    
    /**
     * Run BM25 keyword search.
     */
    private List<SearchResult> runBM25Search(SearchQuery query) throws IOException {
        try {
            MultiFieldQueryParser parser = new MultiFieldQueryParser(
                    new String[]{"title", "content", "keywords"},
                    analyzer,
                    Map.of("title", 2.0f, "content", 1.0f, "keywords", 1.5f)
            );
            Query textQuery = parser.parse(query.getQueryText());
            
            // Apply filters
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(textQuery, BooleanClause.Occur.MUST);
            addFilters(builder, query);
            
            int fetchSize = Math.max(query.getTopK() * 2, 100); // Fetch more for fusion
            TopDocs topDocs = searcher.search(builder.build(), fetchSize);
            
            return convertToResults(topDocs);
            
        } catch (ParseException e) {
            log.warn("Failed to parse query text: {}", query.getQueryText(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Run KNN vector search.
     */
    private List<SearchResult> runKNNSearch(SearchQuery query) throws IOException {
        Query vectorQuery = new KnnFloatVectorQuery("vector", query.getQueryVector(), 
                Math.max(query.getTopK() * 2, 100)); // Fetch more for fusion
        
        // Apply filters
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(vectorQuery, BooleanClause.Occur.MUST);
        addFilters(builder, query);
        
        TopDocs topDocs = searcher.search(builder.build(), Math.max(query.getTopK() * 2, 100));
        
        return convertToResults(topDocs);
    }
    
    /**
     * Add filter clauses to query builder.
     */
    private void addFilters(BooleanQuery.Builder builder, SearchQuery query) {
        if (query.getSourceFilter() != null) {
            builder.add(new TermQuery(new Term("source", query.getSourceFilter())), 
                    BooleanClause.Occur.FILTER);
        }
        
        if (query.getAfterDate() != null) {
            builder.add(LongPoint.newRangeQuery("timestamp", 
                    query.getAfterDate().toEpochMilli(), Long.MAX_VALUE), 
                    BooleanClause.Occur.FILTER);
        }
    }
    
    /**
     * Convert Lucene TopDocs to SearchResults.
     */
    private List<SearchResult> convertToResults(TopDocs topDocs) throws IOException {
        List<SearchResult> results = new ArrayList<>();
        
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            
            SearchResult result = SearchResult.builder()
                    .id(doc.get("id"))
                    .sourceId(doc.get("sourceId"))
                    .title(doc.get("title"))
                    .url(doc.get("url"))
                    .snippet(createSnippet(doc.get("content"), 200))
                    .score(scoreDoc.score)
                    .keywordScore(0.0f)
                    .vectorScore(0.0f)
                    .source(doc.get("source"))
                    .chunkIndex(doc.getField("chunkIndex") != null ?
                            doc.getField("chunkIndex").numericValue().intValue() : 0)
                    .timestamp(doc.getField("timestamp") != null ?
                            Instant.ofEpochMilli(doc.getField("timestamp").numericValue().longValue()) : null)
                    .metadata(new HashMap<>())
                    .build();
            
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * Min-max normalize scores to [0, 1] range.
     */
    private void normalizeScores(List<SearchResult> results) {
        if (results.isEmpty()) {
            return;
        }
        
        float minScore = Float.MAX_VALUE;
        float maxScore = Float.MIN_VALUE;
        
        for (SearchResult result : results) {
            minScore = Math.min(minScore, result.getScore());
            maxScore = Math.max(maxScore, result.getScore());
        }
        
        // Avoid division by zero
        float range = maxScore - minScore;
        if (range < 0.0001f) {
            for (SearchResult result : results) {
                result.setScore(1.0f);
            }
            return;
        }
        
        for (SearchResult result : results) {
            float normalized = (result.getScore() - minScore) / range;
            result.setScore(normalized);
        }
    }
    
    @Override
    public Optional<DocumentChunk> getById(String id) {
        try {
            // Reopen reader to get latest changes
            reopenReader();

            if (searcher == null) {
                return Optional.empty();
            }

            Query query = new TermQuery(new Term("id", id));
            TopDocs topDocs = searcher.search(query, 1);

            if (topDocs.scoreDocs.length == 0) {
                return Optional.empty();
            }

            Document doc = searcher.doc(topDocs.scoreDocs[0].doc);
            return Optional.of(documentToChunk(doc));

        } catch (IOException e) {
            log.error("Failed to get document by id {}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<DocumentChunk> getBySourceId(String sourceId) {
        try {
            if (searcher == null) {
                return Collections.emptyList();
            }
            
            Query query = new TermQuery(new Term("sourceId", sourceId));
            TopDocs topDocs = searcher.search(query, 1000);
            
            List<DocumentChunk> chunks = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                chunks.add(documentToChunk(doc));
            }
            
            return chunks;
            
        } catch (IOException e) {
            log.error("Failed to get documents by sourceId {}", sourceId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    @PreDestroy
    public void close() {
        try {
            if (reader != null) {
                reader.close();
            }
            if (directory != null) {
                directory.close();
            }
            analyzer.close();
            log.info("Lucene searcher closed");
        } catch (IOException e) {
            log.error("Error closing searcher", e);
        }
    }
    
    private DocumentChunk documentToChunk(Document doc) {
        return DocumentChunk.builder()
                .id(doc.get("id"))
                .sourceId(doc.get("sourceId"))
                .title(doc.get("title"))
                .url(doc.get("url"))
                .content(doc.get("content"))
                .source(doc.get("source"))
                .chunkIndex(doc.getField("chunkIndex") != null ? 
                        doc.getField("chunkIndex").numericValue().intValue() : 0)
                .timestamp(doc.getField("timestamp") != null ? 
                        Instant.ofEpochMilli(doc.getField("timestamp").numericValue().longValue()) : null)
                .build();
    }
    
    private String createSnippet(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
