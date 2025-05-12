package com.pdfviewer.model.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BookmarkTest {

    @Test
    void builder_shouldCreateBookmarkWithSpecifiedValues() {
        // Arrange
        String id = UUID.randomUUID().toString();
        String fileId = "file123";
        int pageNumber = 42;
        String name = "Important Section";
        String description = "This section contains important information";
        LocalDateTime createdAt = LocalDateTime.now();

        // Act
        Bookmark bookmark = new Bookmark.Builder()
                .id(id)
                .fileId(fileId)
                .pageNumber(pageNumber)
                .name(name)
                .description(description)
                .createdAt(createdAt)
                .build();

        // Assert
        assertEquals(id, bookmark.getId());
        assertEquals(fileId, bookmark.getFileId());
        assertEquals(pageNumber, bookmark.getPageNumber());
        assertEquals(name, bookmark.getName());
        assertEquals(description, bookmark.getDescription());
        assertEquals(createdAt, bookmark.getCreatedAt());
    }

    @Test
    void builder_shouldCreateBookmarkWithDefaultIdAndCreatedAt() {
        // Arrange
        String fileId = "file123";
        int pageNumber = 42;
        String name = "Important Section";

        // Act
        Bookmark bookmark = new Bookmark.Builder()
                .fileId(fileId)
                .pageNumber(pageNumber)
                .name(name)
                .build();

        // Assert
        assertNotNull(bookmark.getId());
        assertNotNull(bookmark.getCreatedAt());
        assertEquals(fileId, bookmark.getFileId());
        assertEquals(pageNumber, bookmark.getPageNumber());
        assertEquals(name, bookmark.getName());
    }

    @Test
    void equals_shouldReturnTrue_whenBookmarksHaveSameId() {
        // Arrange
        String id = UUID.randomUUID().toString();
        
        Bookmark bookmark1 = new Bookmark.Builder()
                .id(id)
                .fileId("file1")
                .pageNumber(1)
                .name("Bookmark 1")
                .build();
                
        Bookmark bookmark2 = new Bookmark.Builder()
                .id(id)
                .fileId("file2") // Different fileId
                .pageNumber(2)   // Different pageNumber
                .name("Bookmark 2") // Different name
                .build();

        // Act & Assert
        assertEquals(bookmark1, bookmark2);
        assertEquals(bookmark1.hashCode(), bookmark2.hashCode());
    }

    @Test
    void equals_shouldReturnFalse_whenBookmarksHaveDifferentIds() {
        // Arrange
        Bookmark bookmark1 = new Bookmark.Builder()
                .id(UUID.randomUUID().toString())
                .fileId("file1")
                .pageNumber(1)
                .name("Bookmark 1")
                .build();
                
        Bookmark bookmark2 = new Bookmark.Builder()
                .id(UUID.randomUUID().toString())
                .fileId("file1") // Same fileId
                .pageNumber(1)   // Same pageNumber
                .name("Bookmark 1") // Same name
                .build();

        // Act & Assert
        assertNotEquals(bookmark1, bookmark2);
        assertNotEquals(bookmark1.hashCode(), bookmark2.hashCode());
    }

    @Test
    void toString_shouldContainAllFields() {
        // Arrange
        String id = "test-id";
        String fileId = "file123";
        int pageNumber = 42;
        String name = "Important Section";
        String description = "This section contains important information";
        LocalDateTime createdAt = LocalDateTime.of(2023, 1, 1, 12, 0);

        Bookmark bookmark = new Bookmark.Builder()
                .id(id)
                .fileId(fileId)
                .pageNumber(pageNumber)
                .name(name)
                .description(description)
                .createdAt(createdAt)
                .build();

        // Act
        String toString = bookmark.toString();

        // Assert
        assertTrue(toString.contains(id));
        assertTrue(toString.contains(fileId));
        assertTrue(toString.contains(String.valueOf(pageNumber)));
        assertTrue(toString.contains(name));
        assertTrue(toString.contains(description));
        assertTrue(toString.contains(createdAt.toString()));
    }
}