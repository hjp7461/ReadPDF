package com.pdfviewer.model.service.renderer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Implementation of the PdfRenderer interface using Apache PDFBox.
 */
public class PDFBoxRenderer implements PdfRenderer {
    private static final Logger logger = LoggerFactory.getLogger(PDFBoxRenderer.class);

    private final PDDocument document;
    private final PDFRenderer renderer;
    private final ExecutorService executor;

    /**
     * Creates a new PDFBoxRenderer for the given PDDocument.
     *
     * @param document The PDDocument to render
     */
    public PDFBoxRenderer(PDDocument document) {
        this.document = document;
        this.renderer = new PDFRenderer(document);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public BufferedImage renderImageWithDPI(int pageIndex, float dpi) throws IOException {
        logger.debug("Rendering page {} at {} DPI", pageIndex + 1, dpi);
        try {
            return renderer.renderImageWithDPI(pageIndex, dpi);
        } catch (IOException e) {
            logger.error("Error rendering page {} at {} DPI", pageIndex + 1, dpi, e);
            throw e;
        }
    }

    @Override
    public BufferedImage renderImageWithDPI(int pageIndex, float dpi, float rotation) throws IOException {
        logger.debug("Rendering page {} at {} DPI with rotation {}", pageIndex + 1, dpi, rotation);
        try {
            // Get the page
            var page = document.getPage(pageIndex);

            // Save the original rotation
            float originalRotation = page.getRotation();

            // Set the rotation for rendering
            page.setRotation((int) rotation);

            // Render the page
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpi);

            // Restore the original rotation
            page.setRotation((int) originalRotation);

            return image;
        } catch (IOException e) {
            logger.error("Error rendering page {} at {} DPI with rotation {}", pageIndex + 1, dpi, rotation, e);
            throw e;
        }
    }

    @Override
    public BufferedImage renderImageWithZoom(int pageIndex, double zoomFactor) throws IOException {
        // Convert zoom factor to DPI (assuming 72 DPI is 100% zoom)
        float dpi = (float) (72 * zoomFactor);
        return renderImageWithDPI(pageIndex, dpi);
    }

    @Override
    public BufferedImage renderImageWithZoom(int pageIndex, double zoomFactor, float rotation) throws IOException {
        // Convert zoom factor to DPI (assuming 72 DPI is 100% zoom)
        float dpi = (float) (72 * zoomFactor);
        return renderImageWithDPI(pageIndex, dpi, rotation);
    }

    @Override
    public List<BufferedImage> generateThumbnails(int width, int height) throws IOException {
        logger.debug("Generating thumbnails for document with {} pages", document.getNumberOfPages());

        List<BufferedImage> thumbnails = new ArrayList<>();
        List<Future<BufferedImage>> futures = new ArrayList<>();

        // Use virtual threads to generate thumbnails in parallel
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            final int pageIndex = i;
            futures.add(executor.submit(() -> {
                try {
                    // Render at a low DPI for thumbnails
                    BufferedImage image = renderImageWithDPI(pageIndex, 72);

                    // Scale the image to the requested size
                    Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    thumbnail.getGraphics().drawImage(scaledImage, 0, 0, null);

                    return thumbnail;
                } catch (IOException e) {
                    logger.error("Error generating thumbnail for page {}", pageIndex + 1, e);
                    return null;
                }
            }));
        }

        // Collect the results
        for (Future<BufferedImage> future : futures) {
            try {
                BufferedImage thumbnail = future.get();
                if (thumbnail != null) {
                    thumbnails.add(thumbnail);
                }
            } catch (Exception e) {
                logger.error("Error collecting thumbnail", e);
            }
        }

        return thumbnails;
    }

    @Override
    public int getPageCount() {
        return document.getNumberOfPages();
    }

    @Override
    public void close() throws IOException {
        executor.shutdown();
        // Note: We don't close the document here because it's managed by the caller
    }
}
