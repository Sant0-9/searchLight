package com.searchlight.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IngestResponse {
    private int documentsIngested;
    private int urlsProcessed;
    private long timeMs;
    private String status;
}
