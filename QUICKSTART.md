# Quick Start Guide

Get the Todo gRPC application running in under 5 minutes!

## 1. Build and Run

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The application starts on:
- **gRPC Server**: `localhost:9090`
- **Actuator**: `http://localhost:8081`

## 2. Install grpcurl (if not already installed)

**macOS:**
```bash
brew install grpcurl
```

**Linux:**
```bash
wget https://github.com/fullstorydev/grpcurl/releases/download/v1.9.1/grpcurl_1.9.1_linux_x86_64.tar.gz
tar -xvf grpcurl_1.9.1_linux_x86_64.tar.gz
sudo mv grpcurl /usr/local/bin/
```

**Windows (Chocolatey):**
```bash
choco install grpcurl
```

## 3. Try It Out!

### Create a Todo
```bash
grpcurl -plaintext -d '{
  "title": "Learn gRPC",
  "description": "Complete the Spring gRPC tutorial"
}' localhost:9090 todo.TodoService/CreateTodo
```

**Response:**
```json
{
  "todo": {
    "id": "1",
    "title": "Learn gRPC",
    "description": "Complete the Spring gRPC tutorial",
    "completed": false,
    "createdAt": "1702500000000",
    "updatedAt": "1702500000000"
  }
}
```

### List All Todos
```bash
grpcurl -plaintext -d '{}' localhost:9090 todo.TodoService/ListTodos
```

### Get a Specific Todo
```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9090 todo.TodoService/GetTodo
```

### Mark Todo as Complete
```bash
grpcurl -plaintext -d '{
  "id": 1,
  "completed": true
}' localhost:9090 todo.TodoService/UpdateTodo
```

### Delete a Todo
```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9090 todo.TodoService/DeleteTodo
```

## 4. Check Health and Metrics

### gRPC Health Check
```bash
grpcurl -plaintext localhost:9090 grpc.health.v1.Health/Check
```

### HTTP Health Check
```bash
curl http://localhost:8081/actuator/health | jq
```

### View Metrics
```bash
curl http://localhost:8081/actuator/metrics | jq
```

### Prometheus Metrics
```bash
curl http://localhost:8081/actuator/prometheus
```

## 5. Explore Services

### List All Available Services
```bash
grpcurl -plaintext localhost:9090 list
```

**Output:**
```
grpc.health.v1.Health
grpc.reflection.v1alpha.ServerReflection
todo.TodoService
```

### Describe the Todo Service
```bash
grpcurl -plaintext localhost:9090 describe todo.TodoService
```

## 6. Run Tests

```bash
./gradlew test
```

View test report at: `build/reports/tests/test/index.html`

## 7. Run with Docker

```bash
# Build image
docker build -t spring-grpc-server .

# Run container
docker run -p 9090:9090 -p 8081:8081 spring-grpc-server
```

Or use Docker Compose for full observability stack:

```bash
docker-compose up -d
```

Access:
- **gRPC**: `localhost:9090`
- **Actuator**: `http://localhost:8081`
- **Prometheus**: `http://localhost:9091`
- **Grafana**: `http://localhost:3000` (admin/admin)

## Next Steps

- Read the full [README.md](README.md) for detailed gRPC concepts
- Explore the proto file at `src/main/proto/todo.proto`
- Check the service implementation at `src/main/java/com/example/springgrpcserver/service/TodoGrpcService.java`
- Review comprehensive tests at `src/test/java/com/example/springgrpcserver/service/TodoGrpcServiceTest.java`

## Troubleshooting

### Port Already in Use
```properties
# In application.properties, change:
spring.grpc.server.port=9091
```

### Can't Connect with grpcurl
Ensure server reflection is enabled (already configured):
```properties
spring.grpc.server.reflection.enabled=true
```

### View Database Contents
Access H2 console at: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:tododb`
- **Username**: `sa`
- **Password**: (empty)
