#  Getting Started with Searchlight

## Three Ways to Run

### 1⃣ Quick Demo (Docker - Recommended)

**Time: 3 minutes**

```bash
# Clone and start
git clone https://github.com/yourusername/searchlight.git
cd searchlight

# Start everything
docker-compose up -d

# Wait 30 seconds for services to start, then visit:
# - Dashboard: http://localhost:3000
# - API: http://localhost:8080/swagger-ui.html
# - Grafana: http://localhost:3001 (admin/admin)
```

### 2⃣ Development Mode (Local Java)

**Time: 2 minutes (after Java 21 installed)**

```bash
# Clone
git clone https://github.com/yourusername/searchlight.git
cd searchlight

# Run (uses stub embeddings, no external deps)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Or use Make
make dev

# API available at http://localhost:8080
```

### 3⃣ Full Build & Test

**Time: 5 minutes (first build)**

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Generate coverage report
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html

# Run
./gradlew bootRun
```

## First Steps

### 1. Check Health

```bash
curl http://localhost:8080/api/v1/health
```

Expected:
```json
{
  "status": "UP",
  "service": "searchlight",
  "documentCount": 0,
  "embeddingProvider": "onnx-stub",
  "embeddingDimension": 384
}
```

### 2. Ingest Sample Data

```bash
# Using the script
./scripts/ingest_sample.sh

# Or manually
curl -X POST http://localhost:8080/api/v1/admin/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "urls": ["https://news.ycombinator.com/rss"],
    "mode": "RSS"
  }'
```

### 3. Search

```bash
# Keyword search
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{"q": "programming", "k": 5, "alpha": 0.0}' | jq

# Hybrid search
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{"q": "programming", "k": 5, "alpha": 0.5}' | jq

# Vector search
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{"q": "programming", "k": 5, "alpha": 1.0}' | jq
```

### 4. Use the Dashboard

Open http://localhost:3000

1. Enter a search query
2. Adjust the alpha slider
3. See results with scores
4. Click to view source documents

## Common Tasks

### Ingest Your Own Content

```bash
# From URL
curl -X POST http://localhost:8080/api/v1/admin/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "urls": ["https://example.com/article"],
    "mode": "URL",
    "source": "my-docs"
  }'

# From RSS
curl -X POST http://localhost:8080/api/v1/admin/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "urls": ["https://blog.example.com/rss"],
    "mode": "RSS",
    "source": "blog"
  }'
```

### View Metrics

```bash
# Prometheus format
curl http://localhost:8080/actuator/prometheus

# Index stats
curl http://localhost:8080/api/v1/admin/stats | jq
```

### Clear Index

```bash
curl -X POST http://localhost:8080/api/v1/admin/reindex
```

## Development

### Run Tests

```bash
# All tests
./gradlew test

# Specific test
./gradlew test --tests LuceneIndexerSearcherTest

# With coverage
./gradlew jacocoTestReport
```

### Build Dashboard

```bash
cd dashboard
npm install
npm run dev
# Open http://localhost:3000
```

### Load Testing

```bash
# Install k6: https://k6.io
# Then run:
k6 run scripts/k6_search.js
```

## Troubleshooting

### Port 8080 in use

```bash
# Change in application.yaml
server:
  port: 8081
```

### Out of memory

```bash
# Increase heap
JAVA_OPTS="-Xmx2g" ./gradlew bootRun
```

### Can't find Gradle

```bash
# Use wrapper
./gradlew --version
```

### Docker issues

```bash
# Reset everything
docker-compose down -v
docker-compose up --build
```

## Next Steps

1. **Read the docs**: Start with [README.md](README.md)
2. **Explore the API**: Visit http://localhost:8080/swagger-ui.html
3. **Check the code**: Look at `src/main/java/com/searchlight/`
4. **Run the dashboard**: Try http://localhost:3000
5. **Deploy**: See [DEPLOYMENT.md](DEPLOYMENT.md)

## Need Help?

- 📖 [Full Documentation](README.md)
-  [Architecture Guide](ARCHITECTURE.md)
-  [Quick Start](QUICKSTART.md)
-  [Deployment Guide](DEPLOYMENT.md)
- 🐛 [Report Issues](https://github.com/yourusername/searchlight/issues)

---

**Happy searching! **
