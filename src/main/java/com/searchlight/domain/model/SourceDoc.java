package com.searchlight.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Represents the original source document before chunking.
 */
@Data
@Builder
public class SourceDoc {
    private String id;
    private String url;
    private String title;
    private String content;
    private String htmlContent;
    private String source;
    private Instant publishedAt;
    private Instant fetchedAt;
    private String contentType;
}
