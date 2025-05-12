package com.pdfviewer.model.repository;

import com.pdfviewer.model.entity.CustomFont;

import java.sql.SQLException;
import java.util.List;

/**
 * Repository interface for managing CustomFont entities.
 */
public interface CustomFontRepository extends Repository<CustomFont, String> {
    
    /**
     * Finds all active custom fonts.
     * 
     * @return a list of all active custom fonts
     * @throws SQLException if a database access error occurs
     */
    List<CustomFont> findAllActive() throws SQLException;
    
    /**
     * Finds a custom font by its name.
     * 
     * @param fontName the name of the font to find
     * @return a list of custom fonts with the given name
     * @throws SQLException if a database access error occurs
     */
    List<CustomFont> findByName(String fontName) throws SQLException;
    
    /**
     * Sets the active state of a custom font.
     * 
     * @param id the ID of the font to update
     * @param active the new active state
     * @return true if the font was updated, false if the font was not found
     * @throws SQLException if a database access error occurs
     */
    boolean setActive(String id, boolean active) throws SQLException;
    
    /**
     * Deletes all custom fonts.
     * 
     * @return the number of fonts deleted
     * @throws SQLException if a database access error occurs
     */
    int deleteAll() throws SQLException;
}