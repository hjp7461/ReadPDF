package com.pdfviewer.model.service;

import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.entity.PdfPage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PdfServiceImplTest {

    private PdfServiceImpl pdfService;

    @Mock
    private PDDocument mockPdDocument;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pdfService = new PdfServiceImpl();
    }

    @Test
    void validatePdfFile_shouldReturnValid_whenFileIsValid(@TempDir Path tempDir) throws IOException {
        // Create a mock PDF file
        Path pdfPath = tempDir.resolve("test.pdf");
        Files.createFile(pdfPath);
        File pdfFile = pdfPath.toFile();

        // Mock PDDocument.load to return our mock
        try (var mockedStatic = mockStatic(PDDocument.class)) {
            mockedStatic.when(() -> PDDocument.load(any(File.class))).thenReturn(mockPdDocument);

            // Mock document properties
            when(mockPdDocument.getNumberOfPages()).thenReturn(5);

            // Call the method under test
            PdfService.ValidationResult result = pdfService.validatePdfFile(pdfFile);

            // Verify the result
            assertTrue(result.isValid());
            assertTrue(result.pageCount().isPresent());
            assertEquals(5, result.pageCount().get());
            assertFalse(result.error().isPresent());
        }
    }

    @Test
    void validatePdfFile_shouldReturnInvalid_whenFileIsInvalid(@TempDir Path tempDir) throws IOException {
        // Create a mock PDF file
        Path pdfPath = tempDir.resolve("invalid.pdf");
        Files.createFile(pdfPath);
        File pdfFile = pdfPath.toFile();

        // Mock PDDocument.load to throw an exception
        try (var mockedStatic = mockStatic(PDDocument.class)) {
            IOException mockException = new IOException("Invalid PDF file");
            mockedStatic.when(() -> PDDocument.load(any(File.class))).thenThrow(mockException);

            // Call the method under test
            PdfService.ValidationResult result = pdfService.validatePdfFile(pdfFile);

            // Verify the result
            assertFalse(result.isValid());
            assertFalse(result.pageCount().isPresent());
            assertTrue(result.error().isPresent());
            assertEquals(mockException, result.error().get());
        }
    }

    @Test
    void isImageBasedPdf_shouldReturnTrue_whenPdfIsImageBased() {
        // Create a mock PdfDocument
        PdfDocument mockDocument = mock(PdfDocument.class);

        // Set up the mock to return true for isImageBased
        when(mockDocument.isImageBased()).thenReturn(true);

        // Call the method under test
        boolean result = pdfService.isImageBasedPdf(mockDocument);

        // Verify the result
        assertTrue(result);
    }

    @Test
    void enhanceImageQuality_shouldReturnEnhancedImage() {
        // Create a test image
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);

        // Call the method under test
        BufferedImage result = pdfService.enhanceImageQuality(testImage, 1.2f, 1.5f, 0.8f);

        // Verify the result is not null and has the same dimensions
        assertNotNull(result);
        assertEquals(testImage.getWidth(), result.getWidth());
        assertEquals(testImage.getHeight(), result.getHeight());
    }

    @Test
    void closeDocument_shouldNotThrowException() {
        // Create a mock PdfDocument
        PdfDocument mockDocument = mock(PdfDocument.class);

        // Call the method under test
        assertDoesNotThrow(() -> pdfService.closeDocument(mockDocument));
    }
}
