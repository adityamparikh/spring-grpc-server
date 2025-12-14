package com.example.springgrpcserver.service;

import com.example.springgrpcserver.config.GrpcTestConfiguration;
import com.example.springgrpcserver.proto.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for TodoGrpcService.
 *
 * Testing gRPC services with Spring Boot:
 *
 * 1. @SpringBootTest - Starts full Spring application context with embedded gRPC server
 * 2. @Import(GrpcTestConfiguration.class) - Imports test configuration for gRPC client setup
 * 3. TodoServiceGrpc.TodoServiceBlockingStub - Client stub for synchronous calls
 * 4. @DirtiesContext - Resets application context between tests (ensures clean database)
 *
 * Spring gRPC test support automatically:
 * - Starts a gRPC server on configured port
 * - Manages lifecycle (startup/shutdown)
 * - Provides channel factory for creating client connections
 */
@SpringBootTest(properties = {
    // Use in-memory H2 database for testing
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
@Import(GrpcTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TodoGrpcServiceTest {

    /**
     * Client stub configured by GrpcTestConfiguration.
     * BlockingStub = synchronous client (blocks until response received)
     * AsyncStub = asynchronous client (uses callbacks)
     * FutureStub = returns ListenableFuture for async operations
     */
    @Autowired
    private TodoServiceGrpc.TodoServiceBlockingStub todoServiceStub;

    /**
     * Test: Successfully create a new todo.
     *
     * Verifies:
     * - Request is processed correctly
     * - Response contains the created todo with generated ID
     * - Timestamps are set
     */
    @Test
    void testCreateTodo_Success() {
        // Arrange: Prepare request
        CreateTodoRequest request = CreateTodoRequest.newBuilder()
            .setTitle("Buy groceries")
            .setDescription("Milk, eggs, bread")
            .build();

        // Act: Call gRPC service
        TodoResponse response = todoServiceStub.createTodo(request);

        // Assert: Verify response
        assertThat(response).isNotNull();
        assertThat(response.getTodo()).isNotNull();
        assertThat(response.getTodo().getId()).isGreaterThan(0);
        assertThat(response.getTodo().getTitle()).isEqualTo("Buy groceries");
        assertThat(response.getTodo().getDescription()).isEqualTo("Milk, eggs, bread");
        assertThat(response.getTodo().getCompleted()).isFalse();
        assertThat(response.getTodo().getCreatedAt()).isGreaterThan(0);
        assertThat(response.getTodo().getUpdatedAt()).isGreaterThan(0);
    }

    /**
     * Test: Create todo with empty title fails.
     *
     * gRPC error handling testing:
     * - StatusRuntimeException is thrown for gRPC errors
     * - Extract Status to check error code and description
     */
    @Test
    void testCreateTodo_EmptyTitle_ThrowsInvalidArgument() {
        // Arrange
        CreateTodoRequest request = CreateTodoRequest.newBuilder()
            .setTitle("")  // Empty title
            .setDescription("This should fail")
            .build();

        // Act & Assert: Expect INVALID_ARGUMENT status
        assertThatThrownBy(() -> todoServiceStub.createTodo(request))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
            .isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    /**
     * Test: Retrieve an existing todo by ID.
     */
    @Test
    void testGetTodo_Success() {
        // Arrange: Create a todo first
        CreateTodoRequest createRequest = CreateTodoRequest.newBuilder()
            .setTitle("Test Todo")
            .setDescription("Test Description")
            .build();
        TodoResponse createResponse = todoServiceStub.createTodo(createRequest);
        long todoId = createResponse.getTodo().getId();

        // Act: Get the todo
        GetTodoRequest getRequest = GetTodoRequest.newBuilder()
            .setId(todoId)
            .build();
        TodoResponse getResponse = todoServiceStub.getTodo(getRequest);

        // Assert
        assertThat(getResponse.getTodo()).isNotNull();
        assertThat(getResponse.getTodo().getId()).isEqualTo(todoId);
        assertThat(getResponse.getTodo().getTitle()).isEqualTo("Test Todo");
        assertThat(getResponse.getTodo().getDescription()).isEqualTo("Test Description");
    }

    /**
     * Test: Get non-existent todo returns NOT_FOUND.
     */
    @Test
    void testGetTodo_NotFound_ThrowsNotFound() {
        // Arrange
        GetTodoRequest request = GetTodoRequest.newBuilder()
            .setId(999L)  // Non-existent ID
            .build();

        // Act & Assert
        assertThatThrownBy(() -> todoServiceStub.getTodo(request))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
            .isEqualTo(Status.Code.NOT_FOUND);
    }

    /**
     * Test: List all todos without filter.
     */
    @Test
    void testListTodos_NoFilter_ReturnsAll() {
        // Arrange: Create multiple todos
        todoServiceStub.createTodo(CreateTodoRequest.newBuilder()
            .setTitle("Todo 1")
            .build());
        todoServiceStub.createTodo(CreateTodoRequest.newBuilder()
            .setTitle("Todo 2")
            .build());
        todoServiceStub.createTodo(CreateTodoRequest.newBuilder()
            .setTitle("Todo 3")
            .build());

        // Act: List all todos
        ListTodosRequest request = ListTodosRequest.newBuilder()
            .setPageSize(10)
            .setPageNumber(0)
            .build();
        ListTodosResponse response = todoServiceStub.listTodos(request);

        // Assert
        assertThat(response.getTodosList()).hasSize(3);
        assertThat(response.getTotalCount()).isEqualTo(3);
    }

    /**
     * Test: List todos filtered by completion status.
     *
     * Demonstrates proto3 optional field usage.
     */
    @Test
    void testListTodos_FilterByCompleted() {
        // Arrange: Create todos with different completion status
        long todo1Id = todoServiceStub.createTodo(
            CreateTodoRequest.newBuilder().setTitle("Incomplete Todo").build()
        ).getTodo().getId();

        long todo2Id = todoServiceStub.createTodo(
            CreateTodoRequest.newBuilder().setTitle("Complete Todo").build()
        ).getTodo().getId();

        // Mark todo2 as completed
        todoServiceStub.updateTodo(UpdateTodoRequest.newBuilder()
            .setId(todo2Id)
            .setCompleted(true)
            .build());

        // Act: List only completed todos
        ListTodosRequest request = ListTodosRequest.newBuilder()
            .setCompleted(true)  // Filter by completed
            .build();
        ListTodosResponse response = todoServiceStub.listTodos(request);

        // Assert: Should only return completed todo
        assertThat(response.getTodosList()).hasSize(1);
        assertThat(response.getTodosList().get(0).getId()).isEqualTo(todo2Id);
        assertThat(response.getTodosList().get(0).getCompleted()).isTrue();
    }

    /**
     * Test: Pagination works correctly.
     */
    @Test
    void testListTodos_Pagination() {
        // Arrange: Create 5 todos
        for (int i = 1; i <= 5; i++) {
            todoServiceStub.createTodo(CreateTodoRequest.newBuilder()
                .setTitle("Todo " + i)
                .build());
        }

        // Act: Get first page (2 items)
        ListTodosRequest page1Request = ListTodosRequest.newBuilder()
            .setPageSize(2)
            .setPageNumber(0)
            .build();
        ListTodosResponse page1Response = todoServiceStub.listTodos(page1Request);

        // Act: Get second page (2 items)
        ListTodosRequest page2Request = ListTodosRequest.newBuilder()
            .setPageSize(2)
            .setPageNumber(1)
            .build();
        ListTodosResponse page2Response = todoServiceStub.listTodos(page2Request);

        // Assert
        assertThat(page1Response.getTodosList()).hasSize(2);
        assertThat(page1Response.getTotalCount()).isEqualTo(5);

        assertThat(page2Response.getTodosList()).hasSize(2);
        assertThat(page2Response.getTotalCount()).isEqualTo(5);

        // IDs should be different between pages
        assertThat(page1Response.getTodosList().get(0).getId())
            .isNotEqualTo(page2Response.getTodosList().get(0).getId());
    }

    /**
     * Test: Update todo successfully.
     *
     * Demonstrates partial updates using optional fields.
     */
    @Test
    void testUpdateTodo_Success() {
        // Arrange: Create a todo
        long todoId = todoServiceStub.createTodo(
            CreateTodoRequest.newBuilder()
                .setTitle("Original Title")
                .setDescription("Original Description")
                .build()
        ).getTodo().getId();

        // Act: Update only the title and completion status
        UpdateTodoRequest updateRequest = UpdateTodoRequest.newBuilder()
            .setId(todoId)
            .setTitle("Updated Title")
            .setCompleted(true)
            // Note: NOT updating description
            .build();
        TodoResponse response = todoServiceStub.updateTodo(updateRequest);

        // Assert: Title and completed changed, description unchanged
        assertThat(response.getTodo().getTitle()).isEqualTo("Updated Title");
        assertThat(response.getTodo().getCompleted()).isTrue();
        assertThat(response.getTodo().getDescription()).isEqualTo("Original Description");
    }

    /**
     * Test: Update non-existent todo returns NOT_FOUND.
     */
    @Test
    void testUpdateTodo_NotFound_ThrowsNotFound() {
        // Arrange
        UpdateTodoRequest request = UpdateTodoRequest.newBuilder()
            .setId(999L)
            .setTitle("Updated")
            .build();

        // Act & Assert
        assertThatThrownBy(() -> todoServiceStub.updateTodo(request))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
            .isEqualTo(Status.Code.NOT_FOUND);
    }

    /**
     * Test: Update todo with empty title fails.
     */
    @Test
    void testUpdateTodo_EmptyTitle_ThrowsInvalidArgument() {
        // Arrange: Create a todo
        long todoId = todoServiceStub.createTodo(
            CreateTodoRequest.newBuilder().setTitle("Original").build()
        ).getTodo().getId();

        // Act & Assert: Try to update with empty title
        UpdateTodoRequest request = UpdateTodoRequest.newBuilder()
            .setId(todoId)
            .setTitle("")
            .build();

        assertThatThrownBy(() -> todoServiceStub.updateTodo(request))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
            .isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    /**
     * Test: Delete todo successfully.
     */
    @Test
    void testDeleteTodo_Success() {
        // Arrange: Create a todo
        long todoId = todoServiceStub.createTodo(
            CreateTodoRequest.newBuilder().setTitle("To Delete").build()
        ).getTodo().getId();

        // Act: Delete the todo
        DeleteTodoRequest deleteRequest = DeleteTodoRequest.newBuilder()
            .setId(todoId)
            .build();
        DeleteTodoResponse response = todoServiceStub.deleteTodo(deleteRequest);

        // Assert: Deletion successful
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getMessage()).contains("successfully");

        // Verify todo is really deleted
        GetTodoRequest getRequest = GetTodoRequest.newBuilder()
            .setId(todoId)
            .build();
        assertThatThrownBy(() -> todoServiceStub.getTodo(getRequest))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
            .isEqualTo(Status.Code.NOT_FOUND);
    }

    /**
     * Test: Delete non-existent todo returns NOT_FOUND.
     */
    @Test
    void testDeleteTodo_NotFound_ThrowsNotFound() {
        // Arrange
        DeleteTodoRequest request = DeleteTodoRequest.newBuilder()
            .setId(999L)
            .build();

        // Act & Assert
        assertThatThrownBy(() -> todoServiceStub.deleteTodo(request))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
            .isEqualTo(Status.Code.NOT_FOUND);
    }
}
