package com.searchlight.domain.ports;

import com.searchlight.domain.model.DocumentChunk;
import com.searchlight.domain.model.SearchQuery;
import com.searchlight.domain.model.SearchResult;

import java.util.List;
import java.util.Optional;

/**
 * Port for searching the document index.
 */
public interface Searcher {
    
    /**
     * Perform a hybrid search combining keyword and vector similarity.
     */
    List<SearchResult> search(SearchQuery query);
    
    /**
     * Get a document by ID.
     */
    Optional<DocumentChunk> getById(String id);
    
    /**
     * Get all chunks belonging to a source document.
     */
    List<DocumentChunk> getBySourceId(String sourceId);
    
    /**
     * Close the searcher and release resources.
     */
    void close();
}
