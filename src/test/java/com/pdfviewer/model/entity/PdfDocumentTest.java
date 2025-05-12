package com.pdfviewer.model.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PdfDocumentTest {

    @Test
    void builder_shouldCreatePdfDocumentWithSpecifiedValues() {
        // Arrange
        String filePath = "/path/to/document.pdf";
        String fileName = "document.pdf";
        int pageCount = 5;
        PdfDocument.PdfVersion version = new PdfDocument.PdfVersion(1, 7);
        Map<String, String> metadata = Map.of("Title", "Test Document", "Author", "Test Author");
        List<PdfPage> pages = createTestPages(pageCount);
        LocalDateTime openedAt = LocalDateTime.now();
        boolean isImageBased = true;
        boolean isRepaired = true;

        // Act
        PdfDocument document = new PdfDocument.Builder()
                .filePath(filePath)
                .fileName(fileName)
                .pageCount(pageCount)
                .version(version)
                .metadata(metadata)
                .pages(pages)
                .openedAt(openedAt)
                .isImageBased(isImageBased)
                .isRepaired(isRepaired)
                .build();

        // Assert
        assertEquals(filePath, document.getFilePath());
        assertEquals(fileName, document.getFileName());
        assertEquals(pageCount, document.getPageCount());
        assertEquals(version, document.getVersion());
        assertEquals(metadata, document.getMetadata());
        assertEquals(pages, document.getPages());
        assertEquals(openedAt, document.getOpenedAt());
        assertEquals(isImageBased, document.isImageBased());
        assertEquals(isRepaired, document.isRepaired());
    }

    @Test
    void builder_shouldCreatePdfDocumentWithPathOverload() {
        // Arrange
        Path path = Path.of("/path/to/document.pdf");
        
        // Act
        PdfDocument document = new PdfDocument.Builder()
                .filePath(path)
                .build();
        
        // Assert
        assertEquals(path.toString(), document.getFilePath());
        assertEquals("document.pdf", document.getFileName());
    }

    @Test
    void builder_shouldCreatePdfDocumentWithDefaultValues() {
        // Arrange
        String filePath = "/path/to/document.pdf";
        String fileName = "document.pdf";
        int pageCount = 3;
        List<PdfPage> pages = createTestPages(pageCount);
        Map<String, String> metadata = new HashMap<>();
        
        // Act
        PdfDocument document = new PdfDocument.Builder()
                .filePath(filePath)
                .fileName(fileName)
                .pageCount(pageCount)
                .pages(pages)
                .metadata(metadata)
                .build();
        
        // Assert
        assertNotNull(document.getOpenedAt());
        assertFalse(document.isImageBased()); // Default is false
        assertFalse(document.isRepaired()); // Default is false
    }

    @Test
    void getPage_shouldReturnPage_whenPageNumberIsValid() {
        // Arrange
        int pageCount = 5;
        List<PdfPage> pages = createTestPages(pageCount);
        
        PdfDocument document = new PdfDocument.Builder()
                .filePath("/path/to/document.pdf")
                .fileName("document.pdf")
                .pageCount(pageCount)
                .pages(pages)
                .metadata(new HashMap<>())
                .build();
        
        // Act & Assert
        for (int i = 1; i <= pageCount; i++) {
            Optional<PdfPage> page = document.getPage(i);
            assertTrue(page.isPresent());
            assertEquals(i, page.get().getPageNumber());
        }
    }

    @Test
    void getPage_shouldReturnEmpty_whenPageNumberIsInvalid() {
        // Arrange
        int pageCount = 3;
        List<PdfPage> pages = createTestPages(pageCount);
        
        PdfDocument document = new PdfDocument.Builder()
                .filePath("/path/to/document.pdf")
                .fileName("document.pdf")
                .pageCount(pageCount)
                .pages(pages)
                .metadata(new HashMap<>())
                .build();
        
        // Act & Assert
        // Test with page number less than 1
        Optional<PdfPage> pageTooLow = document.getPage(0);
        assertFalse(pageTooLow.isPresent());
        
        // Test with page number greater than page count
        Optional<PdfPage> pageTooHigh = document.getPage(pageCount + 1);
        assertFalse(pageTooHigh.isPresent());
    }

    @Test
    void pdfVersion_shouldFormatToString() {
        // Arrange
        PdfDocument.PdfVersion version = new PdfDocument.PdfVersion(1, 7);
        
        // Act
        String versionString = version.toString();
        
        // Assert
        assertEquals("1.7", versionString);
    }

    private List<PdfPage> createTestPages(int count) {
        List<PdfPage> pages = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            pages.add(new PdfPage.Builder()
                    .pageNumber(i)
                    .build());
        }
        return pages;
    }
}