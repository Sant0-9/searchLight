package com.searchlight.api.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class SearchRequest {
    
    private String q;
    private float[] vector;
    private Integer k = 10;
    private Float alpha = 0.5f;
    private Integer from = 0;
    private FilterParams filters;
    
    @Data
    public static class FilterParams {
        private String source;
        private Instant after;
    }
}
