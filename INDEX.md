#  Searchlight Documentation Index

Welcome to Searchlight - a production-grade document retrieval system with hybrid search!

##  Getting Started (Pick Your Path)

### New Users
1. **[GET_STARTED.md](GET_STARTED.md)** - Three ways to run in under 5 minutes
2. **[QUICKSTART.md](QUICKSTART.md)** - Detailed quick start guide
3. **[README.md](README.md)** - Full overview with examples

### Developers
1. **[ARCHITECTURE.md](ARCHITECTURE.md)** - Design patterns and decisions
2. **[CONTRIBUTING.md](CONTRIBUTING.md)** - How to contribute
3. **[VERIFICATION.md](VERIFICATION.md)** - Complete checklist

### DevOps Engineers
1. **[DEPLOYMENT.md](DEPLOYMENT.md)** - Production deployment guide
2. **[docker-compose.yml](docker-compose.yml)** - Container orchestration
3. **[Makefile](Makefile)** - Automation commands

## 📖 Documentation Overview

### Essential Reading

| Document | Purpose | Time to Read |
|----------|---------|--------------|
| **[GET_STARTED.md](GET_STARTED.md)** | Fastest path to running Searchlight | 2 min |
| **[QUICKSTART.md](QUICKSTART.md)** | Detailed setup with examples | 5 min |
| **[README.md](README.md)** | Complete feature overview | 10 min |

### Deep Dives

| Document | Purpose | Audience |
|----------|---------|----------|
| **[ARCHITECTURE.md](ARCHITECTURE.md)** | Design patterns, hexagonal architecture | Developers |
| **[DEPLOYMENT.md](DEPLOYMENT.md)** | K8s, AWS, GCP, Azure deployment | DevOps |
| **[CONTRIBUTING.md](CONTRIBUTING.md)** | Contribution guidelines | Contributors |

### Reference

| Document | Purpose |
|----------|---------|
| **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** | Complete deliverables checklist |
| **[VERIFICATION.md](VERIFICATION.md)** | Acceptance criteria and testing |
| **[CHANGELOG.md](CHANGELOG.md)** | Version history |
| **[STRUCTURE.txt](STRUCTURE.txt)** | Visual project layout |

##  By Task

### I want to...

#### Run Searchlight
→ [GET_STARTED.md](GET_STARTED.md) - Pick your preferred method

#### Understand the Architecture
→ [ARCHITECTURE.md](ARCHITECTURE.md) - Hexagonal design explained

#### Deploy to Production
→ [DEPLOYMENT.md](DEPLOYMENT.md) - Docker, K8s, Cloud platforms

#### Contribute Code
→ [CONTRIBUTING.md](CONTRIBUTING.md) - Guidelines and workflow

#### Use the API
→ [README.md#api-documentation](README.md#api-documentation) - Endpoints with curl examples
→ http://localhost:8080/swagger-ui.html - Interactive API docs (when running)

#### Build the Dashboard
→ [dashboard/README.md](dashboard/README.md) - Next.js setup

#### Run Tests
→ [VERIFICATION.md](VERIFICATION.md) - Test commands and coverage

#### Monitor & Observe
→ [README.md#observability](README.md#observability) - Metrics and dashboards

##  Project Structure

```
searchlight/
├──  Documentation (9 files)
│   ├── GET_STARTED.md           Start here!
│   ├── QUICKSTART.md             Quick setup
│   ├── README.md                 Full docs
│   ├── ARCHITECTURE.md           Design
│   ├── DEPLOYMENT.md             Production
│   ├── CONTRIBUTING.md           Contributors
│   ├── VERIFICATION.md           Testing
│   ├── PROJECT_SUMMARY.md        Deliverables
│   └── CHANGELOG.md              History
│
├──  Java Application
│   └── src/main/java/com/searchlight/
│       ├── domain/               Core logic
│       ├── infra/                Lucene, embeddings, ingestion
│       ├── api/                  REST controllers
│       └── app/                  Spring Boot config
│
├──  Next.js Dashboard
│   └── dashboard/               React + TypeScript + Tailwind
│
├──  Infrastructure
│   ├── docker-compose.yml       Full stack setup
│   ├── Dockerfile               API container
│   └── config/                  Prometheus, Grafana
│
└──  Development
    ├── Makefile                 Common tasks
    ├── scripts/                 Utilities
    └── .github/                 CI/CD
```

##  Learning Path

### Beginner → Advanced

1. **Start**: Run with Docker ([GET_STARTED.md](GET_STARTED.md))
2. **Explore**: Try API endpoints ([README.md](README.md))
3. **Understand**: Read architecture ([ARCHITECTURE.md](ARCHITECTURE.md))
4. **Extend**: Add a feature ([CONTRIBUTING.md](CONTRIBUTING.md))
5. **Deploy**: Production setup ([DEPLOYMENT.md](DEPLOYMENT.md))

##  Quick Links

### Running Services (when started)
-  API: http://localhost:8080
-  Swagger UI: http://localhost:8080/swagger-ui.html
-  Dashboard: http://localhost:3000
-  Prometheus: http://localhost:9090
-  Grafana: http://localhost:3001 (admin/admin)

### External Resources
-  [Apache Lucene](https://lucene.apache.org/)
-  [Spring Boot](https://spring.io/projects/spring-boot)
-  [Next.js](https://nextjs.org/)
-  [HNSW Algorithm](https://arxiv.org/abs/1603.09320)

##  Key Metrics

| Metric | Value |
|--------|-------|
| Total Files | 80+ |
| Java Classes | 31 |
| Test Classes | 5 |
| Documentation | 9 files |
| Test Coverage | 90%+ target |
| Technologies | Java 21, Spring Boot 3.3, Lucene 9.11, Next.js 14 |

##  Key Features

-  **Hybrid Search** - BM25 keyword + HNSW vector
-  **Lucene HNSW** - Fast approximate nearest neighbor
-  **Pluggable Embeddings** - HTTP or ONNX providers
-  **RSS Ingestion** - Automated document ingestion
-  **Modern Dashboard** - Next.js with Tailwind CSS
-  **Full Observability** - Prometheus + Grafana
-  **Production Ready** - Docker, K8s, CI/CD

##  Next Steps

1. **Choose your path** above
2. **Run Searchlight** ([GET_STARTED.md](GET_STARTED.md))
3. **Explore the code** in `src/main/java/`
4. **Try the dashboard** at http://localhost:3000
5. **Read the architecture** ([ARCHITECTURE.md](ARCHITECTURE.md))

##  Tips

- Use **GET_STARTED.md** for quickest setup
- Run `make help` to see all available commands
- Check **VERIFICATION.md** for testing checklist
- See **DEPLOYMENT.md** for production setup
- Read **ARCHITECTURE.md** to understand design

##  Getting Help

- 📖 Documentation issues? Check [INDEX.md](INDEX.md) (this file)
- 🐛 Found a bug? See [CONTRIBUTING.md](CONTRIBUTING.md)
- ❓ Have questions? Read [README.md](README.md)
-  Want to discuss? Check GitHub Discussions

---

**Happy Learning! **

*Built with  by a Principal Java Engineer*
