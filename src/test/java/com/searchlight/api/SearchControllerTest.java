package com.searchlight.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchlight.api.dto.SearchRequest;
import com.searchlight.domain.model.SearchQuery;
import com.searchlight.domain.model.SearchResult;
import com.searchlight.domain.ports.EmbeddingProvider;
import com.searchlight.domain.ports.Searcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.searchlight.app.SearchlightApplication;

@SpringBootTest(classes = SearchlightApplication.class)
@AutoConfigureMockMvc
class SearchControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private Searcher searcher;
    
    @MockBean
    private EmbeddingProvider embeddingProvider;
    
    @Test
    void testSearch() throws Exception {
        SearchResult mockResult = SearchResult.builder()
                .id("1")
                .title("Test Document")
                .snippet("This is a test")
                .score(0.95f)
                .timestamp(Instant.now())
                .build();
        
        when(searcher.search(any(SearchQuery.class))).thenReturn(List.of(mockResult));
        when(embeddingProvider.embed(any())).thenReturn(new float[384]);
        
        SearchRequest request = new SearchRequest();
        request.setQ("test query");
        request.setK(10);
        request.setAlpha(0.5f);
        
        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].title").value("Test Document"))
                .andExpect(jsonPath("$.total").value(1));
    }
}
