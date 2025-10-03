# Contributing to Searchlight

Thank you for your interest in contributing to Searchlight! This document provides guidelines and instructions for contributing.

## Code of Conduct

Please be respectful and constructive in all interactions.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/yourusername/searchlight.git`
3. Create a feature branch: `git checkout -b feature/my-feature`
4. Make your changes
5. Run tests: `./gradlew test`
6. Commit your changes: `git commit -am 'Add new feature'`
7. Push to your fork: `git push origin feature/my-feature`
8. Create a Pull Request

## Development Setup

### Prerequisites
- Java 21+
- Docker (for integration tests)
- Node.js 20+ (for dashboard)

### Building
```bash
./gradlew build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew jacocoTestReport
```

## Code Style

- Follow standard Java conventions
- Use meaningful variable and method names
- Add JavaDoc for public APIs
- Keep methods focused and concise
- Maximum line length: 120 characters

## Testing Guidelines

- Write tests for all new features
- Maintain >90% code coverage for core packages
- Use meaningful test names describing what is being tested
- Follow Arrange-Act-Assert pattern
- Mock external dependencies

## Pull Request Process

1. Update documentation for any API changes
2. Add tests for new functionality
3. Ensure all tests pass: `./gradlew build`
4. Update CHANGELOG.md with your changes
5. Request review from maintainers

## Commit Message Format

Use conventional commits:

```
feat: add new embedding provider
fix: resolve index corruption issue
docs: update API documentation
test: add tests for chunker
refactor: improve search performance
```

## Questions?

Open an issue for discussion before starting major changes.
