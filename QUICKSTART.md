#  Searchlight Quick Start

Get up and running with Searchlight in 5 minutes.

## Prerequisites

Choose one:
- **Option A**: Docker & Docker Compose (easiest)
- **Option B**: Java 21+

## Option A: Docker (Recommended)

### 1. Start All Services

```bash
# Clone and start
git clone https://github.com/yourusername/searchlight.git
cd searchlight
docker-compose up --build
```

Wait for services to start (~2 minutes first time). You'll see:

```
 API running at http://localhost:8080
 Dashboard at http://localhost:3000
 Prometheus at http://localhost:9090
 Grafana at http://localhost:3001
```

### 2. Verify Health

```bash
curl http://localhost:8080/api/v1/health
```

Expected response:
```json
{
  "status": "UP",
  "service": "searchlight",
  "documentCount": 0,
  "embeddingProvider": "onnx-stub"
}
```

### 3. Ingest Sample Data

```bash
curl -X POST http://localhost:8080/api/v1/admin/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "urls": ["https://news.ycombinator.com/rss"],
    "mode": "RSS"
  }'
```

This fetches the HN RSS feed and indexes articles (~1-2 minutes).

### 4. Search!

```bash
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{
    "q": "programming languages",
    "k": 5,
    "alpha": 0.5
  }' | jq
```

### 5. Use the Dashboard

Open http://localhost:3000 in your browser and start searching!

---

## Option B: Local Development

### 1. Build & Run

```bash
# Clone
git clone https://github.com/yourusername/searchlight.git
cd searchlight

# Run in dev mode (uses stub ONNX embeddings)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Or use Makefile
make dev
```

### 2. Verify

```bash
curl http://localhost:8080/api/v1/health
```

### 3. Ingest & Search

Same as Docker steps 3-4 above.

---

## Next Steps

### Explore the API

**Swagger UI**: http://localhost:8080/swagger-ui.html

**Key Endpoints**:
- `POST /api/v1/search` - Search documents
- `GET /api/v1/docs/{id}` - Get document by ID
- `POST /api/v1/admin/ingest` - Ingest new documents
- `GET /api/v1/admin/stats` - Index statistics
- `GET /actuator/prometheus` - Metrics

### Tune Hybrid Search

The `alpha` parameter controls keyword vs vector balance:

```bash
# Pure keyword search (BM25)
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{"q": "rust programming", "k": 5, "alpha": 0.0}'

# Balanced hybrid
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{"q": "rust programming", "k": 5, "alpha": 0.5}'

# Pure vector search
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{"q": "rust programming", "k": 5, "alpha": 1.0}'
```

### Monitor with Grafana

1. Open http://localhost:3001
2. Login: `admin` / `admin`
3. Go to Dashboards → Searchlight Metrics
4. View request rates, latencies, and index stats

### Run Load Tests

```bash
# Install k6 (if not installed)
# macOS: brew install k6
# Linux: https://k6.io/docs/getting-started/installation/

# Run benchmark
make bench

# Or directly
k6 run scripts/k6_search.js
```

### Run the Dashboard Locally

```bash
cd dashboard
npm install
npm run dev
```

Open http://localhost:3000

---

## Example Workflows

### 1. Ingest Your Own Documents

```bash
# Ingest from URL
curl -X POST http://localhost:8080/api/v1/admin/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "urls": ["https://example.com/article"],
    "mode": "URL",
    "source": "my-blog"
  }'

# Ingest from RSS feed
curl -X POST http://localhost:8080/api/v1/admin/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "urls": [
      "https://blog.example.com/rss",
      "https://another-blog.com/feed"
    ],
    "mode": "RSS",
    "source": "tech-blogs"
  }'
```

### 2. Search with Filters

```bash
# Filter by source
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{
    "q": "kubernetes",
    "k": 10,
    "alpha": 0.5,
    "filters": {
      "source": "tech-blogs"
    }
  }'

# Filter by date
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{
    "q": "kubernetes",
    "k": 10,
    "alpha": 0.5,
    "filters": {
      "after": "2025-01-01T00:00:00Z"
    }
  }'
```

### 3. Get Document Details

```bash
# Get document by ID
curl http://localhost:8080/api/v1/docs/{document-id}

# Get all chunks from a source document
curl http://localhost:8080/api/v1/docs/source/{source-id}
```

### 4. Check Index Stats

```bash
curl http://localhost:8080/api/v1/admin/stats | jq
```

Output:
```json
{
  "documentCount": 1234,
  "indexPath": "data/index"
}
```

---

## Troubleshooting

### Port Already in Use

```bash
# Change port in docker-compose.yml or application.yaml
# Or stop conflicting service:
lsof -ti:8080 | xargs kill -9
```

### Out of Memory

```bash
# Increase Docker memory (Docker Desktop → Settings → Resources)
# Or increase JVM heap:
JAVA_OPTS="-Xmx2g" ./gradlew bootRun
```

### Can't Connect to Embedding Service

The default setup uses a stub ONNX provider that runs locally. No external service needed!

To use the HTTP embedder:
```bash
# In application.yaml
searchlight.embedding.provider: http
searchlight.embedding.url: http://embedder:8000/embed
```

### Index Not Found

First time running? The index is created automatically on first ingest.

```bash
# Check if index directory exists
ls -la data/index

# If missing, ingest some data
make ingest
```

---

## What's Next?

1. **Read the [README](README.md)** for full documentation
2. **Check [ARCHITECTURE.md](ARCHITECTURE.md)** to understand the design
3. **See [DEPLOYMENT.md](DEPLOYMENT.md)** for production deployment
4. **Run tests**: `./gradlew test`
5. **Explore the code** in `src/main/java/com/searchlight/`

---

## Getting Help

- 📖 [Full Documentation](README.md)
- 🐛 [Report Issues](https://github.com/yourusername/searchlight/issues)
-  [Discussions](https://github.com/yourusername/searchlight/discussions)

---

**Happy Searching! **
