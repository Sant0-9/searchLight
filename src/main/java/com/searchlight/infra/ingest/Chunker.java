package com.searchlight.infra.ingest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunks text into smaller pieces for embedding.
 * Uses a simple token-based approach with overlap.
 */
@Slf4j
@Component
public class Chunker {
    
    private final int chunkSize;
    private final int chunkOverlap;
    
    public Chunker(
            @Value("${searchlight.chunker.size:512}") int chunkSize,
            @Value("${searchlight.chunker.overlap:50}") int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        log.info("Initialized chunker: size={}, overlap={}", chunkSize, chunkOverlap);
    }
    
    /**
     * Chunk text into overlapping segments.
     * Uses word-based chunking as a simple approximation of token chunking.
     */
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        
        if (words.length == 0) {
            return List.of();
        }
        
        // If text is shorter than chunk size, return as single chunk
        if (words.length <= chunkSize) {
            chunks.add(text);
            return chunks;
        }
        
        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + chunkSize, words.length);
            
            // Join words into chunk
            StringBuilder chunk = new StringBuilder();
            for (int i = start; i < end; i++) {
                if (i > start) {
                    chunk.append(" ");
                }
                chunk.append(words[i]);
            }
            
            chunks.add(chunk.toString());
            
            // Move start position with overlap
            start += chunkSize - chunkOverlap;
            
            // Avoid infinite loop
            if (start >= words.length) {
                break;
            }
        }
        
        log.debug("Chunked text of {} words into {} chunks", words.length, chunks.size());
        return chunks;
    }
    
    /**
     * Estimate the number of chunks for a given text.
     */
    public int estimateChunkCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        
        String[] words = text.split("\\s+");
        if (words.length <= chunkSize) {
            return 1;
        }
        
        int stride = chunkSize - chunkOverlap;
        return (int) Math.ceil((double) (words.length - chunkSize) / stride) + 1;
    }
}
