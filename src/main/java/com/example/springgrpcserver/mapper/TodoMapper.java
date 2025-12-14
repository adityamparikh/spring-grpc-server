package com.example.springgrpcserver.mapper;

import com.example.springgrpcserver.entity.TodoEntity;
import com.example.springgrpcserver.proto.Todo;
import org.springframework.stereotype.Component;

/**
 * Mapper component for converting between JPA entities and protobuf messages.
 *
 * This separation allows:
 * 1. Database schema to evolve independently from the gRPC API contract
 * 2. Different field names/types in DB vs API if needed
 * 3. Additional business logic during conversion if required
 */
@Component
public class TodoMapper {

    /**
     * Converts a JPA TodoEntity to a protobuf Todo message.
     *
     * Protobuf messages are immutable and use the Builder pattern.
     * All fields must be set through the builder before calling build().
     *
     * @param entity the JPA entity to convert
     * @return the corresponding protobuf message
     */
    public Todo toProto(TodoEntity entity) {
        // Protobuf uses builders for constructing immutable message objects
        Todo.Builder builder = Todo.newBuilder()
                .setId(entity.getId())
                .setTitle(entity.getTitle())
                .setCompleted(entity.isCompleted())
                // Convert Instant to epoch milliseconds for transmission
                .setCreatedAt(entity.getCreatedAt().toEpochMilli())
                .setUpdatedAt(entity.getUpdatedAt().toEpochMilli());

        // Handle optional description field
        // In protobuf, empty strings are the default, but we explicitly set non-null values
        if (entity.getDescription() != null) {
            builder.setDescription(entity.getDescription());
        }

        return builder.build();
    }

    /**
     * Updates an existing TodoEntity from an UpdateTodoRequest.
     * <p>
     * Only updates fields that are explicitly set in the request (using proto3 optional).
     * This allows partial updates - clients can update just the fields they want to change.
     *
     * @param entity the entity to update
     * @param title the new title (if present)
     * @param description the new description (if present)
     * @param completed the new completion status (if present)
     */
    public void updateEntityFromRequest(TodoEntity entity,
                                       com.google.protobuf.ByteString title,
                                       com.google.protobuf.ByteString description,
                                       Boolean completed) {
        // Note: In the actual service, we'll use the hasTitle(), hasDescription() methods
        // from the generated UpdateTodoRequest to check if fields were set
    }
}
