package com.pdfviewer.model.service.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class MapBackedPageCacheTest {

    private MapBackedPageCache pageCache;
    private Map<String, Map<Integer, BufferedImage>> underlyingCache;
    
    private static final String TEST_FILE_PATH = "/path/to/test.pdf";
    private static final int TEST_PAGE_NUMBER = 5;
    private static final double TEST_ZOOM_FACTOR = 1.5;

    @BeforeEach
    void setUp() {
        underlyingCache = new ConcurrentHashMap<>();
        pageCache = new MapBackedPageCache(underlyingCache);
    }

    @Test
    void getPage_shouldReturnEmpty_whenPageNotInCache() {
        // Act
        Optional<BufferedImage> result = pageCache.getPage(TEST_FILE_PATH, TEST_PAGE_NUMBER);
        
        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void getPage_shouldReturnImage_whenPageInCache() {
        // Arrange
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Map<Integer, BufferedImage> documentCache = new ConcurrentHashMap<>();
        documentCache.put(TEST_PAGE_NUMBER, testImage);
        underlyingCache.put(TEST_FILE_PATH, documentCache);
        
        // Act
        Optional<BufferedImage> result = pageCache.getPage(TEST_FILE_PATH, TEST_PAGE_NUMBER);
        
        // Assert
        assertTrue(result.isPresent());
        assertSame(testImage, result.get());
    }

    @Test
    void getPageWithZoom_shouldReturnEmpty_whenPageNotInCache() {
        // Act
        Optional<BufferedImage> result = pageCache.getPage(TEST_FILE_PATH, TEST_PAGE_NUMBER, TEST_ZOOM_FACTOR);
        
        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void getPageWithZoom_shouldReturnImage_whenPageInCache() {
        // Arrange
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Map<Integer, BufferedImage> documentCache = new ConcurrentHashMap<>();
        // Create the same cache key as the implementation would
        int cacheKey = (TEST_PAGE_NUMBER * 1000) + (int)(TEST_ZOOM_FACTOR * 100);
        documentCache.put(cacheKey, testImage);
        underlyingCache.put(TEST_FILE_PATH, documentCache);
        
        // Act
        Optional<BufferedImage> result = pageCache.getPage(TEST_FILE_PATH, TEST_PAGE_NUMBER, TEST_ZOOM_FACTOR);
        
        // Assert
        assertTrue(result.isPresent());
        assertSame(testImage, result.get());
    }

    @Test
    void putPage_shouldAddPageToCache() {
        // Arrange
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        
        // Act
        pageCache.putPage(TEST_FILE_PATH, TEST_PAGE_NUMBER, testImage);
        
        // Assert
        Map<Integer, BufferedImage> documentCache = underlyingCache.get(TEST_FILE_PATH);
        assertNotNull(documentCache);
        assertSame(testImage, documentCache.get(TEST_PAGE_NUMBER));
    }

    @Test
    void putPageWithZoom_shouldAddPageToCache() {
        // Arrange
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        
        // Act
        pageCache.putPage(TEST_FILE_PATH, TEST_PAGE_NUMBER, TEST_ZOOM_FACTOR, testImage);
        
        // Assert
        Map<Integer, BufferedImage> documentCache = underlyingCache.get(TEST_FILE_PATH);
        assertNotNull(documentCache);
        
        // Create the same cache key as the implementation would
        int cacheKey = (TEST_PAGE_NUMBER * 1000) + (int)(TEST_ZOOM_FACTOR * 100);
        assertSame(testImage, documentCache.get(cacheKey));
    }

    @Test
    void clearDocumentCache_shouldRemoveDocumentFromCache() {
        // Arrange
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Map<Integer, BufferedImage> documentCache = new ConcurrentHashMap<>();
        documentCache.put(TEST_PAGE_NUMBER, testImage);
        underlyingCache.put(TEST_FILE_PATH, documentCache);
        
        // Act
        pageCache.clearDocumentCache(TEST_FILE_PATH);
        
        // Assert
        assertNull(underlyingCache.get(TEST_FILE_PATH));
    }

    @Test
    void clearAllCaches_shouldRemoveAllDocumentsFromCache() {
        // Arrange
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Map<Integer, BufferedImage> documentCache1 = new ConcurrentHashMap<>();
        documentCache1.put(TEST_PAGE_NUMBER, testImage);
        underlyingCache.put(TEST_FILE_PATH, documentCache1);
        
        Map<Integer, BufferedImage> documentCache2 = new ConcurrentHashMap<>();
        documentCache2.put(TEST_PAGE_NUMBER, testImage);
        underlyingCache.put("/path/to/another.pdf", documentCache2);
        
        // Act
        pageCache.clearAllCaches();
        
        // Assert
        assertTrue(underlyingCache.isEmpty());
    }

    @Test
    void getUnderlyingCache_shouldReturnUnderlyingCache() {
        // Act
        Map<String, Map<Integer, BufferedImage>> result = pageCache.getUnderlyingCache();
        
        // Assert
        assertSame(underlyingCache, result);
    }
}