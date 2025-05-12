package com.pdfviewer.model.service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A PageCache implementation that wraps the existing Map-based cache.
 * This class provides a bridge between the old Map-based cache and the new PageCache interface.
 */
public class MapBackedPageCache {
    private static final Logger logger = LoggerFactory.getLogger(MapBackedPageCache.class);
    
    // The existing Map-based cache
    private final Map<String, Map<Integer, BufferedImage>> cache;
    
    /**
     * Creates a new MapBackedPageCache that wraps the existing Map-based cache.
     *
     * @param cache The existing Map-based cache
     */
    public MapBackedPageCache(Map<String, Map<Integer, BufferedImage>> cache) {
        this.cache = cache;
    }
    
    /**
     * Gets a page from the cache.
     *
     * @param filePath The path of the PDF file
     * @param pageNumber The page number
     * @return An Optional containing the cached image if found, or empty if not found
     */
    public Optional<BufferedImage> getPage(String filePath, int pageNumber) {
        Map<Integer, BufferedImage> documentCache = cache.get(filePath);
        if (documentCache == null) {
            return Optional.empty();
        }
        
        BufferedImage image = documentCache.get(pageNumber);
        return Optional.ofNullable(image);
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
        Map<Integer, BufferedImage> documentCache = cache.get(filePath);
        if (documentCache == null) {
            return Optional.empty();
        }
        
        int cacheKey = createCacheKey(pageNumber, zoomFactor);
        BufferedImage image = documentCache.get(cacheKey);
        return Optional.ofNullable(image);
    }
    
    /**
     * Puts a page in the cache.
     *
     * @param filePath The path of the PDF file
     * @param pageNumber The page number
     * @param image The rendered page image
     */
    public void putPage(String filePath, int pageNumber, BufferedImage image) {
        Map<Integer, BufferedImage> documentCache = cache.computeIfAbsent(filePath, k -> new ConcurrentHashMap<>());
        documentCache.put(pageNumber, image);
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
        Map<Integer, BufferedImage> documentCache = cache.computeIfAbsent(filePath, k -> new ConcurrentHashMap<>());
        int cacheKey = createCacheKey(pageNumber, zoomFactor);
        documentCache.put(cacheKey, image);
    }
    
    /**
     * Clears the cache for a specific document.
     *
     * @param filePath The path of the PDF file
     */
    public void clearDocumentCache(String filePath) {
        cache.remove(filePath);
    }
    
    /**
     * Clears the entire cache.
     */
    public void clearAllCaches() {
        cache.clear();
    }
    
    /**
     * Gets the underlying Map-based cache.
     * This method is provided for backward compatibility with existing code.
     *
     * @return The underlying Map-based cache
     */
    public Map<String, Map<Integer, BufferedImage>> getUnderlyingCache() {
        return cache;
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
}