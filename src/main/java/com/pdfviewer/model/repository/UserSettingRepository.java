package com.pdfviewer.model.repository;

import com.pdfviewer.model.entity.UserSetting;
import com.pdfviewer.model.entity.UserSetting.SettingKey;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for managing UserSetting entities.
 */
public interface UserSettingRepository extends Repository<UserSetting, String> {
    
    /**
     * Gets a setting value by its key.
     * 
     * @param key the key of the setting to get
     * @return an Optional containing the setting value if found, or empty if not found
     * @throws SQLException if a database access error occurs
     */
    Optional<String> getValue(String key) throws SQLException;
    
    /**
     * Gets a setting value by its key.
     * 
     * @param key the key of the setting to get
     * @return an Optional containing the setting value if found, or empty if not found
     * @throws SQLException if a database access error occurs
     */
    default Optional<String> getValue(SettingKey key) throws SQLException {
        return getValue(key.getKey());
    }
    
    /**
     * Gets a setting value by its key, or returns a default value if not found.
     * 
     * @param key the key of the setting to get
     * @param defaultValue the default value to return if the setting is not found
     * @return the setting value if found, or the default value if not found
     * @throws SQLException if a database access error occurs
     */
    default String getValueOrDefault(String key, String defaultValue) throws SQLException {
        return getValue(key).orElse(defaultValue);
    }
    
    /**
     * Gets a setting value by its key, or returns a default value if not found.
     * 
     * @param key the key of the setting to get
     * @param defaultValue the default value to return if the setting is not found
     * @return the setting value if found, or the default value if not found
     * @throws SQLException if a database access error occurs
     */
    default String getValueOrDefault(SettingKey key, String defaultValue) throws SQLException {
        return getValueOrDefault(key.getKey(), defaultValue);
    }
    
    /**
     * Sets a setting value.
     * 
     * @param key the key of the setting to set
     * @param value the value to set
     * @throws SQLException if a database access error occurs
     */
    void setValue(String key, String value) throws SQLException;
    
    /**
     * Sets a setting value.
     * 
     * @param key the key of the setting to set
     * @param value the value to set
     * @throws SQLException if a database access error occurs
     */
    default void setValue(SettingKey key, String value) throws SQLException {
        setValue(key.getKey(), value);
    }
    
    /**
     * Gets all settings as a map.
     * 
     * @return a map of all settings
     * @throws SQLException if a database access error occurs
     */
    Map<String, String> getAllSettings() throws SQLException;
    
    /**
     * Deletes a setting by its key.
     * 
     * @param key the key of the setting to delete
     * @return true if the setting was deleted, false if the setting was not found
     * @throws SQLException if a database access error occurs
     */
    boolean deleteByKey(String key) throws SQLException;
    
    /**
     * Deletes a setting by its key.
     * 
     * @param key the key of the setting to delete
     * @return true if the setting was deleted, false if the setting was not found
     * @throws SQLException if a database access error occurs
     */
    default boolean deleteByKey(SettingKey key) throws SQLException {
        return deleteByKey(key.getKey());
    }
}