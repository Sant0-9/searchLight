package com.searchlight.domain.ports;

import com.searchlight.domain.model.DocumentChunk;

import java.util.List;

/**
 * Port for indexing document chunks.
 */
public interface Indexer {
    
    /**
     * Index a single document chunk.
     */
    void index(DocumentChunk chunk);
    
    /**
     * Index multiple document chunks in a batch.
     */
    void indexBatch(List<DocumentChunk> chunks);
    
    /**
     * Commit all pending changes to the index.
     */
    void commit();
    
    /**
     * Delete a document by ID.
     */
    void delete(String id);
    
    /**
     * Delete all documents from a source.
     */
    void deleteBySource(String source);
    
    /**
     * Clear the entire index.
     */
    void clearAll();
    
    /**
     * Get the total number of documents in the index.
     */
    long getDocumentCount();
    
    /**
     * Close the indexer and release resources.
     */
    void close();
}
