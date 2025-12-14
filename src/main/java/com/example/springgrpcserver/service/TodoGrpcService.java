package com.example.springgrpcserver.service;

import com.example.springgrpcserver.entity.TodoEntity;
import com.example.springgrpcserver.mapper.TodoMapper;
import com.example.springgrpcserver.proto.*;
import com.example.springgrpcserver.repository.TodoRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * gRPC service implementation for Todo CRUD operations.
 *
 * Key Spring gRPC concepts demonstrated here:
 *
 * 1. @GrpcService - Spring annotation that registers this as a gRPC service
 *    (similar to @RestController for REST APIs)
 *
 * 2. Extends TodoServiceGrpc.TodoServiceImplBase - Generated base class from proto file
 *    Provides method stubs for all RPC methods defined in the proto
 *
 * 3. StreamObserver - Callback interface for sending responses
 *    - onNext(): Send response data
 *    - onError(): Send error status
 *    - onCompleted(): Mark RPC as complete
 *
 * 4. Error Handling - gRPC uses Status codes (similar to HTTP status codes):
 *    - NOT_FOUND: Resource doesn't exist
 *    - INVALID_ARGUMENT: Invalid request parameters
 *    - INTERNAL: Server-side error
 *    - ALREADY_EXISTS: Duplicate resource
 *    etc.
 */
@GrpcService
public class TodoGrpcService extends TodoServiceGrpc.TodoServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(TodoGrpcService.class);

    private final TodoRepository todoRepository;
    private final TodoMapper todoMapper;

    /**
     * Constructor injection of dependencies.
     * Spring automatically injects the repository and mapper beans.
     */
    public TodoGrpcService(TodoRepository todoRepository, TodoMapper todoMapper) {
        this.todoRepository = todoRepository;
        this.todoMapper = todoMapper;
    }

    /**
     * Creates a new todo item.
     *
     * gRPC Flow:
     * 1. Client sends CreateTodoRequest
     * 2. Server validates and processes request
     * 3. Server calls responseObserver.onNext() with response
     * 4. Server calls responseObserver.onCompleted() to finish RPC
     *
     * @param request the create request containing title and description
     * @param responseObserver callback for sending the response
     */
    @Override
    @Transactional
    public void createTodo(CreateTodoRequest request, StreamObserver<TodoResponse> responseObserver) {
        try {
            logger.info("Creating new todo with title: {}", request.getTitle());

            // Validate request
            if (request.getTitle().trim().isEmpty()) {
                // Send error response using gRPC Status
                responseObserver.onError(
                    Status.INVALID_ARGUMENT
                        .withDescription("Title is required and cannot be empty")
                        .asRuntimeException()
                );
                return;
            }

            // Create and save entity
            TodoEntity entity = new TodoEntity(
                request.getTitle().trim(),
                request.getDescription().trim()
            );
            TodoEntity saved = todoRepository.save(entity);

            // Convert to proto and send response
            TodoResponse response = TodoResponse.newBuilder()
                .setTodo(todoMapper.toProto(saved))
                .build();

            // onNext() sends the response data to the client
            responseObserver.onNext(response);
            // onCompleted() marks the RPC as successfully completed
            responseObserver.onCompleted();

            logger.info("Successfully created todo with ID: {}", saved.getId());

        } catch (Exception e) {
            logger.error("Error creating todo", e);
            // Send internal error status
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Failed to create todo: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    /**
     * Retrieves a todo by ID.
     *
     * Demonstrates error handling for not found resources.
     */
    @Override
    @Transactional(readOnly = true)
    public void getTodo(GetTodoRequest request, StreamObserver<TodoResponse> responseObserver) {
        try {
            logger.info("Fetching todo with ID: {}", request.getId());

            Optional<TodoEntity> todoOpt = todoRepository.findById(request.getId());

            if (todoOpt.isEmpty()) {
                // Resource not found - send NOT_FOUND status
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("Todo with ID " + request.getId() + " not found")
                        .asRuntimeException()
                );
                return;
            }

            TodoResponse response = TodoResponse.newBuilder()
                .setTodo(todoMapper.toProto(todoOpt.get()))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error fetching todo", e);
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Failed to fetch todo: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    /**
     * Lists todos with optional filtering and pagination.
     *
     * Demonstrates:
     * - Optional field handling (hasCompleted())
     * - Pagination using Spring Data
     * - Repeated fields in protobuf (list of todos)
     */
    @Override
    @Transactional(readOnly = true)
    public void listTodos(ListTodosRequest request, StreamObserver<ListTodosResponse> responseObserver) {
        try {
            logger.info("Listing todos with filter - completed: {}, page: {}, size: {}",
                request.hasCompleted() ? request.getCompleted() : "all",
                request.getPageNumber(),
                request.getPageSize());

            // Setup pagination (default to page 0, size 10)
            int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
            int pageNumber = request.getPageNumber() >= 0 ? request.getPageNumber() : 0;
            Pageable pageable = PageRequest.of(pageNumber, pageSize);

            Page<TodoEntity> page;

            // Check if completed filter is set (using proto3 optional)
            if (request.hasCompleted()) {
                // Filter by completion status
                page = todoRepository.findByCompleted(request.getCompleted(), pageable);
            } else {
                // Return all todos
                page = todoRepository.findAll(pageable);
            }

            // Convert entities to proto messages
            List<Todo> todos = page.getContent().stream()
                .map(todoMapper::toProto)
                .toList();

            // Build response with list of todos and total count
            ListTodosResponse response = ListTodosResponse.newBuilder()
                .addAllTodos(todos) // addAll for repeated fields
                .setTotalCount((int) page.getTotalElements())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("Returned {} todos out of {} total", todos.size(), page.getTotalElements());

        } catch (Exception e) {
            logger.error("Error listing todos", e);
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Failed to list todos: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    /**
     * Updates an existing todo.
     *
     * Demonstrates partial updates using proto3 optional fields.
     * Only fields that are explicitly set in the request are updated.
     */
    @Override
    @Transactional
    public void updateTodo(UpdateTodoRequest request, StreamObserver<TodoResponse> responseObserver) {
        try {
            logger.info("Updating todo with ID: {}", request.getId());

            Optional<TodoEntity> todoOpt = todoRepository.findById(request.getId());

            if (todoOpt.isEmpty()) {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("Todo with ID " + request.getId() + " not found")
                        .asRuntimeException()
                );
                return;
            }

            TodoEntity entity = todoOpt.get();

            // Update only fields that are set in the request
            // hasXxx() methods are generated for optional fields
            if (request.hasTitle()) {
                if (request.getTitle().trim().isEmpty()) {
                    responseObserver.onError(
                        Status.INVALID_ARGUMENT
                            .withDescription("Title cannot be empty")
                            .asRuntimeException()
                    );
                    return;
                }
                entity.setTitle(request.getTitle().trim());
            }

            if (request.hasDescription()) {
                entity.setDescription(request.getDescription().trim());
            }

            if (request.hasCompleted()) {
                entity.setCompleted(request.getCompleted());
            }

            TodoEntity updated = todoRepository.save(entity);

            TodoResponse response = TodoResponse.newBuilder()
                .setTodo(todoMapper.toProto(updated))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("Successfully updated todo with ID: {}", updated.getId());

        } catch (Exception e) {
            logger.error("Error updating todo", e);
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Failed to update todo: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    /**
     * Deletes a todo by ID.
     *
     * Returns a simple success/failure response.
     */
    @Override
    @Transactional
    public void deleteTodo(DeleteTodoRequest request, StreamObserver<DeleteTodoResponse> responseObserver) {
        try {
            logger.info("Deleting todo with ID: {}", request.getId());

            if (!todoRepository.existsById(request.getId())) {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("Todo with ID " + request.getId() + " not found")
                        .asRuntimeException()
                );
                return;
            }

            todoRepository.deleteById(request.getId());

            DeleteTodoResponse response = DeleteTodoResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Todo deleted successfully")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("Successfully deleted todo with ID: {}", request.getId());

        } catch (Exception e) {
            logger.error("Error deleting todo", e);
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Failed to delete todo: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }
}
