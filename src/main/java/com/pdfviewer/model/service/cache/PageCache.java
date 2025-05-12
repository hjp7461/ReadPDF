package com.pdfviewer.model.service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A cache for rendered PDF pages with memory management and LRU eviction policy.
 * This class provides thread-safe access to cached page images and automatically
 * manages memory usage by limiting cache size and using soft references.
 */
public class PageCache {
    private static final Logger logger = LoggerFactory.getLogger(PageCache.class);
    
    // Default maximum number of pages to cache per document
    private static final int DEFAULT_MAX_PAGES_PER_DOCUMENT = 20;
    
    // Default maximum number of documents to cache
    private static final int DEFAULT_MAX_DOCUMENTS = 5;
    
    // Lock for thread safety
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Cache structure: filePath -> (cacheKey -> page image)
    private final Map<String, Map<Integer, SoftReference<BufferedImage>>> cache = new ConcurrentHashMap<>();
    
    // LRU tracking: filePath -> list of recently used cache keys (most recent at front)
    private final Map<String, Deque<Integer>> lruTrackers = new ConcurrentHashMap<>();
    
    // Document LRU tracking (most recently used documents)
    private final Deque<String> documentLruTracker = new LinkedList<>();
    
    // Configuration
    private final int maxPagesPerDocument;
    private final int maxDocuments;
    
    /**
     * Creates a new PageCache with default settings.
     */
    public PageCache() {
        this(DEFAULT_MAX_PAGES_PER_DOCUMENT, DEFAULT_MAX_DOCUMENTS);
    }
    
    /**
     * Creates a new PageCache with custom settings.
     *
     * @param maxPagesPerDocument Maximum number of pages to cache per document
     * @param maxDocuments Maximum number of documents to cache
     */
    public PageCache(int maxPagesPerDocument, int maxDocuments) {
        this.maxPagesPerDocument = maxPagesPerDocument;
        this.maxDocuments = maxDocuments;
        logger.info("Initialized PageCache with maxPagesPerDocument={}, maxDocuments={}", 
                maxPagesPerDocument, maxDocuments);
    }
    
    /**
     * Gets a page from the cache.
     *
     * @param filePath The path of the PDF file
     * @param pageNumber The page number
     * @return An Optional containing the cached image if found, or empty if not found
     */
    public Optional<BufferedImage> getPage(String filePath, int pageNumber) {
        return getPage(filePath, pageNumber, 0);
    }
    
    /**
     * Gets a page from the cache with a specific zoom factor.
     *
     * @param filePath The path of the PDF file
     * @param pageNumber The page number
     * @param zoomFactor The zoom factor (multiplied by 100 for the cache key)
     * @return An Optional containing the cached image if found, or empty if not found
     */
    public Optional<BufferedImage> getPage(String filePath, int pageNumber, double zoomFactor) {
        int cacheKey = createCacheKey(pageNumber, zoomFactor);
        
        lock.readLock().lock();
        try {
            Map<Integer, SoftReference<BufferedImage>> documentCache = cache.get(filePath);
            if (documentCache == null) {
                return Optional.empty();
            }
            
            SoftReference<BufferedImage> ref = documentCache.get(cacheKey);
            if (ref == null) {
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
        putPage(filePath, pageNumber, 0, image);
    }
    
    /**
     * Puts a page in the cache with a specific zoom factor.
     *
     * @param filePath The path of the PDF file
     * @param pageNumber The page number
     * @param zoomFactor The zoom factor (multiplied by 100 for the cache key)
     * @param image The rendered page image
     */
    public void putPage(String filePath, int pageNumber, double zoomFactor, BufferedImage image) {
        int cacheKey = createCacheKey(pageNumber, zoomFactor);
        
        lock.writeLock().lock();
        try {
            // Ensure the document cache exists
            Map<Integer, SoftReference<BufferedImage>> documentCache = 
                    cache.computeIfAbsent(filePath, k -> new ConcurrentHashMap<>());
            
            // Ensure the LRU tracker exists
            Deque<Integer> lruTracker = 
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
            
            logger.debug("Cached page {} (zoom={}) for document {}", pageNumber, zoomFactor, filePath);
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
            Map<Integer, SoftReference<BufferedImage>> documentCache = cache.get(filePath);
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
     * Creates a cache key from a page number and zoom factor.
     *
     * @param pageNumber The page number
     * @param zoomFactor The zoom factor
     * @return A unique cache key
     */
    private int createCacheKey(int pageNumber, double zoomFactor) {
        // For default zoom (0), just use the page number
        if (zoomFactor == 0) {
            return pageNumber;
        }
        // Otherwise, create a composite key
        return (pageNumber * 1000) + (int)(zoomFactor * 100);
    }
    
    /**
     * Updates the LRU status for a document and page.
     *
     * @param filePath The document file path
     * @param cacheKey The page cache key
     */
    private void updateLruStatus(String filePath, int cacheKey) {
        // Update page LRU status
        Deque<Integer> lruTracker = lruTrackers.get(filePath);
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
        Map<Integer, SoftReference<BufferedImage>> documentCache = cache.get(filePath);
        Deque<Integer> lruTracker = lruTrackers.get(filePath);
        
        if (documentCache != null && lruTracker != null && !lruTracker.isEmpty()) {
            Integer oldestKey = lruTracker.removeLast();
            documentCache.remove(oldestKey);
            
            // Extract page number from cache key for logging
            int pageNumber = oldestKey < 1000 ? oldestKey : oldestKey / 1000;
            logger.debug("Evicted least recently used page {} from document {}", pageNumber, filePath);
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
}