package com.pdfviewer.model.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationTest {

    @Test
    void builder_shouldCreateAnnotationWithSpecifiedValues() {
        // Arrange
        String id = UUID.randomUUID().toString();
        String fileId = "file123";
        int pageNumber = 42;
        Annotation.AnnotationType type = Annotation.AnnotationType.HIGHLIGHT;
        String content = "Important text";
        float x = 100.5f;
        float y = 200.5f;
        Float width = 50.0f;
        Float height = 20.0f;
        String color = "#FF0000";
        LocalDateTime createdAt = LocalDateTime.now();

        // Act
        Annotation annotation = new Annotation.Builder()
                .id(id)
                .fileId(fileId)
                .pageNumber(pageNumber)
                .type(type)
                .content(content)
                .x(x)
                .y(y)
                .width(width)
                .height(height)
                .color(color)
                .createdAt(createdAt)
                .build();

        // Assert
        assertEquals(id, annotation.getId());
        assertEquals(fileId, annotation.getFileId());
        assertEquals(pageNumber, annotation.getPageNumber());
        assertEquals(type, annotation.getType());
        assertEquals(content, annotation.getContent());
        assertEquals(x, annotation.getX());
        assertEquals(y, annotation.getY());
        assertEquals(width, annotation.getWidth());
        assertEquals(height, annotation.getHeight());
        assertEquals(color, annotation.getColor());
        assertEquals(createdAt, annotation.getCreatedAt());
    }

    @Test
    void builder_shouldCreateAnnotationWithDefaultValues() {
        // Arrange
        String fileId = "file123";
        int pageNumber = 42;
        Annotation.AnnotationType type = Annotation.AnnotationType.NOTE;
        String content = "Note content";
        float x = 100.5f;
        float y = 200.5f;

        // Act
        Annotation annotation = new Annotation.Builder()
                .fileId(fileId)
                .pageNumber(pageNumber)
                .type(type)
                .content(content)
                .x(x)
                .y(y)
                .build();

        // Assert
        assertNotNull(annotation.getId());
        assertNotNull(annotation.getCreatedAt());
        assertEquals(fileId, annotation.getFileId());
        assertEquals(pageNumber, annotation.getPageNumber());
        assertEquals(type, annotation.getType());
        assertEquals(content, annotation.getContent());
        assertEquals(x, annotation.getX());
        assertEquals(y, annotation.getY());
        assertEquals("#FFFF00", annotation.getColor()); // Default yellow color
    }

    @Test
    void equals_shouldReturnTrue_whenAnnotationsHaveSameId() {
        // Arrange
        String id = UUID.randomUUID().toString();
        
        Annotation annotation1 = new Annotation.Builder()
                .id(id)
                .fileId("file1")
                .pageNumber(1)
                .type(Annotation.AnnotationType.HIGHLIGHT)
                .content("Content 1")
                .x(100)
                .y(100)
                .build();
                
        Annotation annotation2 = new Annotation.Builder()
                .id(id)
                .fileId("file2") // Different fileId
                .pageNumber(2)   // Different pageNumber
                .type(Annotation.AnnotationType.NOTE) // Different type
                .content("Content 2") // Different content
                .x(200) // Different x
                .y(200) // Different y
                .build();

        // Act & Assert
        assertEquals(annotation1, annotation2);
        assertEquals(annotation1.hashCode(), annotation2.hashCode());
    }

    @Test
    void equals_shouldReturnFalse_whenAnnotationsHaveDifferentIds() {
        // Arrange
        Annotation annotation1 = new Annotation.Builder()
                .id(UUID.randomUUID().toString())
                .fileId("file1")
                .pageNumber(1)
                .type(Annotation.AnnotationType.HIGHLIGHT)
                .content("Content")
                .x(100)
                .y(100)
                .build();
                
        Annotation annotation2 = new Annotation.Builder()
                .id(UUID.randomUUID().toString())
                .fileId("file1") // Same fileId
                .pageNumber(1)   // Same pageNumber
                .type(Annotation.AnnotationType.HIGHLIGHT) // Same type
                .content("Content") // Same content
                .x(100) // Same x
                .y(100) // Same y
                .build();

        // Act & Assert
        assertNotEquals(annotation1, annotation2);
        assertNotEquals(annotation1.hashCode(), annotation2.hashCode());
    }

    @Test
    void toString_shouldContainAllFields() {
        // Arrange
        String id = "test-id";
        String fileId = "file123";
        int pageNumber = 42;
        Annotation.AnnotationType type = Annotation.AnnotationType.TEXT;
        String content = "Text content";
        float x = 100.5f;
        float y = 200.5f;
        Float width = 50.0f;
        Float height = 20.0f;
        String color = "#0000FF";
        LocalDateTime createdAt = LocalDateTime.of(2023, 1, 1, 12, 0);

        Annotation annotation = new Annotation.Builder()
                .id(id)
                .fileId(fileId)
                .pageNumber(pageNumber)
                .type(type)
                .content(content)
                .x(x)
                .y(y)
                .width(width)
                .height(height)
                .color(color)
                .createdAt(createdAt)
                .build();

        // Act
        String toString = annotation.toString();

        // Assert
        assertTrue(toString.contains(id));
        assertTrue(toString.contains(fileId));
        assertTrue(toString.contains(String.valueOf(pageNumber)));
        assertTrue(toString.contains(type.toString()));
        assertTrue(toString.contains(content));
        assertTrue(toString.contains(String.valueOf(x)));
        assertTrue(toString.contains(String.valueOf(y)));
        assertTrue(toString.contains(String.valueOf(width)));
        assertTrue(toString.contains(String.valueOf(height)));
        assertTrue(toString.contains(color));
        assertTrue(toString.contains(createdAt.toString()));
    }
}