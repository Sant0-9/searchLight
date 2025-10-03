package com.searchlight.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Represents a chunk of a document with its vector embedding.
 * This is the core entity stored in the Lucene index.
 */
@Data
@Builder
public class DocumentChunk {
    private String id;
    private String sourceId;
    private String title;
    private String url;
    private String content;
    private float[] vector;
    private String[] keywords;
    private Instant timestamp;
    private String source;
    private int chunkIndex;
    private String contentHash;
}
