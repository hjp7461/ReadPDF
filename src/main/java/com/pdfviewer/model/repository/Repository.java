package com.pdfviewer.model.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Base repository interface that defines common operations for all repositories.
 *
 * @param <T> the entity type
 * @param <ID> the type of the entity's ID
 */
public interface Repository<T, ID> {
    
    /**
     * Finds an entity by its ID.
     *
     * @param id the ID of the entity to find
     * @return an Optional containing the entity if found, or empty if not found
     * @throws SQLException if a database access error occurs
     */
    Optional<T> findById(ID id) throws SQLException;
    
    /**
     * Finds all entities.
     *
     * @return a list of all entities
     * @throws SQLException if a database access error occurs
     */
    List<T> findAll() throws SQLException;
    
    /**
     * Saves an entity.
     * If the entity already exists, it will be updated.
     * If the entity doesn't exist, it will be inserted.
     *
     * @param entity the entity to save
     * @return the saved entity
     * @throws SQLException if a database access error occurs
     */
    T save(T entity) throws SQLException;
    
    /**
     * Deletes an entity by its ID.
     *
     * @param id the ID of the entity to delete
     * @throws SQLException if a database access error occurs
     */
    void deleteById(ID id) throws SQLException;
    
    /**
     * Checks if an entity with the given ID exists.
     *
     * @param id the ID to check
     * @return true if an entity with the given ID exists, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean existsById(ID id) throws SQLException;
}