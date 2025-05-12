package com.pdfviewer.model.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CustomFontTest {

    @Test
    void builder_shouldCreateCustomFontWithSpecifiedValues() {
        // Arrange
        String id = UUID.randomUUID().toString();
        String fontName = "Arial";
        String fontPath = "/fonts/arial.ttf";
        boolean isActive = true;
        LocalDateTime createdAt = LocalDateTime.now();

        // Act
        CustomFont customFont = new CustomFont.Builder()
                .id(id)
                .fontName(fontName)
                .fontPath(fontPath)
                .isActive(isActive)
                .createdAt(createdAt)
                .build();

        // Assert
        assertEquals(id, customFont.getId());
        assertEquals(fontName, customFont.getFontName());
        assertEquals(fontPath, customFont.getFontPath());
        assertEquals(isActive, customFont.isActive());
        assertEquals(createdAt, customFont.getCreatedAt());
    }

    @Test
    void builder_shouldCreateCustomFontWithDefaultValues() {
        // Arrange
        String fontName = "Helvetica";
        String fontPath = "/fonts/helvetica.ttf";

        // Act
        CustomFont customFont = new CustomFont.Builder()
                .fontName(fontName)
                .fontPath(fontPath)
                .build();

        // Assert
        assertNotNull(customFont.getId());
        assertEquals(fontName, customFont.getFontName());
        assertEquals(fontPath, customFont.getFontPath());
        assertTrue(customFont.isActive()); // Default is true
        assertNotNull(customFont.getCreatedAt());
    }

    @Test
    void equals_shouldReturnTrue_whenCustomFontsHaveSameId() {
        // Arrange
        String id = UUID.randomUUID().toString();
        
        CustomFont font1 = new CustomFont.Builder()
                .id(id)
                .fontName("Arial")
                .fontPath("/fonts/arial.ttf")
                .isActive(true)
                .build();
                
        CustomFont font2 = new CustomFont.Builder()
                .id(id)
                .fontName("Helvetica") // Different fontName
                .fontPath("/fonts/helvetica.ttf") // Different fontPath
                .isActive(false) // Different isActive
                .build();

        // Act & Assert
        assertEquals(font1, font2);
        assertEquals(font1.hashCode(), font2.hashCode());
    }

    @Test
    void equals_shouldReturnFalse_whenCustomFontsHaveDifferentIds() {
        // Arrange
        CustomFont font1 = new CustomFont.Builder()
                .id(UUID.randomUUID().toString())
                .fontName("Arial")
                .fontPath("/fonts/arial.ttf")
                .isActive(true)
                .build();
                
        CustomFont font2 = new CustomFont.Builder()
                .id(UUID.randomUUID().toString())
                .fontName("Arial") // Same fontName
                .fontPath("/fonts/arial.ttf") // Same fontPath
                .isActive(true) // Same isActive
                .build();

        // Act & Assert
        assertNotEquals(font1, font2);
        assertNotEquals(font1.hashCode(), font2.hashCode());
    }

    @Test
    void toString_shouldContainAllFields() {
        // Arrange
        String id = "test-id";
        String fontName = "Times New Roman";
        String fontPath = "/fonts/times.ttf";
        boolean isActive = false;
        LocalDateTime createdAt = LocalDateTime.of(2023, 1, 1, 12, 0);

        CustomFont customFont = new CustomFont.Builder()
                .id(id)
                .fontName(fontName)
                .fontPath(fontPath)
                .isActive(isActive)
                .createdAt(createdAt)
                .build();

        // Act
        String toString = customFont.toString();

        // Assert
        assertTrue(toString.contains(id));
        assertTrue(toString.contains(fontName));
        assertTrue(toString.contains(fontPath));
        assertTrue(toString.contains(String.valueOf(isActive)));
        assertTrue(toString.contains(createdAt.toString()));
    }

    @Test
    void withActive_shouldCreateNewInstanceWithUpdatedActiveState() {
        // Arrange
        CustomFont originalFont = new CustomFont.Builder()
                .fontName("Arial")
                .fontPath("/fonts/arial.ttf")
                .isActive(true)
                .build();
        
        // Act
        CustomFont updatedFont = originalFont.withActive(false);
        
        // Assert
        assertEquals(originalFont.getId(), updatedFont.getId());
        assertEquals(originalFont.getFontName(), updatedFont.getFontName());
        assertEquals(originalFont.getFontPath(), updatedFont.getFontPath());
        assertEquals(originalFont.getCreatedAt(), updatedFont.getCreatedAt());
        assertFalse(updatedFont.isActive());
        assertTrue(originalFont.isActive()); // Original should be unchanged
    }
}