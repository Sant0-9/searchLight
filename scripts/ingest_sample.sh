#!/bin/bash

# Script to ingest sample documents for testing

set -e

API_URL="${API_URL:-http://localhost:8080/api/v1}"

echo "🔦 Searchlight - Sample Data Ingestion"
echo "======================================"
echo ""

# Check if API is running
echo "Checking API health..."
if ! curl -sf "${API_URL}/health" > /dev/null; then
    echo "❌ API is not running at ${API_URL}"
    echo "   Start it with: make dev  or  docker-compose up"
    exit 1
fi

echo "✅ API is running"
echo ""

# Ingest RSS feeds
echo "📥 Ingesting RSS feeds..."
curl -X POST "${API_URL}/admin/ingest" \
    -H "Content-Type: application/json" \
    -d '{
        "urls": [
            "https://news.ycombinator.com/rss"
        ],
        "mode": "RSS",
        "source": "hacker-news"
    }' | jq

echo ""
echo "📊 Index statistics:"
curl -s "${API_URL}/admin/stats" | jq

echo ""
echo "✅ Sample data ingestion complete!"
echo ""
echo "Try searching:"
echo "  curl -X POST ${API_URL}/search \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"q\": \"programming\", \"k\": 5, \"alpha\": 0.5}' | jq"
