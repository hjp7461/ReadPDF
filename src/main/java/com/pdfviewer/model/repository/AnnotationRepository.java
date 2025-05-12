package com.pdfviewer.model.repository;

import com.pdfviewer.model.entity.Annotation;
import com.pdfviewer.model.entity.Annotation.AnnotationType;

import java.sql.SQLException;
import java.util.List;

/**
 * Repository interface for managing Annotation entities.
 */
public interface AnnotationRepository extends Repository<Annotation, String> {
    
    /**
     * Finds all annotations for a specific file.
     * 
     * @param fileId the ID of the file to find annotations for
     * @return a list of annotations for the file
     * @throws SQLException if a database access error occurs
     */
    List<Annotation> findAllByFileId(String fileId) throws SQLException;
    
    /**
     * Finds all annotations for a specific file and page.
     * 
     * @param fileId the ID of the file to find annotations for
     * @param pageNumber the page number to find annotations for
     * @return a list of annotations for the file and page
     * @throws SQLException if a database access error occurs
     */
    List<Annotation> findAllByFileIdAndPageNumber(String fileId, int pageNumber) throws SQLException;
    
    /**
     * Finds all annotations of a specific type for a file.
     * 
     * @param fileId the ID of the file to find annotations for
     * @param type the type of annotations to find
     * @return a list of annotations of the specified type for the file
     * @throws SQLException if a database access error occurs
     */
    List<Annotation> findAllByFileIdAndType(String fileId, AnnotationType type) throws SQLException;
    
    /**
     * Finds all annotations of a specific type for a file and page.
     * 
     * @param fileId the ID of the file to find annotations for
     * @param pageNumber the page number to find annotations for
     * @param type the type of annotations to find
     * @return a list of annotations of the specified type for the file and page
     * @throws SQLException if a database access error occurs
     */
    List<Annotation> findAllByFileIdAndPageNumberAndType(String fileId, int pageNumber, AnnotationType type) throws SQLException;
    
    /**
     * Deletes all annotations for a specific file.
     * 
     * @param fileId the ID of the file to delete annotations for
     * @return the number of annotations deleted
     * @throws SQLException if a database access error occurs
     */
    int deleteAllByFileId(String fileId) throws SQLException;
    
    /**
     * Deletes all annotations for a specific file and page.
     * 
     * @param fileId the ID of the file to delete annotations for
     * @param pageNumber the page number to delete annotations for
     * @return the number of annotations deleted
     * @throws SQLException if a database access error occurs
     */
    int deleteAllByFileIdAndPageNumber(String fileId, int pageNumber) throws SQLException;
    
    /**
     * Counts the number of annotations for a specific file.
     * 
     * @param fileId the ID of the file to count annotations for
     * @return the number of annotations for the file
     * @throws SQLException if a database access error occurs
     */
    int countByFileId(String fileId) throws SQLException;
    
    /**
     * Counts the number of annotations for a specific file and page.
     * 
     * @param fileId the ID of the file to count annotations for
     * @param pageNumber the page number to count annotations for
     * @return the number of annotations for the file and page
     * @throws SQLException if a database access error occurs
     */
    int countByFileIdAndPageNumber(String fileId, int pageNumber) throws SQLException;
}