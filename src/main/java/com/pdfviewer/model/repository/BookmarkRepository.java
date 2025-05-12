package com.pdfviewer.model.repository;

import com.pdfviewer.model.entity.Bookmark;

import java.sql.SQLException;
import java.util.List;

/**
 * Repository interface for managing Bookmark entities.
 */
public interface BookmarkRepository extends Repository<Bookmark, String> {
    
    /**
     * Finds all bookmarks for a specific file.
     * 
     * @param fileId the ID of the file to find bookmarks for
     * @return a list of bookmarks for the file
     * @throws SQLException if a database access error occurs
     */
    List<Bookmark> findAllByFileId(String fileId) throws SQLException;
    
    /**
     * Finds all bookmarks for a specific file and page.
     * 
     * @param fileId the ID of the file to find bookmarks for
     * @param pageNumber the page number to find bookmarks for
     * @return a list of bookmarks for the file and page
     * @throws SQLException if a database access error occurs
     */
    List<Bookmark> findAllByFileIdAndPageNumber(String fileId, int pageNumber) throws SQLException;
    
    /**
     * Deletes all bookmarks for a specific file.
     * 
     * @param fileId the ID of the file to delete bookmarks for
     * @return the number of bookmarks deleted
     * @throws SQLException if a database access error occurs
     */
    int deleteAllByFileId(String fileId) throws SQLException;
    
    /**
     * Counts the number of bookmarks for a specific file.
     * 
     * @param fileId the ID of the file to count bookmarks for
     * @return the number of bookmarks for the file
     * @throws SQLException if a database access error occurs
     */
    int countByFileId(String fileId) throws SQLException;
}