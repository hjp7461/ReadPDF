package com.pdfviewer.model.repository;

import com.pdfviewer.model.entity.OcrCache;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing OcrCache entities.
 */
public interface OcrCacheRepository {
    
    /**
     * Finds a cached OCR result for a specific file and page.
     * 
     * @param fileId the ID of the file
     * @param pageNumber the page number
     * @return an Optional containing the OCR cache if found, or empty if not found
     * @throws SQLException if a database access error occurs
     */
    Optional<OcrCache> findByFileIdAndPageNumber(String fileId, int pageNumber) throws SQLException;
    
    /**
     * Finds all cached OCR results for a specific file.
     * 
     * @param fileId the ID of the file
     * @return a list of OCR caches for the file
     * @throws SQLException if a database access error occurs
     */
    List<OcrCache> findAllByFileId(String fileId) throws SQLException;
    
    /**
     * Saves an OCR cache.
     * If a cache for the same file and page already exists, it will be updated.
     * 
     * @param ocrCache the OCR cache to save
     * @return the saved OCR cache
     * @throws SQLException if a database access error occurs
     */
    OcrCache save(OcrCache ocrCache) throws SQLException;
    
    /**
     * Deletes a cached OCR result for a specific file and page.
     * 
     * @param fileId the ID of the file
     * @param pageNumber the page number
     * @return true if the cache was deleted, false if the cache was not found
     * @throws SQLException if a database access error occurs
     */
    boolean deleteByFileIdAndPageNumber(String fileId, int pageNumber) throws SQLException;
    
    /**
     * Deletes all cached OCR results for a specific file.
     * 
     * @param fileId the ID of the file
     * @return the number of caches deleted
     * @throws SQLException if a database access error occurs
     */
    int deleteAllByFileId(String fileId) throws SQLException;
    
    /**
     * Checks if a cached OCR result exists for a specific file and page.
     * 
     * @param fileId the ID of the file
     * @param pageNumber the page number
     * @return true if a cache exists, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean existsByFileIdAndPageNumber(String fileId, int pageNumber) throws SQLException;
    
    /**
     * Counts the number of cached OCR results for a specific file.
     * 
     * @param fileId the ID of the file
     * @return the number of caches for the file
     * @throws SQLException if a database access error occurs
     */
    int countByFileId(String fileId) throws SQLException;
}