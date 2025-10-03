package com.searchlight.infra.ingest;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Cleans HTML content to extract plain text.
 */
@Slf4j
@Component
public class HtmlCleaner {
    
    /**
     * Extract clean text from HTML content.
     */
    public String clean(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        
        try {
            // Parse HTML
            Document doc = Jsoup.parse(html);
            
            // Remove script and style elements
            doc.select("script, style, nav, footer, header, aside").remove();
            
            // Get text content
            String text = doc.body().text();
            
            // Normalize whitespace
            text = text.replaceAll("\\s+", " ").trim();
            
            return text;
            
        } catch (Exception e) {
            log.warn("Failed to clean HTML, returning raw text", e);
            // Fallback: strip all HTML tags
            return Jsoup.clean(html, Safelist.none());
        }
    }
    
    /**
     * Extract title from HTML.
     */
    public String extractTitle(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        
        try {
            Document doc = Jsoup.parse(html);
            String title = doc.title();
            
            if (title == null || title.isBlank()) {
                // Try to get h1
                title = doc.select("h1").first() != null ? 
                        doc.select("h1").first().text() : "";
            }
            
            return title.trim();
            
        } catch (Exception e) {
            log.warn("Failed to extract title", e);
            return "";
        }
    }
}
