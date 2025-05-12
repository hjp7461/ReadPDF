/**
 * Enhanced page cache system with adaptive sizing, memory monitoring, and optimized rendering.
 * This class provides a sophisticated caching mechanism for rendered PDF pages with
 * memory-aware cache management and support for different quality levels based on zoom.
 */
package com.pdfviewer.model.service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.pdfviewer.model.service.cache.MemoryMonitor;
import com.pdfviewer.model.service.cache.MemoryMonitor.MemoryStatus;
import com.pdfviewer.model.service.cache.MemoryMonitor.MemoryStatusListener;

public class PageRenderCache {
    private static final Logger logger = LoggerFactory.getLogger(PageRenderCache.class);

    // Default maximum number of pages to cache per document
    private static final int DEFAULT_MAX_PAGES_PER_DOCUMENT = 30;

    // Default maximum number of documents to cache
    private static final int DEFAULT_MAX_DOCUMENTS = 5;

    // Default memory threshold (80% of max memory)
    private static final double DEFAULT_MEMORY_THRESHOLD = 0.8;

    // Lock for thread safety
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // Cache structure: filePath -> (cacheKey -> page image)
    private final Map<String, Map<CacheKey, SoftReference<BufferedImage>>> cache = new ConcurrentHashMap<>();

    // LRU tracking: filePath -> list of recently used cache keys (most recent at front)
    private final Map<String, Deque<CacheKey>> lruTrackers = new ConcurrentHashMap<>();

    // Document LRU tracking (most recently used documents)
    private final Deque<String> documentLruTracker = new LinkedList<>();

    // Configuration
    private int maxPagesPerDocument;
    private int maxDocuments;
    private double memoryThreshold;

    // Memory monitoring
    private final Runtime runtime = Runtime.getRuntime();

    // Advanced memory monitoring
    private final MemoryMonitor memoryMonitor;

    /**
     * Creates a new PageRenderCache with default settings.
     */
    public PageRenderCache() {
        this(DEFAULT_MAX_PAGES_PER_DOCUMENT, DEFAULT_MAX_DOCUMENTS, DEFAULT_MEMORY_THRESHOLD);
    }

    /**
     * Creates a new PageRenderCache with custom settings.
     *
     * @param maxPagesPerDocument Maximum number of pages to cache per document
     * @param maxDocuments Maximum number of documents to cache
     * @param memoryThreshold Memory usage threshold (0.0-1.0) for cache eviction
     */
    public PageRenderCache(int maxPagesPerDocument, int maxDocuments, double memoryThreshold) {
        this.maxPagesPerDocument = maxPagesPerDocument;
        this.maxDocuments = maxDocuments;
        this.memoryThreshold = memoryThreshold;

        // Initialize memory monitor with the same threshold
        this.memoryMonitor = new MemoryMonitor(memoryThreshold * 0.9, memoryThreshold, 5);

        // Register a listener to handle memory status changes
        this.memoryMonitor.addListener(new MemoryMonitor.MemoryStatusListener() {
            @Override
            public void onMemoryStatusChanged(MemoryStatus oldStatus, MemoryStatus newStatus) {
                handleMemoryStatusChange(oldStatus, newStatus);
            }
        });

        // Start monitoring
        this.memoryMonitor.startMonitoring();

        logger.info("Initialized PageRenderCache with maxPagesPerDocument={}, maxDocuments={}, memoryThreshold={}",
                maxPagesPerDocument, maxDocuments, memoryThreshold);
    }

    /**
     * Handles memory status changes from the MemoryMonitor.
     * 
     * @param oldStatus The previous memory status
     * @param newStatus The new memory status
     */
    private void handleMemoryStatusChange(MemoryStatus oldStatus, MemoryStatus newStatus) {
        logger.info("Memory status changed from {} to {}", oldStatus, newStatus);

        switch (newStatus) {
            case NORMAL:
                // If memory usage returns to normal, we can gradually increase cache size
                if (oldStatus != MemoryStatus.NORMAL) {
                    lock.writeLock().lock();
                    try {
                        maxPagesPerDocument = Math.min(100, maxPagesPerDocument + 5);
                        logger.info("Increased cache size due to normal memory status: maxPagesPerDocument={}", 
                                maxPagesPerDocument);
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
                break;

            case WARNING:
                // Reduce cache size moderately
                lock.writeLock().lock();
                try {
                    maxPagesPerDocument = Math.max(10, maxPagesPerDocument - 5);
                    reduceMemoryUsage();
                    logger.info("Reduced cache size due to memory warning: maxPagesPerDocument={}", 
                            maxPagesPerDocument);
                } finally {
                    lock.writeLock().unlock();
                }
                break;

            case CRITICAL:
                // Reduce cache size aggressively
                lock.writeLock().lock();
                try {
                    maxPagesPerDocument = Math.max(5, maxPagesPerDocument - 10);
                    reduceMemoryUsage();
                    logger.warn("Aggressively reduced cache size due to critical memory status: maxPagesPerDocument={}", 
                            maxPagesPerDocument);
                } finally {
                    lock.writeLock().unlock();
                }
                break;
        }
    }

    /**
     * Gets a page from the cache.
     *
     * @param filePath The path of the PDF file
     * @param pageNumber The page number
     * @return An Optional containing the cached image if found, or empty if not found
     */
    public Optional<BufferedImage> getPage(String filePath, int pageNumber) {
        return getPage(filePath, pageNumber, 0, RenderQuality.NORMAL);
    }

    /**
     * Gets a page from the cache with a specific zoom factor.
     *
     * @param filePath The path of the PDF file
     * @param pageNumber The page number
     * @param zoomFactor The zoom factor
     * @return An Optional containing the cached image if found, or empty if not found
     */
    public Optional<BufferedImage> getPage(String filePath, int pageNumber, double zoomFactor) {
        return getPage(filePath, pageNumber, zoomFactor, RenderQuality.NORMAL);
    }

    /**
     * Gets a page from the cache with a specific zoom factor and quality.
     *
     * @param filePath The path of the PDF file
     * @param pageNumber The page number
     * @param zoomFactor The zoom factor
     * @param quality The render quality
     * @return An Optional containing the cached image if found, or empty if not found
     */
    public Optional<BufferedImage> getPage(String filePath, int pageNumber, double zoomFactor, RenderQuality quality) {
        CacheKey cacheKey = new CacheKey(pageNumber, zoomFactor, quality);

        lock.readLock().lock();
        try {
            Map<CacheKey, SoftReference<BufferedImage>> documentCache = cache.get(filePath);
            if (documentCache == null) {
                return Optional.empty();
            }

            SoftReference<BufferedImage> ref = documentCache.get(cacheKey);
            if (ref == null) {
                // Try to find a higher quality version that we can downscale
                if (quality != RenderQuality.HIGH) {
                    Optional<BufferedImage> higherQualityImage = findHigherQualityVersion(documentCache, pageNumber, zoomFactor, quality);
                    if (higherQualityImage.isPresent()) {
                        // Update LRU status
                        updateLruStatus(filePath, cacheKey);
                        return higherQualityImage;
                    }
                }
                return Optional.empty();
            }

            BufferedImage image = ref.get();
            if (image == null) {
                // Reference was cleared by GC
                documentCache.remove(cacheKey);
                return Optional.empty();
            }

            // Update LRU status
            updateLruStatus(filePath, cacheKey);

            return Optional.of(image);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Puts a page in the cache.
     *
     * @param filePath The path of the PDF file
     * @param pageNumber The page number
     * @param image The rendered page image
     */
    public void putPage(String filePath, int pageNumber, BufferedImage image) {
        putPage(filePath, pageNumber, 0, RenderQuality.NORMAL, image);
    }

    /**
     * Puts a page in the cache with a specific zoom factor.
     *
     * @param filePath The path of the PDF file
     * @param pageNumber The page number
     * @param zoomFactor The zoom factor
     * @param image The rendered page image
     */
    public void putPage(String filePath, int pageNumber, double zoomFactor, BufferedImage image) {
        putPage(filePath, pageNumber, zoomFactor, RenderQuality.NORMAL, image);
    }

    /**
     * Puts a page in the cache with a specific zoom factor and quality.
     *
     * @param filePath The path of the PDF file
     * @param pageNumber The page number
     * @param zoomFactor The zoom factor
     * @param quality The render quality
     * @param image The rendered page image
     */
    public void putPage(String filePath, int pageNumber, double zoomFactor, RenderQuality quality, BufferedImage image) {
        // Check memory usage before adding to cache
        if (isMemoryUsageHigh()) {
            reduceMemoryUsage();
        }

        CacheKey cacheKey = new CacheKey(pageNumber, zoomFactor, quality);

        lock.writeLock().lock();
        try {
            // Ensure the document cache exists
            Map<CacheKey, SoftReference<BufferedImage>> documentCache = 
                    cache.computeIfAbsent(filePath, k -> new ConcurrentHashMap<>());

            // Ensure the LRU tracker exists
            Deque<CacheKey> lruTracker = 
                    lruTrackers.computeIfAbsent(filePath, k -> new LinkedList<>());

            // Check if we need to evict pages from this document's cache
            if (documentCache.size() >= maxPagesPerDocument && !documentCache.containsKey(cacheKey)) {
                evictLeastRecentlyUsedPage(filePath);
            }

            // Add or update the page in the cache
            documentCache.put(cacheKey, new SoftReference<>(image));

            // Update LRU status
            updateLruStatus(filePath, cacheKey);

            // Check if we need to evict documents
            if (cache.size() > maxDocuments) {
                evictLeastRecentlyUsedDocument();
            }

            logger.debug("Cached page {} (zoom={}, quality={}) for document {}", 
                    pageNumber, zoomFactor, quality, filePath);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears the cache for a specific document.
     *
     * @param filePath The path of the PDF file
     */
    public void clearDocumentCache(String filePath) {
        lock.writeLock().lock();
        try {
            cache.remove(filePath);
            lruTrackers.remove(filePath);
            documentLruTracker.remove(filePath);
            logger.debug("Cleared cache for document {}", filePath);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears the entire cache.
     */
    public void clearAllCaches() {
        lock.writeLock().lock();
        try {
            cache.clear();
            lruTrackers.clear();
            documentLruTracker.clear();
            logger.info("Cleared all page caches");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the number of documents in the cache.
     *
     * @return The number of documents
     */
    public int getDocumentCount() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the number of pages cached for a specific document.
     *
     * @param filePath The path of the PDF file
     * @return The number of pages cached
     */
    public int getPageCount(String filePath) {
        lock.readLock().lock();
        try {
            Map<CacheKey, SoftReference<BufferedImage>> documentCache = cache.get(filePath);
            return documentCache != null ? documentCache.size() : 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the total number of pages cached across all documents.
     *
     * @return The total number of pages
     */
    public int getTotalPageCount() {
        lock.readLock().lock();
        try {
            return cache.values().stream()
                    .mapToInt(Map::size)
                    .sum();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the set of document file paths in the cache.
     *
     * @return An unmodifiable set of file paths
     */
    public Set<String> getCachedDocuments() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableSet(cache.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Adjusts the cache size based on available memory.
     */
    public void adjustCacheSize() {
        lock.writeLock().lock();
        try {
            // Calculate memory usage
            double memoryUsage = getMemoryUsage();

            if (memoryUsage > memoryThreshold) {
                // Reduce cache size
                int reduction = (int)((memoryUsage - memoryThreshold) * 10);
                maxPagesPerDocument = Math.max(5, maxPagesPerDocument - reduction);

                // Enforce the new limits
                reduceMemoryUsage();

                logger.info("Reduced cache size due to high memory usage: maxPagesPerDocument={}", maxPagesPerDocument);
            } else if (memoryUsage < memoryThreshold * 0.7) {
                // Increase cache size if memory usage is well below threshold
                maxPagesPerDocument = Math.min(100, maxPagesPerDocument + 5);
                logger.info("Increased cache size due to low memory usage: maxPagesPerDocument={}", maxPagesPerDocument);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the current memory usage as a ratio (0.0-1.0).
     *
     * @return The memory usage ratio
     */
    public double getMemoryUsage() {
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return (double) usedMemory / maxMemory;
    }

    /**
     * Checks if memory usage is above the threshold.
     *
     * @return true if memory usage is high, false otherwise
     */
    private boolean isMemoryUsageHigh() {
        return getMemoryUsage() > memoryThreshold;
    }

    /**
     * Reduces memory usage by evicting cache entries.
     */
    private void reduceMemoryUsage() {
        lock.writeLock().lock();
        try {
            logger.info("Reducing memory usage, current usage: {}%", (int)(getMemoryUsage() * 100));

            // First try to evict least recently used pages from each document
            for (String filePath : new ArrayList<>(cache.keySet())) {
                evictLeastRecentlyUsedPage(filePath);
            }

            // If memory is still high, evict entire documents
            if (isMemoryUsageHigh() && !cache.isEmpty()) {
                evictLeastRecentlyUsedDocument();
            }

            // Request garbage collection
            System.gc();

            logger.info("Memory usage after reduction: {}%", (int)(getMemoryUsage() * 100));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Tries to find a higher quality version of a page that can be downscaled.
     *
     * @param documentCache The document cache
     * @param pageNumber The page number
     * @param zoomFactor The zoom factor
     * @param requestedQuality The requested quality
     * @return An Optional containing a higher quality image if found, or empty if not found
     */
    private Optional<BufferedImage> findHigherQualityVersion(
            Map<CacheKey, SoftReference<BufferedImage>> documentCache,
            int pageNumber, double zoomFactor, RenderQuality requestedQuality) {

        // Try to find a higher quality version
        RenderQuality[] qualities = RenderQuality.values();
        for (int i = qualities.length - 1; i >= 0; i--) {
            RenderQuality quality = qualities[i];
            if (quality.ordinal() > requestedQuality.ordinal()) {
                CacheKey key = new CacheKey(pageNumber, zoomFactor, quality);
                SoftReference<BufferedImage> ref = documentCache.get(key);
                if (ref != null) {
                    BufferedImage image = ref.get();
                    if (image != null) {
                        // Found a higher quality version
                        logger.debug("Using higher quality version ({}->{})", requestedQuality, quality);
                        return Optional.of(image);
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Updates the LRU status for a document and page.
     *
     * @param filePath The document file path
     * @param cacheKey The page cache key
     */
    private void updateLruStatus(String filePath, CacheKey cacheKey) {
        // Update page LRU status
        Deque<CacheKey> lruTracker = lruTrackers.get(filePath);
        if (lruTracker != null) {
            lruTracker.remove(cacheKey);
            lruTracker.addFirst(cacheKey);
        }

        // Update document LRU status
        documentLruTracker.remove(filePath);
        documentLruTracker.addFirst(filePath);
    }

    /**
     * Evicts the least recently used page from a document's cache.
     *
     * @param filePath The document file path
     */
    private void evictLeastRecentlyUsedPage(String filePath) {
        Map<CacheKey, SoftReference<BufferedImage>> documentCache = cache.get(filePath);
        Deque<CacheKey> lruTracker = lruTrackers.get(filePath);

        if (documentCache != null && lruTracker != null && !lruTracker.isEmpty()) {
            CacheKey oldestKey = lruTracker.removeLast();
            documentCache.remove(oldestKey);

            logger.debug("Evicted least recently used page {} (zoom={}, quality={}) from document {}", 
                    oldestKey.pageNumber, oldestKey.zoomFactor, oldestKey.quality, filePath);
        }
    }

    /**
     * Evicts the least recently used document from the cache.
     */
    private void evictLeastRecentlyUsedDocument() {
        if (!documentLruTracker.isEmpty()) {
            String oldestDocument = documentLruTracker.removeLast();
            cache.remove(oldestDocument);
            lruTrackers.remove(oldestDocument);
            logger.debug("Evicted least recently used document {}", oldestDocument);
        }
    }

    /**
     * Cache key for a page with zoom factor and quality.
     */
    private static class CacheKey {
        final int pageNumber;
        final double zoomFactor;
        final RenderQuality quality;

        CacheKey(int pageNumber, double zoomFactor, RenderQuality quality) {
            this.pageNumber = pageNumber;
            this.zoomFactor = zoomFactor;
            this.quality = quality;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return pageNumber == cacheKey.pageNumber &&
                   Double.compare(cacheKey.zoomFactor, zoomFactor) == 0 &&
                   quality == cacheKey.quality;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pageNumber, zoomFactor, quality);
        }
    }

    /**
     * Render quality levels for different use cases.
     */
    public enum RenderQuality {
        LOW,     // For thumbnails and previews
        NORMAL,  // For standard viewing
        HIGH     // For printing and detailed viewing
    }
}
