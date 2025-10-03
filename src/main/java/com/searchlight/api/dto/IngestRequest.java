package com.searchlight.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class IngestRequest {
    
    @NotEmpty(message = "URLs list cannot be empty")
    private List<String> urls;
    
    @NotNull(message = "Mode is required")
    private IngestMode mode;
    
    private String source;
    
    public enum IngestMode {
        RSS,
        URL
    }
}
