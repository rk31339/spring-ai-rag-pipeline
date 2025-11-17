package com.rk.ai.rag.repository;

import com.rk.ai.rag.model.DocumentRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for DocumentRegistry entity.
 * Provides CRUD operations and custom queries for document tracking.
 */
@Repository
public interface DocumentRegistryRepository extends JpaRepository<DocumentRegistry, UUID> {
    
    /**
     * Find a document by filename.
     * 
     * @param filename The name of the file
     * @return Optional containing the document if found
     */
    Optional<DocumentRegistry> findByFilename(String filename);
    
    /**
     * Check if a document with the given filename exists.
     * 
     * @param filename The name of the file
     * @return true if document exists, false otherwise
     */
    boolean existsByFilename(String filename);
    
    /**
     * Delete a document by filename.
     * 
     * @param filename The name of the file to delete
     */
    void deleteByFilename(String filename);
}
