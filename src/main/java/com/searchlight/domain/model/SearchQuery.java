package com.searchlight.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Search query parameters for hybrid search.
 */
@Data
@Builder
public class SearchQuery {
    private String queryText;
    private float[] queryVector;
    private int topK;
    private float alpha; // 0.0 = pure keyword, 1.0 = pure vector
    private int offset;
    private String sourceFilter;
    private Instant afterDate;
}
