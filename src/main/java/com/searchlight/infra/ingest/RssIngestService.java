package com.searchlight.infra.ingest;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.searchlight.domain.model.DocumentChunk;
import com.searchlight.domain.model.SourceDoc;
import com.searchlight.domain.ports.EmbeddingProvider;
import com.searchlight.domain.ports.Indexer;
import com.searchlight.infra.util.IdCodec;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for ingesting documents from RSS feeds and URLs.
 */
@Slf4j
@Service
public class RssIngestService {
    
    private final HtmlCleaner htmlCleaner;
    private final Chunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final Indexer indexer;
    private final Counter ingestCounter;
    private final Counter errorCounter;
    
    public RssIngestService(
            HtmlCleaner htmlCleaner,
            Chunker chunker,
            EmbeddingProvider embeddingProvider,
            Indexer indexer,
            MeterRegistry meterRegistry) {
        this.htmlCleaner = htmlCleaner;
        this.chunker = chunker;
        this.embeddingProvider = embeddingProvider;
        this.indexer = indexer;
        this.ingestCounter = meterRegistry.counter("ingest.documents");
        this.errorCounter = meterRegistry.counter("ingest.errors");
    }
    
    /**
     * Ingest documents from RSS feed URLs.
     */
    public int ingestRssFeeds(List<String> feedUrls) {
        int totalIngested = 0;
        
        for (String feedUrl : feedUrls) {
            try {
                log.info("Ingesting RSS feed: {}", feedUrl);
                totalIngested += ingestRssFeed(feedUrl);
            } catch (Exception e) {
                log.error("Failed to ingest RSS feed: {}", feedUrl, e);
                errorCounter.increment();
            }
        }
        
        indexer.commit();
        log.info("Ingested {} documents from {} feeds", totalIngested, feedUrls.size());
        return totalIngested;
    }
    
    /**
     * Ingest a single RSS feed.
     */
    private int ingestRssFeed(String feedUrl) throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(feedUrl)));
        
        int count = 0;
        for (SyndEntry entry : feed.getEntries()) {
            try {
                String url = entry.getLink();
                if (url == null || url.isBlank()) {
                    continue;
                }
                
                log.debug("Ingesting entry: {}", url);
                ingestUrl(url, feed.getTitle());
                count++;
                ingestCounter.increment();
                
            } catch (Exception e) {
                log.warn("Failed to ingest entry: {}", entry.getLink(), e);
                errorCounter.increment();
            }
        }
        
        return count;
    }
    
    /**
     * Ingest a single URL.
     */
    public void ingestUrl(String url, String source) {
        try {
            log.debug("Fetching URL: {}", url);
            
            // Fetch HTML
            org.jsoup.nodes.Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; Searchlight/1.0)")
                    .timeout(30000)
                    .get();
            
            String html = doc.html();
            String title = htmlCleaner.extractTitle(html);
            String cleanText = htmlCleaner.clean(html);
            
            // Create source document
            SourceDoc sourceDoc = SourceDoc.builder()
                    .id(IdCodec.generateId())
                    .url(url)
                    .title(title)
                    .content(cleanText)
                    .htmlContent(html)
                    .source(source != null ? source : "web")
                    .publishedAt(Instant.now())
                    .fetchedAt(Instant.now())
                    .contentType("text/html")
                    .build();
            
            // Chunk and index
            processAndIndex(sourceDoc);
            
        } catch (IOException e) {
            log.error("Failed to fetch URL: {}", url, e);
            errorCounter.increment();
            throw new RuntimeException("Failed to fetch URL", e);
        }
    }
    
    /**
     * Process a source document: chunk, embed, and index.
     */
    private void processAndIndex(SourceDoc sourceDoc) {
        List<String> chunks = chunker.chunk(sourceDoc.getContent());
        
        if (chunks.isEmpty()) {
            log.warn("No chunks generated for document: {}", sourceDoc.getUrl());
            return;
        }
        
        log.debug("Generated {} chunks for document: {}", chunks.size(), sourceDoc.getUrl());
        
        // Embed chunks
        List<float[]> embeddings = embeddingProvider.embedBatch(chunks);
        
        // Create and index document chunks
        List<DocumentChunk> documentChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            float[] embedding = embeddings.get(i);
            
            // Extract keywords (simple: split on whitespace and take first 20 words)
            String[] words = chunkText.split("\\s+");
            String[] keywords = new String[Math.min(20, words.length)];
            System.arraycopy(words, 0, keywords, 0, keywords.length);
            
            DocumentChunk chunk = DocumentChunk.builder()
                    .id(IdCodec.generateId())
                    .sourceId(sourceDoc.getId())
                    .title(sourceDoc.getTitle())
                    .url(sourceDoc.getUrl())
                    .content(chunkText)
                    .vector(embedding)
                    .keywords(keywords)
                    .timestamp(sourceDoc.getPublishedAt())
                    .source(sourceDoc.getSource())
                    .chunkIndex(i)
                    .contentHash(Integer.toString(chunkText.hashCode()))
                    .build();
            
            documentChunks.add(chunk);
        }
        
        // Index all chunks
        indexer.indexBatch(documentChunks);
        
        log.info("Indexed {} chunks for document: {}", documentChunks.size(), sourceDoc.getTitle());
    }
}
