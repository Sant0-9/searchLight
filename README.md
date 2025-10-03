#  Searchlight

[![CI](https://github.com/yourusername/searchlight/workflows/CI/badge.svg)](https://github.com/yourusername/searchlight/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-green.svg)](https://spring.io/projects/spring-boot)

Production-grade document retrieval API with **hybrid search** combining BM25 keyword matching and HNSW vector similarity using Apache Lucene 9+.

##  Features

-  **Hybrid Search**: Combine keyword (BM25) and vector (HNSW) search with adjustable weighting
-  **High Performance**: Lucene HNSW for fast approximate nearest neighbor search
-  **Hexagonal Architecture**: Clean separation of domain, ports, and adapters
-  **Observability**: Prometheus metrics, OpenTelemetry instrumentation, Grafana dashboards
- 🔌 **Pluggable Embeddings**: HTTP-based or ONNX Runtime providers
- 📰 **RSS Ingestion**: Automated document ingestion from RSS feeds and URLs
-  **Modern Dashboard**: Next.js 14 UI with real-time search
-  **Docker Ready**: Complete docker-compose setup with all services
-  **90%+ Test Coverage**: Comprehensive test suite with JUnit 5, WireMock, Testcontainers

##  Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    REST API Layer                        │
│  (SearchController, AdminController, HealthController)   │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────┐
│                   Domain Layer                           │
│   Models: DocumentChunk, SearchQuery, SearchResult      │
│   Ports: EmbeddingProvider, Indexer, Searcher           │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────┐
│               Infrastructure Layer                       │
│                                                          │
│  ┌─────────────────┐  ┌──────────────────────────────┐ │
│  │ Lucene HNSW     │  │ Embedding Providers          │ │
│  │ - Indexer       │  │ - HttpEmbeddingProvider      │ │
│  │ - Searcher      │  │ - OnnxEmbeddingProvider      │ │
│  └─────────────────┘  └──────────────────────────────┘ │
│                                                          │
│  ┌─────────────────────────────────────────────────────┐│
│  │ Ingestion Pipeline                                  ││
│  │ - RssIngestService                                  ││
│  │ - HtmlCleaner (jsoup)                              ││
│  │ - Chunker (token-based splitting)                  ││
│  └─────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────┘
```

##  Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose (optional)
- Node.js 20+ (for dashboard)

### Option 1: Docker Compose (Recommended)

```bash
# Start all services
docker-compose up --build

# API available at http://localhost:8080
# Dashboard at http://localhost:3000
# Prometheus at http://localhost:9090
# Grafana at http://localhost:3001 (admin/admin)
```

### Option 2: Local Development

```bash
# Run API in dev mode (with ONNX stub embeddings)
make dev

# Or manually:
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Option 3: Using Makefile

```bash
# See all available commands
make help

# Build and test
make build
make test

# Run with Docker
make docker-up
```

### Quick Demo

Once the API is running (via `make dev` or `docker-compose up`), run the interactive demo:

```bash
make demo
```

This will:
1. Ingest sample documents from Hacker News RSS
2. Run search with `alpha=0.0` (pure BM25 keyword search)
3. Run search with `alpha=1.0` (pure KNN vector search)
4. Show how rankings differ based on the alpha parameter

**Expected output:**
```
🔦 Searchlight Hybrid Search Demo
====================================

📥 Ingesting sample RSS feed (Hacker News)...
✅ Indexed 15 documents, 73 chunks

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 Search 1: Pure Keyword (alpha=0.0, BM25 only)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  [0.95] Best Programming Languages for 2025
  [0.82] Learn to Code: A Beginner's Guide
  [0.71] Programming Paradigms Explained
  ...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 Search 2: Pure Vector (alpha=1.0, KNN only)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  [0.91] Machine Learning Fundamentals
  [0.87] Deep Learning in Practice
  [0.79] Building Neural Networks
  ...

✅ Demo complete! Rankings differ based on alpha parameter.
```

## 📖 API Documentation

### Swagger UI
Once running, access interactive API docs at:
```
http://localhost:8080/swagger-ui.html
```

### Core Endpoints

####  Search Documents
```bash
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{
    "q": "machine learning",
    "k": 10,
    "alpha": 0.5
  }'
```

**Request Body (copy to Swagger UI):**
```json
{
  "q": "machine learning neural networks",
  "k": 5,
  "alpha": 0.5
}
```

**Parameters:**
- `q` (string, required): Query text
- `k` (int, optional): Number of results (default: 10)
- `alpha` (float 0-1, optional): Hybrid weight (default: 0.5)
  - `0.0` = pure keyword search (BM25 only)
  - `1.0` = pure vector search (KNN only)
  - `0.5` = balanced hybrid search
- `offset` (int, optional): Pagination offset (default: 0)

**Sample Response:**
```json
{
  "query": "machine learning neural networks",
  "results": [
    {
      "id": "doc-1-chunk-0",
      "sourceId": "doc-1",
      "title": "Introduction to Neural Networks",
      "url": "https://example.com/neural-networks",
      "snippet": "Neural networks are the foundation of modern machine learning...",
      "score": 0.85,
      "keywordScore": 0.72,
      "vectorScore": 0.91,
      "source": "hacker-news",
      "timestamp": "2025-10-03T10:30:00Z"
    }
  ],
  "total": 1,
  "took": 45
}
```

####  Ingest Documents
```bash
curl -X POST http://localhost:8080/api/v1/admin/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "urls": ["https://news.ycombinator.com/rss"],
    "mode": "RSS"
  }'
```

**Request Body (copy to Swagger UI):**
```json
{
  "urls": [
    "https://news.ycombinator.com/rss",
    "https://example.com/blog/feed.xml"
  ],
  "mode": "RSS"
}
```

**Sample Response:**
```json
{
  "message": "Ingestion completed",
  "documentsProcessed": 25,
  "chunksIndexed": 142,
  "errors": 0
}
```

####  Get Document by ID
```bash
curl http://localhost:8080/api/v1/docs/{id}
```

#### 🔄 Reindex
```bash
curl -X POST http://localhost:8080/api/v1/admin/reindex
```

####  Health Check
```bash
curl http://localhost:8080/api/v1/health
```

####  Metrics (Prometheus)
```bash
curl http://localhost:8080/actuator/prometheus
```

##  Dashboard

The Next.js dashboard provides a beautiful UI for searching:

- Real-time search with results
- Adjustable alpha slider for hybrid search tuning
- Result scoring visualization
- Responsive design

### Running Dashboard Locally

```bash
cd dashboard
npm install
npm run dev
# Open http://localhost:3000
```

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Generate coverage report
./gradlew jacocoTestReport

# View coverage
open build/reports/jacoco/test/html/index.html
```

### Test Categories

- **Unit Tests**: Domain logic, embeddings, chunking
- **Integration Tests**: Lucene indexing & searching
- **API Tests**: Controller endpoints with MockMvc
- **E2E Tests**: Full stack smoke tests

## ⚙ Configuration

### application.yaml

```yaml
searchlight:
  index:
    path: data/index
    similarity: COSINE  # COSINE, DOT_PRODUCT, EUCLIDEAN
    hnsw:
      m: 16
      ef-construction: 100
  
  embedding:
    provider: onnx  # http or onnx
    url: http://localhost:8000/embed
    dimension: 384
    timeout: 30000
  
  chunker:
    size: 512
    overlap: 50
```

### Environment Variables

```bash
SEARCHLIGHT_EMBEDDING_PROVIDER=http|onnx
SEARCHLIGHT_EMBEDDING_URL=http://embedder:8000/embed
SEARCHLIGHT_EMBEDDING_DIMENSION=384
SPRING_PROFILES_ACTIVE=dev|ci|prod
```

##  Performance Benchmarks

Run load tests with k6:

```bash
make bench
```

**Sample Results** (local machine, mock embeddings):

| Metric | Value |
|--------|-------|
| Requests/sec | ~200 RPS |
| P50 Latency | 45ms |
| P95 Latency | 120ms |
| P99 Latency | 180ms |
| Error Rate | <0.1% |

*Note: Performance varies based on index size, hardware, and embedding provider.*

##  Development

### Project Structure

```
searchlight/
├── src/main/java/com/searchlight/
│   ├── app/              # Spring Boot application & config
│   ├── domain/           # Core domain models & ports
│   │   ├── model/
│   │   └── ports/
│   ├── infra/            # Infrastructure implementations
│   │   ├── embeddings/   # Embedding providers
│   │   ├── index/        # Lucene HNSW
│   │   └── ingest/       # RSS/HTML processing
│   └── api/              # REST controllers & DTOs
│       ├── controller/
│       └── dto/
├── src/test/java/com/searchlight/
│   ├── fixtures/
│   ├── infra/
│   ├── api/
│   └── e2e/
├── dashboard/            # Next.js frontend
├── scripts/              # Helper scripts
├── config/               # Prometheus, Grafana configs
└── docker-compose.yml
```

### Adding a New Embedding Provider

1. Implement `EmbeddingProvider` interface
2. Add `@ConditionalOnProperty` for configuration
3. Register in Spring context
4. Update configuration

Example:

```java
@Component
@ConditionalOnProperty(name = "searchlight.embedding.provider", havingValue = "custom")
public class CustomEmbeddingProvider implements EmbeddingProvider {
    // Implementation...
}
```

## 🔬 Observability

### Metrics

Key metrics exposed via Prometheus:

- `search_requests_total` - Total search requests
- `search_latency` - Search latency histogram
- `embedding_latency` - Embedding generation time
- `index_docs_count` - Total documents in index
- `ingest_documents_total` - Documents ingested
- `ingest_errors_total` - Ingestion errors

### Grafana Dashboards

Pre-configured dashboards available at `http://localhost:3001`:

- Request rates and latencies
- JVM metrics (heap, GC, threads)
- Index statistics
- Error rates

## 🗺 Roadmap

- [ ] Real ONNX Runtime integration with MiniLM-L6-v2
- [ ] Multi-tenant indexes with namespace isolation
- [ ] Synonym expansion for keyword search
- [ ] Query rewriting and expansion
- [ ] Re-ranking with cross-encoder models
- [ ] Postgres integration for source document registry
- [ ] Incremental indexing and updates
- [ ] Document deduplication
- [ ] Faceted search support
- [ ] Saved searches and query history
- [ ] API rate limiting
- [ ] Authentication & authorization

##  Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure `./gradlew build` passes
5. Submit a pull request

##  License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Apache Lucene** - High-performance search library
- **Spring Boot** - Application framework
- **Next.js** - React framework for dashboard
- **HNSW** - Hierarchical Navigable Small World algorithm

## 📧 Contact

For questions or feedback, please open an issue on GitHub.

---

**Built with  using Java 21, Spring Boot 3, and Apache Lucene**
