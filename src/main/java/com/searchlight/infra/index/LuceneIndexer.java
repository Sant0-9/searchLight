package com.searchlight.infra.index;

import com.searchlight.domain.model.DocumentChunk;
import com.searchlight.domain.ports.Indexer;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Lucene-based indexer with HNSW vector support.
 */
@Slf4j
@Component
public class LuceneIndexer implements Indexer {
    
    private final Path indexPath;
    private final int vectorDimension;
    private final SimilarityMode similarityMode;
    private final int hnswM;
    private final int hnswEfConstruction;
    
    private Directory directory;
    private IndexWriter writer;
    private final StandardAnalyzer analyzer;
    
    public LuceneIndexer(
            @Value("${searchlight.index.path:data/index}") String indexPath,
            @Value("${searchlight.embedding.dimension:384}") int vectorDimension,
            @Value("${searchlight.index.similarity:COSINE}") SimilarityMode similarityMode,
            @Value("${searchlight.index.hnsw.m:16}") int hnswM,
            @Value("${searchlight.index.hnsw.ef-construction:100}") int hnswEfConstruction) {
        this.indexPath = Path.of(indexPath);
        this.vectorDimension = vectorDimension;
        this.similarityMode = similarityMode;
        this.hnswM = hnswM;
        this.hnswEfConstruction = hnswEfConstruction;
        this.analyzer = new StandardAnalyzer();
    }
    
    @PostConstruct
    public void initialize() throws IOException {
        log.info("Initializing Lucene index at {} with dimension={}, similarity={}, M={}, efConstruction={}", 
                indexPath, vectorDimension, similarityMode, hnswM, hnswEfConstruction);
        
        this.indexPath.toFile().mkdirs();
        this.directory = FSDirectory.open(this.indexPath);
        
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        config.setCommitOnClose(true);
        
        // Use custom codec with HNSW parameters
        config.setCodec(new SearchlightCodec(hnswM, hnswEfConstruction));
        
        this.writer = new IndexWriter(directory, config);
        
        log.info("Lucene index initialized with {} documents", writer.getDocStats().numDocs);
    }
    
    @Override
    public void index(DocumentChunk chunk) {
        try {
            Document doc = createDocument(chunk);
            writer.updateDocument(new Term("id", chunk.getId()), doc);
        } catch (IOException e) {
            log.error("Failed to index document {}", chunk.getId(), e);
            throw new RuntimeException("Indexing failed", e);
        }
    }
    
    @Override
    public void indexBatch(List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            index(chunk);
        }
        commit();
    }
    
    @Override
    public void commit() {
        try {
            writer.commit();
            log.debug("Index committed successfully");
        } catch (IOException e) {
            log.error("Failed to commit index", e);
            throw new RuntimeException("Commit failed", e);
        }
    }
    
    @Override
    public void delete(String id) {
        try {
            writer.deleteDocuments(new Term("id", id));
        } catch (IOException e) {
            log.error("Failed to delete document {}", id, e);
            throw new RuntimeException("Delete failed", e);
        }
    }
    
    @Override
    public void deleteBySource(String source) {
        try {
            writer.deleteDocuments(new Term("source", source));
            commit();
        } catch (IOException e) {
            log.error("Failed to delete documents from source {}", source, e);
            throw new RuntimeException("Delete by source failed", e);
        }
    }
    
    @Override
    public void clearAll() {
        try {
            writer.deleteAll();
            commit();
            log.info("Index cleared");
        } catch (IOException e) {
            log.error("Failed to clear index", e);
            throw new RuntimeException("Clear failed", e);
        }
    }
    
    @Override
    public long getDocumentCount() {
        return writer.getDocStats().numDocs;
    }
    
    @Override
    @PreDestroy
    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (directory != null) {
                directory.close();
            }
            analyzer.close();
            log.info("Lucene index closed");
        } catch (IOException e) {
            log.error("Error closing index", e);
        }
    }
    
    private Document createDocument(DocumentChunk chunk) {
        Document doc = new Document();
        
        // ID fields
        doc.add(new StringField("id", chunk.getId(), Field.Store.YES));
        doc.add(new StringField("sourceId", chunk.getSourceId(), Field.Store.YES));
        
        // Text fields
        doc.add(new TextField("title", chunk.getTitle() != null ? chunk.getTitle() : "", Field.Store.YES));
        doc.add(new TextField("content", chunk.getContent(), Field.Store.YES));
        doc.add(new StoredField("url", chunk.getUrl() != null ? chunk.getUrl() : ""));
        
        // Keywords (analyzed)
        if (chunk.getKeywords() != null) {
            for (String keyword : chunk.getKeywords()) {
                doc.add(new TextField("keywords", keyword, Field.Store.NO));
            }
        }
        
        // Metadata
        doc.add(new StringField("source", chunk.getSource() != null ? chunk.getSource() : "unknown", Field.Store.YES));
        doc.add(new IntPoint("chunkIndex", chunk.getChunkIndex()));
        doc.add(new StoredField("chunkIndex", chunk.getChunkIndex()));
        
        // Timestamp
        if (chunk.getTimestamp() != null) {
            long epochMilli = chunk.getTimestamp().toEpochMilli();
            doc.add(new LongPoint("timestamp", epochMilli));
            doc.add(new StoredField("timestamp", epochMilli));
        }
        
        // Vector field with HNSW
        if (chunk.getVector() != null && chunk.getVector().length == vectorDimension) {
            VectorSimilarityFunction similarityFunction = switch (similarityMode) {
                case COSINE -> VectorSimilarityFunction.COSINE;
                case DOT_PRODUCT -> VectorSimilarityFunction.DOT_PRODUCT;
                case EUCLIDEAN -> VectorSimilarityFunction.EUCLIDEAN;
            };
            
            doc.add(new KnnFloatVectorField("vector", chunk.getVector(), similarityFunction));
        }
        
        return doc;
    }
}
