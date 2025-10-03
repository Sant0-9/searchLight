package com.searchlight.app.config;

import com.searchlight.domain.ports.Indexer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ObservabilityConfig {
    
    private final MeterRegistry meterRegistry;
    private final Indexer indexer;
    
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }
    
    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }
    
    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }
    
    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }
    
    /**
     * Register custom Searchlight metrics on startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerCustomMetrics() {
        // Index document count gauge
        Gauge.builder("searchlight.index.documents", indexer, Indexer::getDocumentCount)
                .description("Total number of documents in the index")
                .register(meterRegistry);
        
        log.debug("Custom Searchlight metrics registered");
    }
}
