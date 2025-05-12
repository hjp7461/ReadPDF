package com.pdfviewer.model.repository.db;

import com.pdfviewer.model.entity.Bookmark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SqliteBookmarkRepositoryTest {

    @Mock
    private SqliteConnectionManager connectionManager;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private SqliteBookmarkRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);

        // Set up the connection manager to return our mock connection
        when(connectionManager.getConnection()).thenReturn(connection);

        // Set up the connection to return our mock prepared statement
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        // Set up the prepared statement to return our mock result set
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        repository = new SqliteBookmarkRepository(connectionManager);
    }

    @Test
    void findById_shouldReturnBookmark_whenBookmarkExists() throws SQLException {
        // Arrange
        String id = UUID.randomUUID().toString();
        String fileId = "file123";
        int pageNumber = 42;
        String name = "Test Bookmark";
        String description = "Test Description";
        LocalDateTime createdAt = LocalDateTime.now();

        // Set up the result set to return one row
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("id")).thenReturn(id);
        when(resultSet.getString("file_id")).thenReturn(fileId);
        when(resultSet.getInt("page_number")).thenReturn(pageNumber);
        when(resultSet.getString("name")).thenReturn(name);
        when(resultSet.getString("description")).thenReturn(description);
        when(resultSet.getObject("created_at", LocalDateTime.class)).thenReturn(createdAt);

        // Act
        Optional<Bookmark> result = repository.findById(id);

        // Assert
        assertTrue(result.isPresent());
        Bookmark bookmark = result.get();
        assertEquals(id, bookmark.getId());
        assertEquals(fileId, bookmark.getFileId());
        assertEquals(pageNumber, bookmark.getPageNumber());
        assertEquals(name, bookmark.getName());
        assertEquals(description, bookmark.getDescription());
        assertEquals(createdAt, bookmark.getCreatedAt());

        // Verify that the correct SQL was executed
        verify(preparedStatement).setString(1, id);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void findById_shouldReturnEmpty_whenBookmarkDoesNotExist() throws SQLException {
        // Arrange
        String id = UUID.randomUUID().toString();

        // Set up the result set to return no rows
        when(resultSet.next()).thenReturn(false);

        // Act
        Optional<Bookmark> result = repository.findById(id);

        // Assert
        assertFalse(result.isPresent());

        // Verify that the correct SQL was executed
        verify(preparedStatement).setString(1, id);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void findAllByFileId_shouldReturnBookmarks_whenBookmarksExist() throws SQLException {
        // Arrange
        String fileId = "file123";
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();

        // Set up the result set to return two rows
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("id")).thenReturn(id1, id2);
        when(resultSet.getString("file_id")).thenReturn(fileId, fileId);
        when(resultSet.getInt("page_number")).thenReturn(1, 2);
        when(resultSet.getString("name")).thenReturn("Bookmark 1", "Bookmark 2");
        when(resultSet.getString("description")).thenReturn("Description 1", "Description 2");
        when(resultSet.getObject("created_at", LocalDateTime.class)).thenReturn(
            LocalDateTime.now(), LocalDateTime.now());

        // Act
        List<Bookmark> bookmarks = repository.findAllByFileId(fileId);

        // Assert
        assertEquals(2, bookmarks.size());
        assertEquals(id1, bookmarks.get(0).getId());
        assertEquals(id2, bookmarks.get(1).getId());

        // Verify that the correct SQL was executed
        verify(preparedStatement).setString(1, fileId);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void save_shouldInsertNewBookmark_whenBookmarkDoesNotExist() throws SQLException {
        // Arrange
        String id = UUID.randomUUID().toString();
        Bookmark bookmark = new Bookmark.Builder()
                .id(id)
                .fileId("file123")
                .pageNumber(42)
                .name("Test Bookmark")
                .description("Test Description")
                .build();

        // Set up the existsById method to return false
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Set up the insert method
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Act
        Bookmark result = repository.save(bookmark);

        // Assert
        assertEquals(bookmark, result);

        // Verify that the correct SQL was executed
        verify(preparedStatement, atLeastOnce()).executeUpdate();
    }

    @Test
    void deleteById_shouldDeleteBookmark() throws SQLException {
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
}
