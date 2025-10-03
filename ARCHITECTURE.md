# Searchlight Architecture

## Overview

Searchlight follows a **hexagonal architecture** (ports and adapters) pattern, ensuring clean separation of concerns and testability.

## Core Principles

1. **Domain-Driven Design**: Business logic isolated in the domain layer
2. **Dependency Inversion**: Domain depends on abstractions (ports), not implementations
3. **Pluggability**: Easy to swap implementations (e.g., embedding providers)
4. **Testability**: Each layer can be tested independently

## Layer Breakdown

### 1. Domain Layer (`com.searchlight.domain`)

**Purpose**: Contains core business logic and abstractions

**Components**:
- `model/`: Core entities
  - `DocumentChunk`: Indexed document with vector
  - `SearchQuery`: Search parameters
  - `SearchResult`: Search result with scoring
  - `SourceDoc`: Original document before chunking

- `ports/`: Interfaces defining contracts
  - `EmbeddingProvider`: Generate embeddings from text
  - `Indexer`: Index documents
  - `Searcher`: Search indexed documents

**Dependencies**: None (pure domain logic)

### 2. Infrastructure Layer (`com.searchlight.infra`)

**Purpose**: Implement domain ports with concrete technologies

**Components**:

#### Indexing (`infra/index`)
- `LuceneIndexer`: Implements `Indexer` using Apache Lucene
  - HNSW vector indexing
  - Full-text indexing with BM25
  - Document management (add, delete, commit)

- `LuceneSearcher`: Implements `Searcher`
  - Hybrid search (keyword + vector)
  - Query parsing and execution
  - Result scoring and ranking

- `SimilarityMode`: Enum for vector similarity functions

#### Embeddings (`infra/embeddings`)
- `HttpEmbeddingProvider`: Remote embedding service
  - HTTP client with configurable endpoint
  - Batch embedding support
  - Metrics instrumentation

- `OnnxEmbeddingProvider`: Local ONNX Runtime
  - Stub mode for offline development
  - TODO: Real model inference

#### Ingestion (`infra/ingest`)
- `RssIngestService`: Orchestrates ingestion pipeline
  - Fetches RSS feeds
  - Fetches article content
  - Coordinates chunking and indexing

- `HtmlCleaner`: Extract clean text from HTML
  - Remove scripts, styles, navigation
  - Extract title and main content

- `Chunker`: Split text into smaller pieces
  - Token-based chunking (~512 tokens)
  - Overlap for context preservation

### 3. Application Layer (`com.searchlight.app`)

**Purpose**: Application configuration and startup

**Components**:
- `SearchlightApplication`: Spring Boot entry point
- `config/OpenAPIConfig`: Swagger/OpenAPI setup
- `config/ObservabilityConfig`: Metrics and monitoring

### 4. API Layer (`com.searchlight.api`)

**Purpose**: HTTP interface to the system

**Components**:

#### Controllers
- `SearchController`: Search and document retrieval
- `AdminController`: Ingestion and management
- `HealthController`: Health checks

#### DTOs (Data Transfer Objects)
- `SearchRequest/Response`: Search API contracts
- `IngestRequest/Response`: Ingestion API contracts

## Data Flow

### Search Flow

```
1. HTTP Request → SearchController
2. Parse SearchRequest DTO
3. Build SearchQuery (domain model)
4. Call EmbeddingProvider (if needed)
5. Call Searcher.search()
6. LuceneSearcher executes hybrid query
7. Convert results to SearchResponse DTO
8. Return HTTP Response
```

### Ingestion Flow

```
1. HTTP Request → AdminController
2. Parse IngestRequest DTO
3. Call RssIngestService
4. Fetch RSS feed items
5. For each item:
   a. Fetch HTML content
   b. Clean HTML → HtmlCleaner
   c. Chunk text → Chunker
   d. Generate embeddings → EmbeddingProvider
   e. Index chunks → Indexer
6. Commit index
7. Return IngestResponse DTO
```

## Technology Choices

### Why Lucene?

- **Mature**: Battle-tested for 20+ years
- **HNSW Support**: Native approximate nearest neighbor search
- **Performance**: Highly optimized for search workloads
- **Flexibility**: Complete control over indexing and querying
- **No External Dependencies**: Embedded library

### Why Spring Boot?

- **Ecosystem**: Rich set of libraries and integrations
- **Observability**: Built-in metrics, health checks, tracing
- **Configuration**: Flexible profiles and property management
- **Testing**: Excellent test support with MockMvc, etc.

### Why Hexagonal Architecture?

- **Testability**: Mock ports, test domain logic independently
- **Flexibility**: Swap Lucene for Elasticsearch without touching domain
- **Clarity**: Clear boundaries between layers
- **Maintainability**: Changes isolated to specific layers

## Scalability Considerations

### Current Design (Single Node)
- Lucene index on local filesystem
- In-memory searcher
- Single application instance

### Future Scaling Options

1. **Horizontal Scaling with Shared Index**
   - NFS or S3 for index storage
   - Read-only replicas
   - Load balancer in front

2. **Distributed Search**
   - Replace Lucene with Elasticsearch
   - Keep domain layer unchanged
   - Implement new `Searcher` adapter

3. **Async Ingestion**
   - Message queue for ingestion jobs
   - Separate ingestion workers
   - Event-driven updates

4. **Caching**
   - Redis for frequent queries
   - Embeddings cache
   - Query result cache

## Security Considerations

### Current State
- No authentication/authorization (MVP)
- All endpoints public
- Suitable for internal/trusted networks

### Production Recommendations
1. Add Spring Security
2. API keys or OAuth2
3. Rate limiting
4. Input validation (already present)
5. HTTPS/TLS
6. Audit logging

## Monitoring & Observability

### Metrics (Prometheus)
- Request rates and latencies
- Index size and document count
- Embedding provider performance
- JVM metrics (heap, GC, threads)

### Logging (Logback)
- Structured logging
- Log levels by package
- File rotation

### Tracing (OpenTelemetry)
- Ready for distributed tracing
- Automatic instrumentation hooks

## Extension Points

### Adding a New Embedding Provider

1. Implement `EmbeddingProvider` interface
2. Add Spring `@Component` annotation
3. Use `@ConditionalOnProperty` for config-based activation
4. Add configuration properties
5. Write tests

### Adding a New Search Backend

1. Implement `Indexer` and `Searcher` interfaces
2. Keep domain models unchanged
3. Wire up in configuration
4. All existing code continues to work

### Adding a New Ingestion Source

1. Create service in `infra/ingest`
2. Use existing `HtmlCleaner` and `Chunker`
3. Call `Indexer` to store
4. Add controller endpoint if needed

## Performance Characteristics

### Indexing
- **Throughput**: ~100-500 docs/sec (depends on embedding provider)
- **Latency**: Dominated by embedding generation
- **Memory**: Scales with batch size and vector dimension

### Search
- **Throughput**: ~200-1000 QPS (depends on index size, k)
- **Latency**: 
  - Keyword only: ~10-50ms
  - Vector only: ~20-100ms (HNSW)
  - Hybrid: ~30-150ms
- **Memory**: Index fits in page cache for best performance

### Disk Usage
- ~1-10 KB per document chunk (depends on content size)
- Vector overhead: dimension * 4 bytes (float32)
- Example: 1M docs, 384 dims = ~1.5 GB vectors + text
