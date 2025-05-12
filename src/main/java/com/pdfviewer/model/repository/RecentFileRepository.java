package com.pdfviewer.model.repository;

import com.pdfviewer.model.entity.RecentFile;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing RecentFile entities.
 */
public interface RecentFileRepository extends Repository<RecentFile, String> {
    
    /**
     * Finds a recent file by its file path.
     * 
     * @param filePath the file path to search for
     * @return an Optional containing the recent file if found, or empty if not found
     * @throws SQLException if a database access error occurs
     */
    Optional<RecentFile> findByFilePath(String filePath) throws SQLException;
    
    /**
     * Finds all recent files, ordered by last opened timestamp (most recent first).
     * 
     * @param limit the maximum number of files to return
     * @return a list of recent files
     * @throws SQLException if a database access error occurs
     */
    List<RecentFile> findAllOrderByLastOpenedDesc(int limit) throws SQLException;
    
    /**
     * Updates the last opened timestamp, last page viewed, and last zoom level for a file.
     * 
     * @param fileId the ID of the file to update
     * @param lastPageViewed the last page viewed by the user
     * @param lastZoomLevel the last zoom level used by the user
     * @return true if the file was updated, false if the file was not found
     * @throws SQLException if a database access error occurs
     */
    boolean updateLastViewedInfo(String fileId, int lastPageViewed, float lastZoomLevel) throws SQLException;
    
    /**
     * Deletes old recent files, keeping only the most recent ones.
     * 
     * @param keepCount the number of recent files to keep
     * @return the number of files deleted
     * @throws SQLException if a database access error occurs
     */
    int deleteOldFiles(int keepCount) throws SQLException;
}