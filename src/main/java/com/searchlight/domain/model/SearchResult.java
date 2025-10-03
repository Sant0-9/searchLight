package com.searchlight.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * A single search result with scoring information.
 */
@Data
@Builder
public class SearchResult {
    private String id;
    private String sourceId;
    private String title;
    private String url;
    private String snippet;
    private float score;
    private float keywordScore;
    private float vectorScore;
    private Instant timestamp;
    private String source;
    private int chunkIndex;
    private Map<String, Object> metadata;
}
