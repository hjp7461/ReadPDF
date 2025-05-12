/**
 * Specialized handler for large PDF documents.
 * This class implements strategies for efficiently loading and rendering large PDF documents,
 * including progressive loading and special optimizations for memory usage.
 */
package com.pdfviewer.model.service.cache;

import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.entity.PdfPage;
import com.pdfviewer.model.service.renderer.PdfRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class LargeDocumentHandler {
    private static final Logger logger = LoggerFactory.getLogger(LargeDocumentHandler.class);

    // Threshold for considering a document as "large" (number of pages)
    private static final int LARGE_DOCUMENT_THRESHOLD = 100;

    // Threshold for considering a document as "very large" (number of pages)
    private static final int VERY_LARGE_DOCUMENT_THRESHOLD = 500;

    // Number of pages to load in each batch for progressive loading
    private static final int BATCH_SIZE = 20;

    // The PDF renderer to use for rendering pages
    private final PdfRenderer renderer;

    // The page cache to store rendered pages
    private final PageRenderCache pageCache;

    // The memory monitor to track memory usage
    private final MemoryMonitor memoryMonitor;

    // Executor service for background loading
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // Flag to indicate if progressive loading is enabled
    private boolean progressiveLoadingEnabled = true;

    /**
     * Creates a new LargeDocumentHandler.
     *
     * @param renderer The PDF renderer to use
     * @param pageCache The page cache to store rendered pages
     * @param memoryMonitor The memory monitor to track memory usage
     */
    public LargeDocumentHandler(PdfRenderer renderer, PageRenderCache pageCache, MemoryMonitor memoryMonitor) {
        this.renderer = renderer;
        this.pageCache = pageCache;
        this.memoryMonitor = memoryMonitor;

        // Add memory status listener to adjust behavior based on memory usage
        memoryMonitor.addListener(this::handleMemoryStatusChange);
    }

    /**
     * Checks if a document is considered "large" based on its page count.
     *
     * @param document The PDF document to check
     * @return true if the document is large, false otherwise
     */
    public boolean isLargeDocument(PdfDocument document) {
        return document.getPageCount() >= LARGE_DOCUMENT_THRESHOLD;
    }

    /**
     * Checks if a document is considered "very large" based on its page count.
     *
     * @param document The PDF document to check
     * @return true if the document is very large, false otherwise
     */
    public boolean isVeryLargeDocument(PdfDocument document) {
        return document.getPageCount() >= VERY_LARGE_DOCUMENT_THRESHOLD;
    }

    /**
     * Enables or disables progressive loading.
     *
     * @param enabled true to enable progressive loading, false to disable
     */
    public void setProgressiveLoadingEnabled(boolean enabled) {
        this.progressiveLoadingEnabled = enabled;
        logger.info("Progressive loading {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Loads thumbnails for a large document progressively.
     *
     * @param document The PDF document
     * @param width The thumbnail width
     * @param height The thumbnail height
     * @param progressCallback Callback for reporting progress (0.0-1.0)
     * @param batchCompleteCallback Callback for when a batch of thumbnails is loaded
     * @return A CompletableFuture that completes when all thumbnails are loaded
     */
    public CompletableFuture<List<BufferedImage>> loadThumbnailsProgressively(
            PdfDocument document, 
            int width, 
            int height, 
            Consumer<Double> progressCallback,
            Consumer<List<BufferedImage>> batchCompleteCallback) {

        if (!isLargeDocument(document) || !progressiveLoadingEnabled) {
            // For smaller documents, load all thumbnails at once
            try {
                List<BufferedImage> thumbnails = renderer.generateThumbnails(width, height);
                progressCallback.accept(1.0);
                return CompletableFuture.completedFuture(thumbnails);
            } catch (IOException e) {
                logger.error("Error generating thumbnails", e);
                return CompletableFuture.failedFuture(e);
            }
        }

        // For large documents, load thumbnails in batches
        return CompletableFuture.supplyAsync(() -> {
            List<BufferedImage> allThumbnails = new ArrayList<>(document.getPageCount());

            // Initialize with placeholder thumbnails
            for (int i = 0; i < document.getPageCount(); i++) {
                allThumbnails.add(null);
            }

            int totalPages = document.getPageCount();
            AtomicInteger completedPages = new AtomicInteger(0);

            // Calculate number of batches
            int numBatches = (totalPages + BATCH_SIZE - 1) / BATCH_SIZE;

            // Load thumbnails in batches
            for (int batchIndex = 0; batchIndex < numBatches; batchIndex++) {
                int startPage = batchIndex * BATCH_SIZE;
                int endPage = Math.min(startPage + BATCH_SIZE, totalPages);

                List<BufferedImage> batchThumbnails = new ArrayList<>(endPage - startPage);

                try {
                    // Check memory status before loading batch
                    if (memoryMonitor.getCurrentStatus() == MemoryMonitor.MemoryStatus.CRITICAL) {
                        // If memory is critical, pause briefly before continuing
                        Thread.sleep(1000);
                        memoryMonitor.requestGarbageCollection();
                    }

                    // Load thumbnails for this batch
                    for (int pageIndex = startPage; pageIndex < endPage; pageIndex++) {
                        // Use low DPI rendering for thumbnails
                        float thumbnailDpi = 72.0f; // Low DPI for thumbnails
                        BufferedImage fullImage = renderer.renderImageWithDPI(pageIndex, thumbnailDpi);

                        // Scale the image to the requested thumbnail size if needed
                        BufferedImage thumbnail = fullImage;
                        if (fullImage.getWidth() != width || fullImage.getHeight() != height) {
                            thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                            thumbnail.createGraphics().drawImage(fullImage, 0, 0, width, height, null);
                        }

                        batchThumbnails.add(thumbnail);
                        allThumbnails.set(pageIndex, thumbnail);

                        // Update progress
                        int completed = completedPages.incrementAndGet();
                        double progress = (double) completed / totalPages;
                        progressCallback.accept(progress);
                    }

                    // Notify batch completion
                    batchCompleteCallback.accept(batchThumbnails);

                    logger.debug("Loaded thumbnail batch {}/{} (pages {}-{})", 
                            batchIndex + 1, numBatches, startPage + 1, endPage);

                } catch (Exception e) {
                    logger.error("Error loading thumbnail batch {}/{}", batchIndex + 1, numBatches, e);
                }
            }

            return allThumbnails;
        }, executor);
    }

    /**
     * Optimizes rendering settings based on document size and memory status.
     *
     * @param document The PDF document
     * @param pageNumber The page number to render
     * @param zoomFactor The zoom factor
     * @return The optimal render quality for the current conditions
     */
    public PageRenderCache.RenderQuality getOptimalRenderQuality(
            PdfDocument document, int pageNumber, double zoomFactor) {

        // Default to normal quality
        PageRenderCache.RenderQuality quality = PageRenderCache.RenderQuality.NORMAL;

        // For very large documents, use lower quality unless zoomed in
        if (isVeryLargeDocument(document)) {
            if (zoomFactor < 1.5) {
                quality = PageRenderCache.RenderQuality.LOW;
            }
        }

        // If memory usage is high, reduce quality
        MemoryMonitor.MemoryStatus memoryStatus = memoryMonitor.getCurrentStatus();
        if (memoryStatus == MemoryMonitor.MemoryStatus.WARNING) {
            if (quality == PageRenderCache.RenderQuality.HIGH) {
                quality = PageRenderCache.RenderQuality.NORMAL;
            }
        } else if (memoryStatus == MemoryMonitor.MemoryStatus.CRITICAL) {
            quality = PageRenderCache.RenderQuality.LOW;
        }

        return quality;
    }

    /**
     * Renders a page with optimized settings for large documents.
     *
     * @param document The PDF document
     * @param pageNumber The page number to render (1-based)
     * @param zoomFactor The zoom factor
     * @return The rendered page image
     * @throws IOException if an error occurs during rendering
     */
    public BufferedImage renderPageOptimized(PdfDocument document, int pageNumber, double zoomFactor) 
            throws IOException {

        // Get the optimal render quality
        PageRenderCache.RenderQuality quality = getOptimalRenderQuality(document, pageNumber, zoomFactor);

        // Check if the page is already cached with this quality
        String filePath = document.getFilePath();
        if (pageCache.getPage(filePath, pageNumber, zoomFactor, quality).isPresent()) {
            return pageCache.getPage(filePath, pageNumber, zoomFactor, quality).get();
        }

        // Adjust DPI based on quality and zoom
        float dpi;
        switch (quality) {
            case LOW:
                dpi = 72;
                break;
            case HIGH:
                dpi = 300;
                break;
            case NORMAL:
            default:
                dpi = 150;
                break;
        }

        // Apply zoom factor
        dpi *= zoomFactor;

        // Get the page rotation
        float rotation = document.getPage(pageNumber)
                .map(PdfPage::getRotation)
                .orElse(0.0f);

        // Render the page
        BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, dpi, rotation);

        // Cache the rendered page
        pageCache.putPage(filePath, pageNumber, zoomFactor, quality, image);

        return image;
    }

    /**
     * Handles memory status changes by adjusting caching and rendering behavior.
     *
     * @param oldStatus The previous memory status
     * @param newStatus The new memory status
     */
    private void handleMemoryStatusChange(MemoryMonitor.MemoryStatus oldStatus, MemoryMonitor.MemoryStatus newStatus) {
        if (newStatus == MemoryMonitor.MemoryStatus.CRITICAL) {
            // Reduce memory usage by clearing some caches
            pageCache.adjustCacheSize();

            // If still critical, request garbage collection
            if (memoryMonitor.getMemoryUsage() > 0.9) {
                memoryMonitor.requestGarbageCollection();
            }
        } else if (newStatus == MemoryMonitor.MemoryStatus.WARNING) {
            // Adjust cache size to prevent reaching critical status
            pageCache.adjustCacheSize();
        }
    }

    /**
     * Shuts down the handler and releases resources.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
