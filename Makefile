.PHONY: help build test clean run dev docker-build docker-up docker-down ingest bench

# Default target
help:
	@echo "Searchlight - Production-grade document retrieval system"
	@echo ""
	@echo "Available targets:"
	@echo "  make build        - Build the application"
	@echo "  make test         - Run tests"
	@echo "  make coverage     - Generate coverage report"
	@echo "  make run          - Run the application (prod mode)"
	@echo "  make dev          - Run in development mode"
	@echo "  make demo         - Run hybrid search demo (ingest + search with alpha=0/1)"
	@echo "  make clean        - Clean build artifacts"
	@echo "  make docker-build - Build Docker images"
	@echo "  make docker-up    - Start all services with docker-compose"
	@echo "  make docker-down  - Stop all services"
	@echo "  make ingest       - Ingest sample RSS feeds"
	@echo "  make bench        - Run k6 load test"
	@echo "  make dashboard    - Build and run dashboard"

# Build the application
build:
	./gradlew clean build

# Run tests
test:
	./gradlew test

# Generate coverage report
coverage:
	./gradlew jacocoTestReport
	@echo "Coverage report: build/reports/jacoco/test/html/index.html"

# Clean build artifacts
clean:
	./gradlew clean
	rm -rf data/index

# Run application in production mode
run:
	./gradlew bootRun

# Run application in development mode
dev:
	./gradlew bootRun --args='--spring.profiles.active=dev'

# Build Docker images
docker-build:
	docker-compose build

# Start all services
docker-up:
	docker-compose up -d
	@echo "Services starting..."
	@echo "API: http://localhost:8080"
	@echo "Dashboard: http://localhost:3000"
	@echo "Prometheus: http://localhost:9090"
	@echo "Grafana: http://localhost:3001 (admin/admin)"
	@echo "Swagger UI: http://localhost:8080/swagger-ui.html"

# Stop all services
docker-down:
	docker-compose down

# Full reset
docker-reset:
	docker-compose down -v
	docker-compose up -d --build

# Ingest sample RSS feeds
ingest:
	@echo "Ingesting sample RSS feeds..."
	curl -X POST http://localhost:8080/api/v1/admin/ingest \
		-H "Content-Type: application/json" \
		-d '{"urls": ["https://news.ycombinator.com/rss"], "mode": "RSS"}'

# Run load test with k6
bench:
	@if command -v k6 >/dev/null 2>&1; then \
		k6 run scripts/k6_search.js; \
	else \
		echo "k6 not installed. Install from https://k6.io/"; \
	fi

# Build and run dashboard
dashboard:
	cd dashboard && npm install && npm run build && npm start

# Show logs
logs:
	docker-compose logs -f

# Show stats
stats:
	curl http://localhost:8080/api/v1/admin/stats | jq

# Health check
health:
	curl http://localhost:8080/api/v1/health | jq

# Run complete demo: ingest + search with different alpha values
demo:
	@echo "🔦 Searchlight Hybrid Search Demo"
	@echo "===================================="
	@echo ""
	@echo "⏳ Waiting for API to be ready..."
	@bash -c 'for i in {1..30}; do curl -sf http://localhost:8080/api/v1/health >/dev/null && break || sleep 1; done'
	@echo "✅ API is ready"
	@echo ""
	@echo "📥 Ingesting sample RSS feed (Hacker News)..."
	@curl -X POST http://localhost:8080/api/v1/admin/ingest \
		-H "Content-Type: application/json" \
		-d '{"urls": ["https://news.ycombinator.com/rss"], "mode": "RSS"}' | jq -r '.message // .error // .'
	@sleep 2
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "🔍 Search 1: Pure Keyword (alpha=0.0, BM25 only)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@curl -X POST http://localhost:8080/api/v1/search \
		-H "Content-Type: application/json" \
		-d '{"q": "programming", "k": 5, "alpha": 0.0}' 2>/dev/null | \
		jq -r '.results[]? | "  [\(.score | tostring | .[0:4])] \(.title)"'
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "🔍 Search 2: Pure Vector (alpha=1.0, KNN only)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@curl -X POST http://localhost:8080/api/v1/search \
		-H "Content-Type: application/json" \
		-d '{"q": "programming", "k": 5, "alpha": 1.0}' 2>/dev/null | \
		jq -r '.results[]? | "  [\(.score | tostring | .[0:4])] \(.title)"'
	@echo ""
	@echo "✅ Demo complete! Rankings differ based on alpha parameter."
	@echo "   Try adjusting alpha between 0.0 and 1.0 for hybrid search."
