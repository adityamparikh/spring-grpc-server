package com.example.springgrpcserver.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * JPA Entity representing a Todo item in the database.
 *
 * This entity is separate from the protobuf-generated Todo message to maintain
 * separation between persistence layer and the gRPC transport layer.
 * This pattern allows database schema to evolve independently from the API contract.
 */
@Entity
@Table(name = "todos")
public class TodoEntity {

    /**
     * Primary key, auto-generated using database identity column.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Title of the todo item. Required field with length constraints.
     */
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Column(nullable = false)
    private String title;

    /**
     * Optional detailed description of the todo item.
     */
    @Column(length = 1000)
    private String description;

    /**
     * Completion status of the todo item.
     * Defaults to false (not completed) when created.
     */
    @Column(nullable = false)
    private boolean completed = false;

    /**
     * Timestamp when the todo was created.
     * Automatically set by JPA on entity creation.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the todo was last updated.
     * Automatically updated by JPA on entity modification.
     */
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * JPA lifecycle callback to set timestamps before persisting.
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * JPA lifecycle callback to update the modification timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Constructors

    /**
     * Default constructor required by JPA.
     */
    public TodoEntity() {
    }

    /**
     * Constructor for creating a new todo item.
     *
     * @param title the title of the todo
     * @param description the description of the todo
     */
    public TodoEntity(String title, String description) {
        this.title = title;
        this.description = description;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
