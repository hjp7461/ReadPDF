package com.pdfviewer.model.service;

import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.entity.PdfPage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for PDF file processing operations.
 */
public interface PdfService {

    /**
     * Opens a PDF file and returns a PdfDocument object.
     *
     * @param file The PDF file to open
     * @return A PdfDocument object containing the document's metadata and pages
     * @throws PdfProcessingException if the file cannot be opened or is not a valid PDF
     */
    PdfDocument openDocument(File file) throws PdfProcessingException;

    /**
     * Opens a PDF file and returns a PdfDocument object.
     *
     * @param path The path to the PDF file to open
     * @return A PdfDocument object containing the document's metadata and pages
     * @throws PdfProcessingException if the file cannot be opened or is not a valid PDF
     */
    PdfDocument openDocument(Path path) throws PdfProcessingException;

    /**
     * Validates a PDF file and returns the validation result.
     *
     * @param file The PDF file to validate
     * @return A ValidationResult object containing the validation status and details
     */
    ValidationResult validatePdfFile(File file);

    /**
     * Attempts to repair a damaged PDF file.
     *
     * @param file The damaged PDF file to repair
     * @return A RepairResult object containing the repair status and the repaired file if successful
     */
    RepairResult attemptPdfRepair(File file);

    /**
     * Checks if a PDF document is image-based (contains mostly images instead of text).
     *
     * @param document The PDF document to check
     * @return true if the document is image-based, false otherwise
     */
    boolean isImageBasedPdf(PdfDocument document);

    /**
     * Renders a page from a PDF document at the specified DPI.
     *
     * @param document The PDF document
     * @param pageNumber The page number to render (1-based)
     * @param dpi The DPI (dots per inch) to render at
     * @return A BufferedImage containing the rendered page
     * @throws PdfProcessingException if the page cannot be rendered
     */
    BufferedImage renderPage(PdfDocument document, int pageNumber, float dpi) throws PdfProcessingException;

    /**
     * Renders a page from a PDF document with the specified zoom factor.
     *
     * @param document The PDF document
     * @param pageNumber The page number to render (1-based)
     * @param zoomFactor The zoom factor (1.0 = 100%)
     * @return A BufferedImage containing the rendered page
     * @throws PdfProcessingException if the page cannot be rendered
     */
    BufferedImage renderPage(PdfDocument document, int pageNumber, double zoomFactor) throws PdfProcessingException;

    /**
     * Generates thumbnails for all pages in a PDF document.
     *
     * @param document The PDF document
     * @param width The width of the thumbnails
     * @param height The height of the thumbnails
     * @return A list of BufferedImage objects containing the thumbnails
     */
    List<BufferedImage> generateThumbnails(PdfDocument document, int width, int height);

    /**
     * Extracts text from a page in a PDF document.
     *
     * @param document The PDF document
     * @param pageNumber The page number to extract text from (1-based)
     * @return The extracted text, or an empty string if no text is found
     */
    String extractText(PdfDocument document, int pageNumber);

    /**
     * Enhances the quality of an image-based PDF page.
     *
     * @param image The page image to enhance
     * @param brightness The brightness adjustment (-1.0 to 1.0)
     * @param contrast The contrast adjustment (-1.0 to 1.0)
     * @param sharpness The sharpness adjustment (0.0 to 2.0)
     * @return The enhanced image
     */
    BufferedImage enhanceImageQuality(BufferedImage image, float brightness, float contrast, float sharpness);

    /**
     * Closes a PDF document and releases its resources.
     *
     * @param document The PDF document to close
     */
    void closeDocument(PdfDocument document);

    /**
     * Rotates a page to the specified angle.
     *
     * @param document The PDF document
     * @param pageNumber The page number to rotate (1-based)
     * @param angle The rotation angle in degrees (0, 90, 180, 270)
     * @return A new PdfDocument with the rotated page
     * @throws PdfProcessingException if the page cannot be rotated
     */
    PdfDocument rotatePage(PdfDocument document, int pageNumber, float angle) throws PdfProcessingException;

    /**
     * Rotates a page by the specified angle relative to its current rotation.
     *
     * @param document The PDF document
     * @param pageNumber The page number to rotate (1-based)
     * @param angleDelta The rotation angle delta in degrees (e.g., 90 for 90 degrees clockwise)
     * @return A new PdfDocument with the rotated page
     * @throws PdfProcessingException if the page cannot be rotated
     */
    PdfDocument rotatePageBy(PdfDocument document, int pageNumber, float angleDelta) throws PdfProcessingException;

    /**
     * Resets a page's rotation to 0 degrees.
     *
     * @param document The PDF document
     * @param pageNumber The page number to reset (1-based)
     * @return A new PdfDocument with the reset page
     * @throws PdfProcessingException if the page cannot be reset
     */
    PdfDocument resetPageRotation(PdfDocument document, int pageNumber) throws PdfProcessingException;

    /**
     * Result of a PDF file validation operation.
     */
    record ValidationResult(
        boolean isValid,
        String message,
        Optional<Exception> error,
        Optional<PdfDocument.PdfVersion> pdfVersion,
        Optional<Integer> pageCount
    ) {
        public static ValidationResult valid(PdfDocument.PdfVersion version, int pageCount) {
            return new ValidationResult(true, "Valid PDF file", Optional.empty(), 
                                       Optional.of(version), Optional.of(pageCount));
        }

        public static ValidationResult invalid(String message, Exception error) {
            return new ValidationResult(false, message, Optional.of(error), 
                                       Optional.empty(), Optional.empty());
        }
    }

    /**
     * Result of a PDF file repair operation.
     */
    record RepairResult(
        boolean isSuccess,
        String message,
        Optional<File> originalFile,
        Optional<File> repairedFile,
        Optional<Exception> error
    ) {
        public static RepairResult success(File original, File repaired) {
            return new RepairResult(true, "PDF file successfully repaired", 
                                   Optional.of(original), Optional.of(repaired), Optional.empty());
        }

        public static RepairResult failure(File original, String message, Exception error) {
            return new RepairResult(false, message, Optional.of(original), 
                                   Optional.empty(), Optional.of(error));
        }
    }

    /**
     * Exception thrown when PDF processing operations fail.
     */
    class PdfProcessingException extends Exception {
        public PdfProcessingException(String message) {
            super(message);
        }

        public PdfProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
