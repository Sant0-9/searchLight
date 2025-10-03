package com.searchlight.infra.index;

import com.searchlight.domain.model.DocumentChunk;
import com.searchlight.domain.model.SearchQuery;
import com.searchlight.domain.model.SearchResult;
import com.searchlight.domain.ports.Searcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
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
            
            Query luceneQuery = buildHybridQuery(query);
            
            int totalToFetch = query.getTopK() + query.getOffset();
            TopDocs topDocs = searcher.search(luceneQuery, totalToFetch);
            
            List<SearchResult> results = new ArrayList<>();
            int start = Math.min(query.getOffset(), topDocs.scoreDocs.length);
            int end = Math.min(totalToFetch, topDocs.scoreDocs.length);
            
            for (int i = start; i < end; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                Document doc = searcher.doc(scoreDoc.doc);
                
                SearchResult result = SearchResult.builder()
                        .id(doc.get("id"))
                        .sourceId(doc.get("sourceId"))
                        .title(doc.get("title"))
                        .url(doc.get("url"))
                        .snippet(createSnippet(doc.get("content"), 200))
                        .score(scoreDoc.score)
                        .source(doc.get("source"))
                        .chunkIndex(doc.getField("chunkIndex") != null ? 
                                doc.getField("chunkIndex").numericValue().intValue() : 0)
                        .timestamp(doc.getField("timestamp") != null ? 
                                Instant.ofEpochMilli(doc.getField("timestamp").numericValue().longValue()) : null)
                        .metadata(new HashMap<>())
                        .build();
                
                results.add(result);
            }
            
            log.debug("Search returned {} results for query", results.size());
            return results;
            
        } catch (IOException e) {
            log.error("Search failed", e);
            throw new RuntimeException("Search failed", e);
        }
    }
    
    @Override
    public Optional<DocumentChunk> getById(String id) {
        try {
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
    
    private Query buildHybridQuery(SearchQuery query) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        
        // Keyword query (BM25)
        if (query.getQueryText() != null && !query.getQueryText().isBlank() && query.getAlpha() < 1.0f) {
            try {
                MultiFieldQueryParser parser = new MultiFieldQueryParser(
                        new String[]{"title", "content", "keywords"},
                        analyzer,
                        Map.of("title", 2.0f, "content", 1.0f, "keywords", 1.5f)
                );
                Query textQuery = parser.parse(query.getQueryText());
                
                if (query.getAlpha() > 0) {
                    // Boost based on alpha
                    float keywordBoost = (1.0f - query.getAlpha()) * 10;
                    textQuery = new BoostQuery(textQuery, keywordBoost);
                }
                
                builder.add(textQuery, BooleanClause.Occur.SHOULD);
            } catch (ParseException e) {
                log.warn("Failed to parse query text: {}", query.getQueryText(), e);
            }
        }
        
        // Vector query (KNN)
        if (query.getQueryVector() != null && query.getQueryVector().length == vectorDimension && query.getAlpha() > 0) {
            // Note: Lucene's KNN search is handled differently in production
            // For now, we create a placeholder that will be replaced with actual KNN search
            Query vectorQuery = new KnnFloatVectorQuery("vector", query.getQueryVector(), query.getTopK());
            
            if (query.getAlpha() < 1.0f) {
                float vectorBoost = query.getAlpha() * 10;
                vectorQuery = new BoostQuery(vectorQuery, vectorBoost);
            }
            
            builder.add(vectorQuery, BooleanClause.Occur.SHOULD);
        }
        
        // Filters
        if (query.getSourceFilter() != null) {
            builder.add(new TermQuery(new Term("source", query.getSourceFilter())), BooleanClause.Occur.FILTER);
        }
        
        if (query.getAfterDate() != null) {
            builder.add(LongPoint.newRangeQuery("timestamp", 
                    query.getAfterDate().toEpochMilli(), Long.MAX_VALUE), 
                    BooleanClause.Occur.FILTER);
        }
        
        BooleanQuery booleanQuery = builder.build();
        
        // If no clauses, return match all
        if (booleanQuery.clauses().isEmpty()) {
            return new MatchAllDocsQuery();
        }
        
        return booleanQuery;
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
