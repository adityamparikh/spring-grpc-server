package com.example.springgrpcserver.repository;

import com.example.springgrpcserver.entity.TodoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for TodoEntity.
 *
 * JpaRepository provides standard CRUD operations out of the box:
 * - save(), findById(), findAll(), deleteById(), etc.
 *
 * Spring Data JPA can derive query methods from method names,
 * eliminating the need to write SQL or JPQL for simple queries.
 */
@Repository
public interface TodoRepository extends JpaRepository<TodoEntity, Long> {

    /**
     * Find todos by completion status with pagination support.
     *
     * Spring Data JPA automatically implements this method by parsing
     * the method name: "findBy" + "Completed" field.
     *
     * @param completed the completion status to filter by
     * @param pageable pagination parameters (page size, page number, sorting)
     * @return a page of todos matching the completion status
     */
    Page<TodoEntity> findByCompleted(boolean completed, Pageable pageable);
}
