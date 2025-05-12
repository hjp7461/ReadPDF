package com.pdfviewer.model.service.renderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * Interface for PDF page rendering operations.
 * This interface defines the contract for rendering PDF pages to images.
 */
public interface PdfRenderer {

    /**
     * Renders a page from a PDF document at the specified DPI.
     *
     * @param pageIndex The zero-based index of the page to render
     * @param dpi The DPI (dots per inch) to render at
     * @return A BufferedImage containing the rendered page
     * @throws IOException if the page cannot be rendered
     */
    BufferedImage renderImageWithDPI(int pageIndex, float dpi) throws IOException;

    /**
     * Renders a page from a PDF document at the specified DPI with rotation.
     *
     * @param pageIndex The zero-based index of the page to render
     * @param dpi The DPI (dots per inch) to render at
     * @param rotation The rotation angle in degrees (0, 90, 180, 270)
     * @return A BufferedImage containing the rendered page
     * @throws IOException if the page cannot be rendered
     */
    BufferedImage renderImageWithDPI(int pageIndex, float dpi, float rotation) throws IOException;

    /**
     * Renders a page from a PDF document with the specified zoom factor.
     *
     * @param pageIndex The zero-based index of the page to render
     * @param zoomFactor The zoom factor (1.0 = 100%)
     * @return A BufferedImage containing the rendered page
     * @throws IOException if the page cannot be rendered
     */
    BufferedImage renderImageWithZoom(int pageIndex, double zoomFactor) throws IOException;

    /**
     * Renders a page from a PDF document with the specified zoom factor and rotation.
     *
     * @param pageIndex The zero-based index of the page to render
     * @param zoomFactor The zoom factor (1.0 = 100%)
     * @param rotation The rotation angle in degrees (0, 90, 180, 270)
     * @return A BufferedImage containing the rendered page
     * @throws IOException if the page cannot be rendered
     */
    BufferedImage renderImageWithZoom(int pageIndex, double zoomFactor, float rotation) throws IOException;

    /**
     * Generates thumbnails for all pages in the PDF document.
     *
     * @param width The width of the thumbnails
     * @param height The height of the thumbnails
     * @return A list of BufferedImage objects containing the thumbnails
     * @throws IOException if the thumbnails cannot be generated
     */
    List<BufferedImage> generateThumbnails(int width, int height) throws IOException;

    /**
     * Gets the number of pages in the PDF document.
     *
     * @return The number of pages
     */
    int getPageCount();

    /**
     * Closes the renderer and releases any resources.
     *
     * @throws IOException if an error occurs while closing
     */
    void close() throws IOException;
}
