package com.pdfviewer.model.repository.db;

import com.pdfviewer.model.entity.RecentFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SqliteRecentFileRepositoryTest {

    @Mock
    private SqliteConnectionManager connectionManager;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    private SqliteRecentFileRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        
        // Set up the connection manager to return our mock connection
        when(connectionManager.getConnection()).thenReturn(connection);
        
        // Set up the connection to return our mock prepared statement and statement
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.createStatement()).thenReturn(statement);
        
        // Set up the prepared statement and statement to return our mock result set
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        
        repository = new SqliteRecentFileRepository(connectionManager);
    }

    @Test
    void findById_shouldReturnRecentFile_whenRecentFileExists() throws SQLException {
        // Arrange
        String id = UUID.randomUUID().toString();
        String filePath = "/path/to/file.pdf";
        String fileName = "file.pdf";
        LocalDateTime lastOpened = LocalDateTime.now();
        int pageCount = 10;
        int lastPageViewed = 5;
        float lastZoomLevel = 1.5f;
        
        // Set up the result set to return one row
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("id")).thenReturn(id);
        when(resultSet.getString("file_path")).thenReturn(filePath);
        when(resultSet.getString("file_name")).thenReturn(fileName);
        when(resultSet.getTimestamp("last_opened")).thenReturn(Timestamp.valueOf(lastOpened));
        when(resultSet.getInt("page_count")).thenReturn(pageCount);
        when(resultSet.getInt("last_page_viewed")).thenReturn(lastPageViewed);
        when(resultSet.getFloat("last_zoom_level")).thenReturn(lastZoomLevel);
        
        // Act
        Optional<RecentFile> result = repository.findById(id);
        
        // Assert
        assertTrue(result.isPresent());
        RecentFile recentFile = result.get();
        assertEquals(id, recentFile.getId());
        assertEquals(filePath, recentFile.getFilePath());
        assertEquals(fileName, recentFile.getFileName());
        assertEquals(lastOpened, recentFile.getLastOpened());
        assertEquals(pageCount, recentFile.getPageCount());
        assertEquals(lastPageViewed, recentFile.getLastPageViewed());
        assertEquals(lastZoomLevel, recentFile.getLastZoomLevel());
        
        // Verify that the correct SQL was executed
        verify(preparedStatement).setString(1, id);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void findById_shouldReturnEmpty_whenRecentFileDoesNotExist() throws SQLException {
        // Arrange
        String id = UUID.randomUUID().toString();
        
        // Set up the result set to return no rows
        when(resultSet.next()).thenReturn(false);
        
        // Act
        Optional<RecentFile> result = repository.findById(id);
        
        // Assert
        assertFalse(result.isPresent());
        
        // Verify that the correct SQL was executed
        verify(preparedStatement).setString(1, id);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void findByFilePath_shouldReturnRecentFile_whenRecentFileExists() throws SQLException {
        // Arrange
        String id = UUID.randomUUID().toString();
        String filePath = "/path/to/file.pdf";
        String fileName = "file.pdf";
        LocalDateTime lastOpened = LocalDateTime.now();
        int pageCount = 10;
        int lastPageViewed = 5;
        float lastZoomLevel = 1.5f;
        
        // Set up the result set to return one row
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("id")).thenReturn(id);
        when(resultSet.getString("file_path")).thenReturn(filePath);
        when(resultSet.getString("file_name")).thenReturn(fileName);
        when(resultSet.getTimestamp("last_opened")).thenReturn(Timestamp.valueOf(lastOpened));
        when(resultSet.getInt("page_count")).thenReturn(pageCount);
        when(resultSet.getInt("last_page_viewed")).thenReturn(lastPageViewed);
        when(resultSet.getFloat("last_zoom_level")).thenReturn(lastZoomLevel);
        
        // Act
        Optional<RecentFile> result = repository.findByFilePath(filePath);
        
        // Assert
        assertTrue(result.isPresent());
        RecentFile recentFile = result.get();
        assertEquals(id, recentFile.getId());
        assertEquals(filePath, recentFile.getFilePath());
        assertEquals(fileName, recentFile.getFileName());
        assertEquals(lastOpened, recentFile.getLastOpened());
        assertEquals(pageCount, recentFile.getPageCount());
        assertEquals(lastPageViewed, recentFile.getLastPageViewed());
        assertEquals(lastZoomLevel, recentFile.getLastZoomLevel());
        
        // Verify that the correct SQL was executed
        verify(preparedStatement).setString(1, filePath);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void findAll_shouldReturnAllRecentFiles() throws SQLException {
        // Arrange
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        
        // Set up the result set to return two rows
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("id")).thenReturn(id1, id2);
        when(resultSet.getString("file_path")).thenReturn("/path/to/file1.pdf", "/path/to/file2.pdf");
        when(resultSet.getString("file_name")).thenReturn("file1.pdf", "file2.pdf");
        when(resultSet.getTimestamp("last_opened")).thenReturn(
            Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()));
        when(resultSet.getInt("page_count")).thenReturn(10, 20);
        when(resultSet.getInt("last_page_viewed")).thenReturn(5, 10);
        when(resultSet.getFloat("last_zoom_level")).thenReturn(1.5f, 2.0f);
        
        // Act
        List<RecentFile> recentFiles = repository.findAll();
        
        // Assert
        assertEquals(2, recentFiles.size());
        assertEquals(id1, recentFiles.get(0).getId());
        assertEquals(id2, recentFiles.get(1).getId());
        
        // Verify that the correct SQL was executed
        verify(statement).executeQuery(anyString());
    }

    @Test
    void findAllOrderByLastOpenedDesc_shouldReturnLimitedRecentFiles() throws SQLException {
        // Arrange
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        int limit = 2;
        
        // Set up the result set to return two rows
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("id")).thenReturn(id1, id2);
        when(resultSet.getString("file_path")).thenReturn("/path/to/file1.pdf", "/path/to/file2.pdf");
        when(resultSet.getString("file_name")).thenReturn("file1.pdf", "file2.pdf");
        when(resultSet.getTimestamp("last_opened")).thenReturn(
            Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()));
        when(resultSet.getInt("page_count")).thenReturn(10, 20);
        when(resultSet.getInt("last_page_viewed")).thenReturn(5, 10);
        when(resultSet.getFloat("last_zoom_level")).thenReturn(1.5f, 2.0f);
        
        // Act
        List<RecentFile> recentFiles = repository.findAllOrderByLastOpenedDesc(limit);
        
        // Assert
        assertEquals(2, recentFiles.size());
        assertEquals(id1, recentFiles.get(0).getId());
        assertEquals(id2, recentFiles.get(1).getId());
        
        // Verify that the correct SQL was executed
        verify(preparedStatement).setInt(1, limit);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void save_shouldInsertNewRecentFile_whenRecentFileDoesNotExist() throws SQLException {
        // Arrange
        String id = UUID.randomUUID().toString();
        RecentFile recentFile = new RecentFile.Builder()
                .id(id)
                .filePath("/path/to/file.pdf")
                .fileName("file.pdf")
                .lastOpened(LocalDateTime.now())
                .pageCount(10)
                .lastPageViewed(5)
                .lastZoomLevel(1.5f)
                .build();
        
        // Set up the existsById method to return false
        when(resultSet.next()).thenReturn(false);
        
        // Set up the insert method
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Act
        RecentFile result = repository.save(recentFile);
        
        // Assert
        assertEquals(recentFile, result);
        
        // Verify that the correct SQL was executed
        verify(preparedStatement, atLeastOnce()).executeUpdate();
    }

    @Test
    void deleteById_shouldDeleteRecentFile() throws SQLException {
        // Arrange
        String id = UUID.randomUUID().toString();
        
        // Set up the delete method
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Act
        repository.deleteById(id);
        
        // Verify that the correct SQL was executed
        verify(preparedStatement).setString(1, id);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void existsById_shouldReturnTrue_whenRecentFileExists() throws SQLException {
        // Arrange
        String id = UUID.randomUUID().toString();
        
        // Set up the result set to return one row
        when(resultSet.next()).thenReturn(true);
        
        // Act
        boolean result = repository.existsById(id);
        
        // Assert
        assertTrue(result);
        
        // Verify that the correct SQL was executed
        verify(preparedStatement).setString(1, id);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void updateLastViewedInfo_shouldUpdateRecentFile() throws SQLException {
        // Arrange
        String id = UUID.randomUUID().toString();
        int lastPageViewed = 5;
        float lastZoomLevel = 1.5f;
        
        // Set up the update method
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Act
        boolean result = repository.updateLastViewedInfo(id, lastPageViewed, lastZoomLevel);
        
        // Assert
        assertTrue(result);
        
        // Verify that the correct SQL was executed
        verify(preparedStatement).setInt(1, lastPageViewed);
        verify(preparedStatement).setFloat(2, lastZoomLevel);
        verify(preparedStatement).setTimestamp(eq(3), any(Timestamp.class));
        verify(preparedStatement).setString(4, id);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void deleteOldFiles_shouldDeleteOldFiles() throws SQLException {
        // Arrange
        int keepCount = 5;
        
        // Set up the delete method
        when(preparedStatement.executeUpdate()).thenReturn(3);
        
        // Act
        int result = repository.deleteOldFiles(keepCount);
        
        // Assert
        assertEquals(3, result);
        
        // Verify that the correct SQL was executed
        verify(preparedStatement).setInt(1, keepCount);
        verify(preparedStatement).executeUpdate();
    }
}