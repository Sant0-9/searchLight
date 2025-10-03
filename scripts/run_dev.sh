#!/bin/bash

# Development startup script

set -e

echo "🚀 Starting Searchlight in development mode..."

# Ensure data directory exists
mkdir -p data/index

# Run with dev profile
./gradlew bootRun --args='--spring.profiles.active=dev'
