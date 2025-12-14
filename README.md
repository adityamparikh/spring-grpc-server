# Spring gRPC Todo Application

A comprehensive CRUD Todo application built with **Spring Boot 4** and **Spring gRPC 1.0 GA**, demonstrating modern gRPC service development with full observability, health checks, and native image support.

## Table of Contents

- [What is gRPC?](#what-is-grpc)
- [Key gRPC Concepts](#key-grpc-concepts)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Using grpcurl](#using-grpcurl)
- [Observability](#observability)
- [Health Checks](#health-checks)
- [Native Image Building](#native-image-building)
- [API Reference](#api-reference)

---

## What is gRPC?

**gRPC** (gRPC Remote Procedure Call) is a modern, high-performance RPC framework developed by Google. It's an alternative to REST APIs with several key advantages:

### gRPC vs REST

| Feature | gRPC | REST |
|---------|------|------|
| **Protocol** | HTTP/2 (binary) | HTTP/1.1 (text) |
| **Data Format** | Protocol Buffers (binary) | JSON (text) |
| **Performance** | Faster (binary, multiplexing) | Slower (text parsing) |
| **Type Safety** | Strong (proto definitions) | Weak (requires validation) |
| **Streaming** | Native bidirectional streaming | Limited (SSE, WebSocket) |
| **Browser Support** | Requires gRPC-Web | Native |
| **Human Readable** | No (binary) | Yes (JSON) |
| **Code Generation** | Automatic from .proto | Manual or OpenAPI |

### Why Use gRPC?

1. **Performance**: Binary serialization is 3-10x faster than JSON
2. **Type Safety**: Proto files define strict contracts, preventing runtime errors
3. **Code Generation**: Client/server code auto-generated in multiple languages
4. **Streaming**: Built-in support for client, server, and bidirectional streaming
5. **Backward Compatibility**: Protocol Buffers support schema evolution
6. **Language Agnostic**: Clients in Java can call services in Go, Python, etc.

### When to Use gRPC

- **Microservices**: High-performance inter-service communication
- **Real-time Systems**: Streaming data (IoT, monitoring, chat)
- **Mobile Apps**: Efficient bandwidth usage
- **Polyglot Environments**: Multiple programming languages

### When NOT to Use gRPC

- **Browser-only apps**: Limited browser support (use gRPC-Web or REST)
- **Simple CRUD APIs**: REST may be simpler
- **External public APIs**: JSON is more accessible

---

## Key gRPC Concepts

### 1. Protocol Buffers (.proto files)

Protocol Buffers (protobuf) is gRPC's Interface Definition Language (IDL). It defines:
- **Messages**: Data structures (like JSON objects)
- **Services**: RPC methods (like REST endpoints)
- **Field Numbers**: Binary identifiers (never change these!)

```protobuf
syntax = "proto3";

message Todo {
  int64 id = 1;        // Field number 1
  string title = 2;    // Field number 2
  bool completed = 3;  // Field number 3
}

service TodoService {
  rpc CreateTodo(CreateTodoRequest) returns (TodoResponse);
}
```

**Key Points**:
- Field numbers are used for binary encoding and must NEVER change
- Adding new fields is safe (backward compatible)
- Removing fields requires care (use `reserved` keyword)

### 2. Service Types

gRPC supports four RPC patterns:

```protobuf
service TodoService {
  // 1. Unary RPC: Single request → Single response (like REST)
  rpc GetTodo(GetTodoRequest) returns (TodoResponse);

  // 2. Server Streaming: Single request → Stream of responses
  rpc WatchTodos(WatchRequest) returns (stream TodoEvent);

  // 3. Client Streaming: Stream of requests → Single response
  rpc BatchCreate(stream CreateTodoRequest) returns (BatchResponse);

  // 4. Bidirectional Streaming: Stream ↔ Stream
  rpc Chat(stream ChatMessage) returns (stream ChatMessage);
}
```

This application uses **Unary RPCs** (standard request/response).

### 3. Generated Code

The protobuf compiler generates:

- **Message Classes**: Java POJOs for each message (immutable, builder pattern)
- **Service Base Classes**: Abstract classes to implement
- **Client Stubs**: Pre-built clients for calling services

```java
// Generated message (immutable)
Todo todo = Todo.newBuilder()
    .setId(1)
    .setTitle("Buy groceries")
    .build();

// Generated service base (implement this)
public class TodoGrpcService extends TodoServiceGrpc.TodoServiceImplBase {
    @Override
    public void createTodo(CreateTodoRequest request,
                          StreamObserver<TodoResponse> responseObserver) {
        // Your implementation
    }
}

// Generated client stub
TodoServiceBlockingStub client = TodoServiceGrpc.newBlockingStub(channel);
TodoResponse response = client.createTodo(request);
```

### 4. Error Handling

gRPC uses status codes similar to HTTP:

| gRPC Status | Description | When to Use |
|-------------|-------------|-------------|
| `OK` | Success | Implicit on `onCompleted()` |
| `INVALID_ARGUMENT` | Bad request data | Missing/invalid fields |
| `NOT_FOUND` | Resource doesn't exist | GetTodo(999) |
| `ALREADY_EXISTS` | Duplicate resource | Creating existing item |
| `PERMISSION_DENIED` | Authorization failed | Unauthorized access |
| `INTERNAL` | Server error | Unexpected exceptions |
| `UNAVAILABLE` | Service down | Temporary outage |

```java
// Returning an error
responseObserver.onError(
    Status.NOT_FOUND
        .withDescription("Todo with ID " + id + " not found")
        .asRuntimeException()
);
```

### 5. Spring gRPC Integration

Spring gRPC 1.0 provides:

- **@GrpcService**: Registers gRPC services (like `@RestController`)
- **Auto-configuration**: Server setup via `application.properties`
- **Dependency Injection**: Use Spring beans in gRPC services
- **Testing Support**: `@SpringBootTest` with embedded gRPC server
- **Observability**: Metrics, tracing, health checks

---

## Project Structure

```
spring-grpc-server/
├── src/main/
│   ├── proto/
│   │   └── todo.proto              # gRPC service definition
│   ├── java/com/example/springgrpcserver/
│   │   ├── entity/
│   │   │   └── TodoEntity.java     # JPA entity (database)
│   │   ├── repository/
│   │   │   └── TodoRepository.java # Spring Data JPA repository
│   │   ├── service/
│   │   │   └── TodoGrpcService.java # gRPC service implementation
│   │   ├── mapper/
│   │   │   └── TodoMapper.java     # Entity ↔ Proto converter
│   │   └── SpringGrpcServerApplication.java
│   └── resources/
│       └── application.properties   # Configuration (ports, database, observability)
└── src/test/
    └── java/com/example/springgrpcserver/
        └── service/
            └── TodoGrpcServiceTest.java # Integration tests
```

**Generated Code** (after build):
```
build/generated/source/proto/main/
├── java/com/example/springgrpcserver/proto/
│   ├── Todo.java                    # Message classes
│   ├── CreateTodoRequest.java
│   ├── TodoResponse.java
│   ├── ...
│   └── TodoServiceGrpc.java         # Service stub & base class
```

---

## Getting Started

### Prerequisites

- **Java 17+** (this project uses Java 25)
- **Gradle** (included via wrapper)
- **grpcurl** (for testing, install below)

### 1. Build the Project

```bash
# Build and generate protobuf classes
./gradlew build

# Run tests
./gradlew test
```

The build automatically:
1. Compiles `.proto` files to Java
2. Generates gRPC service stubs
3. Compiles Java code
4. Runs tests

### 2. Run the Application

```bash
./gradlew bootRun
```

The application starts:
- **gRPC Server**: `localhost:9090`
- **Actuator Endpoints**: `localhost:8081`
- **H2 Console**: `http://localhost:8080/h2-console`

You should see logs indicating:
```
Started gRPC server on port 9090
```

### 3. Verify Health

Check the application is healthy:

```bash
# HTTP health check
curl http://localhost:8081/actuator/health

# gRPC health check (with grpcurl)
grpcurl -plaintext localhost:9090 grpc.health.v1.Health/Check
```

---

## Using grpcurl

**grpcurl** is a command-line tool for interacting with gRPC servers (like `curl` for REST).

### Install grpcurl

**macOS**:
```bash
brew install grpcurl
```

**Linux**:
```bash
# Download from https://github.com/fullstorydev/grpcurl/releases
wget https://github.com/fullstorydev/grpcurl/releases/download/v1.9.1/grpcurl_1.9.1_linux_x86_64.tar.gz
tar -xvf grpcurl_1.9.1_linux_x86_64.tar.gz
sudo mv grpcurl /usr/local/bin/
```

**Windows** (using Chocolatey):
```bash
choco install grpcurl
```

### Discover Available Services

Server reflection must be enabled (already configured in `application.properties`):

```bash
# List all services
grpcurl -plaintext localhost:9090 list

# Output:
# grpc.health.v1.Health
# grpc.reflection.v1alpha.ServerReflection
# todo.TodoService

# Describe a service
grpcurl -plaintext localhost:9090 describe todo.TodoService

# Describe a message type
grpcurl -plaintext localhost:9090 describe todo.Todo
```

### CRUD Operations with grpcurl

#### 1. Create a Todo

```bash
grpcurl -plaintext -d '{
  "title": "Buy groceries",
  "description": "Milk, eggs, bread, cheese"
}' localhost:9090 todo.TodoService/CreateTodo
```

**Response**:
```json
{
  "todo": {
    "id": "1",
    "title": "Buy groceries",
    "description": "Milk, eggs, bread, cheese",
    "completed": false,
    "createdAt": "1702500000000",
    "updatedAt": "1702500000000"
  }
}
```

**Explanation**:
- `-plaintext`: No TLS (for local development)
- `-d`: JSON request data
- Response shows auto-generated ID and timestamps

#### 2. Get a Todo by ID

```bash
grpcurl -plaintext -d '{
  "id": 1
}' localhost:9090 todo.TodoService/GetTodo
```

**Error handling example** (non-existent ID):
```bash
grpcurl -plaintext -d '{"id": 999}' localhost:9090 todo.TodoService/GetTodo

# Output:
# ERROR:
#   Code: NotFound
#   Message: Todo with ID 999 not found
```

#### 3. List All Todos

```bash
# List all todos
grpcurl -plaintext -d '{
  "pageSize": 10,
  "pageNumber": 0
}' localhost:9090 todo.TodoService/ListTodos
```

**Response**:
```json
{
  "todos": [
    {
      "id": "1",
      "title": "Buy groceries",
      "completed": false,
      ...
    },
    {
      "id": "2",
      "title": "Write documentation",
      "completed": false,
      ...
    }
  ],
  "totalCount": 2
}
```

#### 4. Filter Todos by Completion Status

```bash
# List only completed todos
grpcurl -plaintext -d '{
  "completed": true,
  "pageSize": 10
}' localhost:9090 todo.TodoService/ListTodos

# List only incomplete todos
grpcurl -plaintext -d '{
  "completed": false
}' localhost:9090 todo.TodoService/ListTodos
```

#### 5. Update a Todo

```bash
# Update title and mark as completed
grpcurl -plaintext -d '{
  "id": 1,
  "title": "Groceries purchased",
  "completed": true
}' localhost:9090 todo.TodoService/UpdateTodo
```

**Partial updates**:
```bash
# Only update completion status (title/description unchanged)
grpcurl -plaintext -d '{
  "id": 1,
  "completed": true
}' localhost:9090 todo.TodoService/UpdateTodo
```

**Note**: Fields not included in the request are NOT modified (proto3 optional fields).

#### 6. Delete a Todo

```bash
grpcurl -plaintext -d '{
  "id": 1
}' localhost:9090 todo.TodoService/DeleteTodo
```

**Response**:
```json
{
  "success": true,
  "message": "Todo deleted successfully"
}
```

### Advanced grpcurl Usage

#### Pretty Print JSON

```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9090 todo.TodoService/GetTodo | jq
```

#### Save Response to File

```bash
grpcurl -plaintext -d '{}' localhost:9090 todo.TodoService/ListTodos > todos.json
```

#### Use Request from File

```bash
# Create request.json
echo '{"title": "From file", "description": "Test"}' > request.json

grpcurl -plaintext -d @ localhost:9090 todo.TodoService/CreateTodo < request.json
```

#### Verbose Output (see metadata)

```bash
grpcurl -plaintext -v -d '{"id": 1}' localhost:9090 todo.TodoService/GetTodo
```

---

## Observability

Spring Boot Actuator provides production-ready observability features.

### Health Checks

**HTTP Health Endpoint**:
```bash
curl http://localhost:8081/actuator/health | jq
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500068036608,
        "free": 100068036608,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**gRPC Health Check**:
```bash
# Check overall health
grpcurl -plaintext localhost:9090 grpc.health.v1.Health/Check

# Check specific service
grpcurl -plaintext -d '{"service": "todo.TodoService"}' \
  localhost:9090 grpc.health.v1.Health/Check
```

### Metrics

Spring gRPC automatically records metrics for all RPC calls.

**View Available Metrics**:
```bash
curl http://localhost:8081/actuator/metrics | jq
```

**gRPC-specific Metrics**:
```bash
# Total RPC calls
curl http://localhost:8081/actuator/metrics/grpc.server.requests | jq

# RPC duration
curl http://localhost:8081/actuator/metrics/grpc.server.duration | jq
```

**Prometheus Format** (for Prometheus/Grafana):
```bash
curl http://localhost:8081/actuator/prometheus
```

### Distributed Tracing

The application includes Micrometer tracing with Brave.

To visualize traces, integrate with:
- **Zipkin**: `docker run -d -p 9411:9411 openzipkin/zipkin`
- **Jaeger**: `docker run -d -p 16686:16686 jaegertracing/all-in-one`

Add to `application.properties`:
```properties
management.tracing.sampling.probability=1.0
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans
```

### Logging

Application logs show:
- SQL queries and parameters
- gRPC method invocations
- Request/response details

Configure logging in `application.properties`:
```properties
logging.level.com.example.springgrpcserver=DEBUG
logging.level.io.grpc=INFO
```

---

## Health Checks

### Spring Boot Actuator Health

Actuator provides:
1. **Database Health**: Checks database connectivity
2. **Disk Space Health**: Ensures sufficient disk space
3. **Custom Health Indicators**: Add your own checks

**Add Custom Health Check**:

```java
@Component
public class TodoServiceHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Check if service is functioning
        return Health.up()
            .withDetail("todoCount", todoRepository.count())
            .build();
    }
}
```

### gRPC Health Protocol

gRPC defines a standard health check protocol (`grpc.health.v1.Health`).

**Uses**:
- **Kubernetes Liveness/Readiness Probes**
- **Load Balancer Health Checks**
- **Service Mesh Health**

**Kubernetes Example**:
```yaml
livenessProbe:
  exec:
    command: ["/bin/grpc_health_probe", "-addr=:9090"]
  initialDelaySeconds: 10
```

---

## Docker Deployment

The application includes Docker configuration for easy deployment.

### Build Docker Image

```bash
docker build -t spring-grpc-server .
```

### Run with Docker

```bash
# Run the gRPC server
docker run -p 9090:9090 -p 8081:8081 spring-grpc-server

# Test the server
grpcurl -plaintext localhost:9090 list
```

### Run with Docker Compose

Docker Compose sets up the complete stack including Prometheus and Grafana for observability:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f grpc-server

# Stop all services
docker-compose down
```

**Services**:
- **gRPC Server**: http://localhost:9090 (gRPC), http://localhost:8081 (Actuator)
- **Prometheus**: http://localhost:9091
- **Grafana**: http://localhost:3000 (admin/admin)

### Grafana Dashboard Setup

1. Open Grafana at http://localhost:3000
2. Login with admin/admin
3. Add Prometheus data source: http://prometheus:9090
4. Import Spring Boot dashboard (ID: 4701)
5. View gRPC metrics, JVM stats, and more

---

## Native Image Building

Spring Boot 4 has excellent support for GraalVM Native Images.

### What is Native Image?

GraalVM Native Image compiles Java applications to **native executables**:
- **Fast Startup**: ~50ms vs 2-3 seconds JVM
- **Low Memory**: ~30MB vs 200MB+ JVM
- **No JIT Warmup**: Peak performance immediately

### Build Native Image

**Prerequisites**:
- GraalVM 22+ with Native Image

**Install GraalVM** (using SDKMAN):
```bash
sdk install java 22.0.0-graal
gu install native-image
```

**Build Native Executable**:
```bash
./gradlew nativeCompile
```

**Run Native Image**:
```bash
./build/native/nativeCompile/spring-grpc-server
```

### Docker Native Image

**Create Dockerfile**:
```dockerfile
FROM ghcr.io/graalvm/native-image:22 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew nativeCompile

FROM gcr.io/distroless/base
COPY --from=builder /app/build/native/nativeCompile/spring-grpc-server /app
ENTRYPOINT ["/app"]
```

**Build and Run**:
```bash
docker build -t spring-grpc-server:native .
docker run -p 9090:9090 -p 8081:8081 spring-grpc-server:native
```

### Native Image Considerations

**What Works**:
- gRPC services (fully supported)
- Spring Data JPA
- Actuator metrics
- Reflection (configured automatically)

**Potential Issues**:
- Dynamic class loading (use `@RegisterReflectionForBinding`)
- JNI (requires configuration)

**Test Native Image**:
```bash
./gradlew nativeTest
```

---

## API Reference

### CreateTodo

Creates a new todo item.

**Request**:
```protobuf
message CreateTodoRequest {
  string title = 1;        // Required
  string description = 2;  // Optional
}
```

**Response**:
```protobuf
message TodoResponse {
  Todo todo = 1;
}
```

**Example**:
```bash
grpcurl -plaintext -d '{"title": "New Todo"}' localhost:9090 todo.TodoService/CreateTodo
```

**Errors**:
- `INVALID_ARGUMENT`: Empty title

---

### GetTodo

Retrieves a todo by ID.

**Request**:
```protobuf
message GetTodoRequest {
  int64 id = 1;  // Required
}
```

**Response**:
```protobuf
message TodoResponse {
  Todo todo = 1;
}
```

**Example**:
```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9090 todo.TodoService/GetTodo
```

**Errors**:
- `NOT_FOUND`: Todo doesn't exist

---

### ListTodos

Lists todos with optional filtering and pagination.

**Request**:
```protobuf
message ListTodosRequest {
  optional bool completed = 1;  // Filter by status
  int32 page_size = 2;          // Items per page
  int32 page_number = 3;        // Page number (0-indexed)
}
```

**Response**:
```protobuf
message ListTodosResponse {
  repeated Todo todos = 1;
  int32 total_count = 2;
}
```

**Example**:
```bash
# All todos
grpcurl -plaintext -d '{}' localhost:9090 todo.TodoService/ListTodos

# Completed only
grpcurl -plaintext -d '{"completed": true}' localhost:9090 todo.TodoService/ListTodos

# Pagination
grpcurl -plaintext -d '{"pageSize": 5, "pageNumber": 0}' localhost:9090 todo.TodoService/ListTodos
```

---

### UpdateTodo

Updates an existing todo (partial updates supported).

**Request**:
```protobuf
message UpdateTodoRequest {
  int64 id = 1;                    // Required
  optional string title = 2;       // Optional
  optional string description = 3; // Optional
  optional bool completed = 4;     // Optional
}
```

**Response**:
```protobuf
message TodoResponse {
  Todo todo = 1;
}
```

**Example**:
```bash
# Update title only
grpcurl -plaintext -d '{"id": 1, "title": "Updated"}' localhost:9090 todo.TodoService/UpdateTodo

# Mark as completed
grpcurl -plaintext -d '{"id": 1, "completed": true}' localhost:9090 todo.TodoService/UpdateTodo
```

**Errors**:
- `NOT_FOUND`: Todo doesn't exist
- `INVALID_ARGUMENT`: Empty title

---

### DeleteTodo

Deletes a todo by ID.

**Request**:
```protobuf
message DeleteTodoRequest {
  int64 id = 1;  // Required
}
```

**Response**:
```protobuf
message DeleteTodoResponse {
  bool success = 1;
  string message = 2;
}
```

**Example**:
```bash
grpcurl -plaintext -d '{"id": 1}' localhost:9090 todo.TodoService/DeleteTodo
```

**Errors**:
- `NOT_FOUND`: Todo doesn't exist

---

## Configuration

### application.properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.grpc.server.port` | `9090` | gRPC server port |
| `spring.grpc.server.reflection.enabled` | `true` | Enable server reflection (for grpcurl) |
| `spring.grpc.server.health.enabled` | `true` | Enable gRPC health check service |
| `management.server.port` | `8081` | Actuator HTTP port |
| `management.tracing.sampling.probability` | `1.0` | Trace sampling rate (0.0-1.0) |

See [`application.properties`](src/main/resources/application.properties) for full configuration.

---

## Development Tips

### 1. Auto-reload on Code Changes

Use Spring DevTools for automatic restarts:

```gradle
dependencies {
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}
```

### 2. View Database Contents

Access H2 console at `http://localhost:8080/h2-console`:
- **JDBC URL**: `jdbc:h2:mem:tododb`
- **Username**: `sa`
- **Password**: (empty)

### 3. Debug gRPC with Logging

Enable detailed gRPC logging:
```properties
logging.level.io.grpc.netty=TRACE
```

### 4. Test with Postman

Postman supports gRPC (requires proto files or server reflection).

---

## Troubleshooting

### "Service Not Found"

**Error**: `Code: Unimplemented, Message: Method not found`

**Solution**: Ensure server reflection is enabled:
```properties
spring.grpc.server.reflection.enabled=true
```

### Port Already in Use

**Error**: `Address already in use: bind`

**Solution**: Change port in `application.properties`:
```properties
spring.grpc.server.port=9091
```

### Proto Changes Not Applied

**Solution**: Rebuild the project:
```bash
./gradlew clean build
```

---

## Further Reading

- [Spring gRPC Documentation](https://github.com/spring-projects/spring-grpc)
- [gRPC Official Docs](https://grpc.io/docs/)
- [Protocol Buffers Guide](https://protobuf.dev/)
- [grpcurl GitHub](https://github.com/fullstorydev/grpcurl)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html)

---

## License

MIT License - See LICENSE file for details.
