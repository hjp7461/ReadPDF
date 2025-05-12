/**
 * Intelligent page preloading system that predicts which pages will be viewed next.
 * This class analyzes user navigation patterns and preloads pages accordingly to
 * improve the perceived performance of the PDF viewer.
 */
package com.pdfviewer.model.service.cache;

import com.pdfviewer.model.entity.PdfDocument;
import com.pdfviewer.model.service.renderer.PdfRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class IntelligentPreloader {
    private static final Logger logger = LoggerFactory.getLogger(IntelligentPreloader.class);
    
    // Maximum navigation history size
    private static final int MAX_HISTORY_SIZE = 20;
    
    // Maximum number of pages to preload at once
    private static final int MAX_PRELOAD_PAGES = 5;
    
    // The PDF renderer to use for rendering pages
    private final PdfRenderer renderer;
    
    // The page cache to store rendered pages
    private final PageRenderCache pageCache;
    
    // The current document being viewed
    private PdfDocument currentDocument;
    
    // The current page being viewed
    private final AtomicInteger currentPage = new AtomicInteger(0);
    
    // Navigation history (most recent at the end)
    private final LinkedList<Integer> navigationHistory = new LinkedList<>();
    
    // Executor service for background preloading
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    
    // Flag to indicate if preloading is enabled
    private boolean preloadingEnabled = true;
    
    /**
     * Creates a new IntelligentPreloader.
     *
     * @param renderer The PDF renderer to use
     * @param pageCache The page cache to store rendered pages
     */
    public IntelligentPreloader(PdfRenderer renderer, PageRenderCache pageCache) {
        this.renderer = renderer;
        this.pageCache = pageCache;
    }
    
    /**
     * Sets the current document being viewed.
     *
     * @param document The current document
     */
    public void setDocument(PdfDocument document) {
        this.currentDocument = document;
        this.navigationHistory.clear();
        this.currentPage.set(0);
    }
    
    /**
     * Sets the current page being viewed and triggers preloading of adjacent pages.
     *
     * @param pageNumber The current page number (1-based)
     */
    public void setCurrentPage(int pageNumber) {
        if (currentDocument == null) {
            return;
        }
        
        int oldPage = currentPage.getAndSet(pageNumber);
        
        // Only record navigation if it's a different page
        if (oldPage != pageNumber && oldPage > 0) {
            recordNavigation(oldPage, pageNumber);
        }
        
        // Preload pages if enabled
        if (preloadingEnabled) {
            preloadAdjacentPages(pageNumber);
        }
    }
    
    /**
     * Enables or disables preloading.
     *
     * @param enabled true to enable preloading, false to disable
     */
    public void setPreloadingEnabled(boolean enabled) {
        this.preloadingEnabled = enabled;
        logger.info("Page preloading {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Records a navigation from one page to another and updates the navigation history.
     *
     * @param fromPage The page navigated from
     * @param toPage The page navigated to
     */
    private void recordNavigation(int fromPage, int toPage) {
        // Add the new page to the history
        navigationHistory.add(toPage);
        
        // Trim history if it gets too large
        if (navigationHistory.size() > MAX_HISTORY_SIZE) {
            navigationHistory.removeFirst();
        }
        
        logger.debug("Recorded navigation from page {} to {}", fromPage, toPage);
    }
    
    /**
     * Preloads pages adjacent to the current page based on the detected navigation pattern.
     *
     * @param currentPage The current page number
     */
    private void preloadAdjacentPages(int currentPage) {
        if (currentDocument == null || renderer == null) {
            return;
        }
        
        // Determine the navigation pattern
        NavigationPattern pattern = detectNavigationPattern();
        
        // Get the list of pages to preload based on the pattern
        List<Integer> pagesToPreload = getPagesToPreload(currentPage, pattern);
        
        // Preload the pages in the background
        preloadPages(pagesToPreload);
        
        logger.debug("Preloading {} pages based on {} navigation pattern", 
                pagesToPreload.size(), pattern);
    }
    
    /**
     * Detects the user's navigation pattern based on the navigation history.
     *
     * @return The detected navigation pattern
     */
    private NavigationPattern detectNavigationPattern() {
        if (navigationHistory.size() < 3) {
            return NavigationPattern.SEQUENTIAL_FORWARD;
        }
        
        int forwardCount = 0;
        int backwardCount = 0;
        int jumpCount = 0;
        
        // Analyze the last few navigations
        Integer prev = null;
        for (Integer page : navigationHistory) {
            if (prev != null) {
                int diff = page - prev;
                if (diff == 1) {
                    forwardCount++;
                } else if (diff == -1) {
                    backwardCount++;
                } else {
                    jumpCount++;
                }
            }
            prev = page;
        }
        
        // Determine the dominant pattern
        if (jumpCount > Math.max(forwardCount, backwardCount)) {
            return NavigationPattern.RANDOM_ACCESS;
        } else if (forwardCount > backwardCount) {
            return NavigationPattern.SEQUENTIAL_FORWARD;
        } else {
            return NavigationPattern.SEQUENTIAL_BACKWARD;
        }
    }
    
    /**
     * Gets the list of pages to preload based on the current page and navigation pattern.
     *
     * @param currentPage The current page number
     * @param pattern The detected navigation pattern
     * @return The list of pages to preload
     */
    private List<Integer> getPagesToPreload(int currentPage, NavigationPattern pattern) {
        List<Integer> pagesToPreload = new ArrayList<>();
        
        switch (pattern) {
            case SEQUENTIAL_FORWARD:
                // Preload next pages
                for (int i = 1; i <= MAX_PRELOAD_PAGES; i++) {
                    int pageToPreload = currentPage + i;
                    if (isValidPage(pageToPreload)) {
                        pagesToPreload.add(pageToPreload);
                    }
                }
                break;
                
            case SEQUENTIAL_BACKWARD:
                // Preload previous pages
                for (int i = 1; i <= MAX_PRELOAD_PAGES; i++) {
                    int pageToPreload = currentPage - i;
                    if (isValidPage(pageToPreload)) {
                        pagesToPreload.add(pageToPreload);
                    }
                }
                break;
                
            case RANDOM_ACCESS:
                // Preload both next and previous pages, but fewer of each
                for (int i = 1; i <= 2; i++) {
                    int nextPage = currentPage + i;
                    if (isValidPage(nextPage)) {
                        pagesToPreload.add(nextPage);
                    }
                    
                    int prevPage = currentPage - i;
                    if (isValidPage(prevPage)) {
                        pagesToPreload.add(prevPage);
                    }
                }
                
                // For random access, also try to preload pages that were visited before
                for (Integer historicPage : navigationHistory) {
                    if (pagesToPreload.size() >= MAX_PRELOAD_PAGES) {
                        break;
                    }
                    if (historicPage != currentPage && isValidPage(historicPage) && !pagesToPreload.contains(historicPage)) {
                        pagesToPreload.add(historicPage);
                    }
                }
                break;
        }
        
        return pagesToPreload;
    }
    
    /**
     * Preloads a list of pages in the background.
     *
     * @param pagesToPreload The list of pages to preload
     */
    private void preloadPages(List<Integer> pagesToPreload) {
        if (pagesToPreload.isEmpty()) {
            return;
        }
        
        String filePath = currentDocument.getFilePath();
        
        // Submit preloading tasks to the executor
        for (Integer pageNumber : pagesToPreload) {
            executor.submit(() -> {
                try {
                    // Check if the page is already cached
                    if (pageCache.getPage(filePath, pageNumber).isPresent()) {
                        return;
                    }
                    
                    logger.debug("Preloading page {}", pageNumber);
                    
                    // Render the page at normal quality
                    BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, 150);
                    
                    // Cache the rendered page
                    pageCache.putPage(filePath, pageNumber, image);
                    
                } catch (IOException e) {
                    logger.warn("Error preloading page {}: {}", pageNumber, e.getMessage());
                } catch (Exception e) {
                    logger.error("Unexpected error preloading page {}", pageNumber, e);
                }
            });
        }
    }
    
    /**
     * Checks if a page number is valid for the current document.
     *
     * @param pageNumber The page number to check
     * @return true if the page number is valid, false otherwise
     */
    private boolean isValidPage(int pageNumber) {
        return pageNumber >= 1 && pageNumber <= currentDocument.getPageCount();
    }
    
    /**
     * Shuts down the preloader and releases resources.
     */
    public void shutdown() {
        executor.shutdown();
    }
    
    /**
     * Navigation patterns that can be detected.
     */
    private enum NavigationPattern {
        SEQUENTIAL_FORWARD,   // User is moving forward through the document
        SEQUENTIAL_BACKWARD,  // User is moving backward through the document
        RANDOM_ACCESS         // User is jumping to random pages
    }
}