package com.pdfviewer.model.service.renderer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating PdfRenderer instances.
 * This factory allows for easy switching between different renderer implementations.
 */
public class PdfRendererFactory {
    private static final Logger logger = LoggerFactory.getLogger(PdfRendererFactory.class);
    
    /**
     * Creates a new PdfRenderer for the given PDDocument.
     *
     * @param document The PDDocument to render
     * @return A PdfRenderer instance
     */
    public static PdfRenderer createRenderer(PDDocument document) {
        logger.debug("Creating PDFBoxRenderer for document");
        return new PDFBoxRenderer(document);
    }
    
    /**
     * Creates a new PdfRenderer for the given PDDocument with the specified renderer type.
     *
     * @param document The PDDocument to render
     * @param rendererType The type of renderer to create
     * @return A PdfRenderer instance
     */
    public static PdfRenderer createRenderer(PDDocument document, RendererType rendererType) {
        logger.debug("Creating {} for document", rendererType);
        
        return switch (rendererType) {
            case PDF_BOX -> new PDFBoxRenderer(document);
            // Add other renderer types here as they are implemented
        };
    }
    
    /**
     * Enum representing the available renderer types.
     */
    public enum RendererType {
        PDF_BOX
        // Add other renderer types here as they are implemented
    }
}