package com.searchlight.e2e;

import com.searchlight.domain.model.DocumentChunk;
import com.searchlight.domain.ports.Indexer;
import com.searchlight.fixtures.SampleDocs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end smoke test with real components.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class EndToEndSmokeTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private Indexer indexer;
    
    @BeforeEach
    void setUp() {
        // Clear index and add sample documents
        indexer.clearAll();
        
        List<DocumentChunk> docs = SampleDocs.createSampleDocuments(384);
        indexer.indexBatch(docs);
    }
    
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("searchlight"));
    }
    
    @Test
    void testSearchWithKeywords() throws Exception {
        String searchRequest = """
                {
                    "q": "machine learning",
                    "k": 5,
                    "alpha": 0.0
                }
                """;
        
        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }
    
    @Test
    void testGetDocumentById() throws Exception {
        mockMvc.perform(get("/api/v1/docs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));
    }
    
    @Test
    void testAdminStats() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentCount").value(5));
    }
}
