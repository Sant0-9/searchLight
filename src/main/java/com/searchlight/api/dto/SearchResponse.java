package com.searchlight.api.dto;

import com.searchlight.domain.model.SearchResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {
    
    private List<SearchResult> results;
    private int total;
    private int offset;
    private int limit;
    private long queryTimeMs;
    private QueryInfo query;
    
    @Data
    @Builder
    public static class QueryInfo {
        private String text;
        private boolean hasVector;
        private float alpha;
    }
}
