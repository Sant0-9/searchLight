# Deployment Guide

## Deployment Options

### 1. Docker Compose (Development/Small Scale)

**Best for**: Development, testing, small deployments (<10K docs)

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f api

# Stop services
docker-compose down
```

**Services**:
- API (port 8080)
- Embedder (port 8000)
- Dashboard (port 3000)
- Prometheus (port 9090)
- Grafana (port 3001)

### 2. Standalone JAR (Production Single Instance)

**Best for**: Simple production deployments, single server

```bash
# Build
./gradlew bootJar

# Run
java -Xmx2g -Xms1g \
  -Dspring.profiles.active=prod \
  -jar build/libs/searchlight-0.1.0.jar
```

**Configuration**:
```bash
export SEARCHLIGHT_EMBEDDING_PROVIDER=http
export SEARCHLIGHT_EMBEDDING_URL=http://embedder:8000/embed
export SEARCHLIGHT_INDEX_PATH=/var/lib/searchlight/index
```

### 3. Kubernetes (Production Cluster)

**Best for**: High availability, scaling, multi-tenant

Create deployment manifests:

**api-deployment.yaml**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: searchlight-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: searchlight-api
  template:
    metadata:
      labels:
        app: searchlight-api
    spec:
      containers:
      - name: api
        image: ghcr.io/yourusername/searchlight/api:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SEARCHLIGHT_EMBEDDING_URL
          value: "http://embedder-service:8000/embed"
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        volumeMounts:
        - name: index-data
          mountPath: /app/data
        livenessProbe:
          httpGet:
            path: /api/v1/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/v1/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
      volumes:
      - name: index-data
        persistentVolumeClaim:
          claimName: searchlight-index-pvc
```

**service.yaml**:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: searchlight-api
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
  selector:
    app: searchlight-api
```

Deploy:
```bash
kubectl apply -f k8s/
kubectl get pods
kubectl logs -f deployment/searchlight-api
```

### 4. Cloud Platforms

#### AWS

**Option A: ECS Fargate**
```bash
# Build and push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account>.dkr.ecr.us-east-1.amazonaws.com
docker build -t searchlight-api .
docker tag searchlight-api:latest <account>.dkr.ecr.us-east-1.amazonaws.com/searchlight-api:latest
docker push <account>.dkr.ecr.us-east-1.amazonaws.com/searchlight-api:latest

# Create ECS task definition and service
# Use EFS for shared index storage
```

**Option B: EKS**
- Use Kubernetes manifests above
- EBS volumes for index storage
- ALB ingress controller

#### GCP

**Cloud Run** (simplest):
```bash
gcloud run deploy searchlight-api \
  --image gcr.io/PROJECT_ID/searchlight-api \
  --platform managed \
  --region us-central1 \
  --memory 2Gi \
  --cpu 2 \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod
```

**GKE** (for scale):
- Use Kubernetes manifests
- Persistent Disks for index
- Cloud Load Balancing

#### Azure

**Azure Container Instances**:
```bash
az container create \
  --resource-group searchlight \
  --name searchlight-api \
  --image <registry>/searchlight-api:latest \
  --cpu 2 --memory 4 \
  --ports 8080 \
  --environment-variables SPRING_PROFILES_ACTIVE=prod
```

**AKS** (Kubernetes):
- Use Kubernetes manifests
- Azure Disks for storage

## Configuration Management

### Environment Variables

Required:
```bash
SPRING_PROFILES_ACTIVE=prod
SEARCHLIGHT_EMBEDDING_PROVIDER=http
SEARCHLIGHT_EMBEDDING_URL=http://embedder:8000/embed
```

Optional:
```bash
SEARCHLIGHT_INDEX_PATH=/var/lib/searchlight/index
SEARCHLIGHT_EMBEDDING_DIMENSION=384
SEARCHLIGHT_CHUNKER_SIZE=512
JAVA_OPTS="-Xmx2g -Xms1g"
```

### Secrets Management

**Kubernetes Secrets**:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: searchlight-secrets
type: Opaque
data:
  embedding-api-key: <base64-encoded-key>
```

**AWS Secrets Manager**:
```bash
aws secretsmanager create-secret \
  --name searchlight/embedding-api-key \
  --secret-string "your-api-key"
```

### Configuration Files

Mount custom `application.yaml`:

**Docker**:
```bash
docker run -v /path/to/application.yaml:/app/config/application.yaml searchlight-api
```

**Kubernetes**:
```yaml
volumeMounts:
- name: config
  mountPath: /app/config
volumes:
- name: config
  configMap:
    name: searchlight-config
```

## Persistence

### Index Storage

**Local Filesystem** (single instance):
- Simple, fast
- Use SSD for best performance
- Backup regularly

**Network Storage** (multi-instance read):
- NFS, EFS, Azure Files
- Mount read-only on searcher instances
- Write from single indexer instance

**Object Storage** (S3, GCS):
- Export index snapshots
- Load on startup
- Good for disaster recovery

### Backup Strategy

**Automated Backups**:
```bash
#!/bin/bash
# backup-index.sh

DATE=$(date +%Y%m%d-%H%M%S)
tar -czf /backups/index-${DATE}.tar.gz /var/lib/searchlight/index

# Upload to S3
aws s3 cp /backups/index-${DATE}.tar.gz s3://my-backups/searchlight/

# Keep last 7 days
find /backups -name "index-*.tar.gz" -mtime +7 -delete
```

**Cron**:
```bash
0 2 * * * /usr/local/bin/backup-index.sh
```

## Monitoring

### Health Checks

**Docker Compose**:
```yaml
healthcheck:
  test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/api/v1/health"]
  interval: 30s
  timeout: 10s
  retries: 3
```

**Kubernetes**:
```yaml
livenessProbe:
  httpGet:
    path: /api/v1/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /api/v1/health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
```

### Metrics

**Prometheus Scraping**:
```yaml
scrape_configs:
  - job_name: 'searchlight'
    static_configs:
      - targets: ['api:8080']
    metrics_path: '/actuator/prometheus'
```

**Grafana Dashboards**:
- Import `config/grafana/dashboards/searchlight.json`
- Or use dashboard ID: (create and share)

### Logging

**Centralized Logging**:

**ELK Stack**:
```bash
# Logstash input
input {
  file {
    path => "/var/log/searchlight/*.log"
    type => "searchlight"
  }
}
```

**Cloud Logging**:
- AWS CloudWatch Logs
- GCP Cloud Logging
- Azure Monitor Logs

## Scaling

### Vertical Scaling

Increase resources:
```yaml
resources:
  requests:
    memory: "4Gi"
    cpu: "2000m"
  limits:
    memory: "8Gi"
    cpu: "4000m"
```

### Horizontal Scaling

**Read Replicas**:
1. Share index via network storage (read-only)
2. Scale searcher instances
3. Load balance requests

**Separate Indexer/Searcher**:
```yaml
# Indexer (single instance)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: searchlight-indexer
spec:
  replicas: 1
  # ... indexer config with read-write volume

# Searcher (multiple instances)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: searchlight-searcher
spec:
  replicas: 5
  # ... searcher config with read-only volume
```

## Security

### Network Security

**Firewall Rules**:
- Allow 8080 from load balancer only
- Restrict admin endpoints

**TLS/SSL**:
```yaml
# Kubernetes ingress with cert-manager
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: searchlight
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - searchlight.example.com
    secretName: searchlight-tls
  rules:
  - host: searchlight.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: searchlight-api
            port:
              number: 80
```

### Authentication

Add Spring Security:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Configure API keys or OAuth2.

## Troubleshooting

### Common Issues

**Out of Memory**:
```bash
# Increase heap size
JAVA_OPTS="-Xmx4g -Xms2g"
```

**Slow Searches**:
- Check index size
- Monitor disk I/O
- Increase page cache
- Reduce `k` parameter

**Index Corruption**:
```bash
# Restore from backup
tar -xzf backup.tar.gz -C /var/lib/searchlight/
```

**Embedding Service Timeout**:
- Increase timeout in config
- Scale embedding service
- Use batch endpoints

## Performance Tuning

### JVM Options

```bash
JAVA_OPTS="
  -Xmx4g
  -Xms2g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+UseStringDeduplication
  -XX:+AlwaysPreTouch
"
```

### Lucene Tuning

```yaml
searchlight:
  index:
    hnsw:
      m: 32              # Higher = better recall, more memory
      ef-construction: 200  # Higher = better quality, slower indexing
```

### OS Tuning

```bash
# Increase file descriptors
ulimit -n 65536

# Increase max map count (for Lucene)
sysctl -w vm.max_map_count=262144

# Disable swap
swapoff -a
```
