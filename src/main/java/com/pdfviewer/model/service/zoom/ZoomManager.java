package com.pdfviewer.model.service.zoom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages zoom levels for PDF rendering.
 * This class provides methods to zoom in, zoom out, and set specific zoom levels.
 */
public class ZoomManager {
    private static final Logger logger = LoggerFactory.getLogger(ZoomManager.class);
    
    // Default zoom levels (in percentage)
    private static final double[] ZOOM_LEVELS = {
        25, 50, 75, 100, 125, 150, 175, 200, 300, 400, 500
    };
    
    // Default zoom level index (100%)
    private static final int DEFAULT_ZOOM_LEVEL_INDEX = 3;
    
    // Current zoom level index
    private int currentZoomLevelIndex;
    
    /**
     * Creates a new ZoomManager with the default zoom level (100%).
     */
    public ZoomManager() {
        this.currentZoomLevelIndex = DEFAULT_ZOOM_LEVEL_INDEX;
    }
    
    /**
     * Creates a new ZoomManager with the specified initial zoom level.
     *
     * @param initialZoomFactor The initial zoom factor (e.g., 1.0 for 100%)
     */
    public ZoomManager(double initialZoomFactor) {
        setZoomFactor(initialZoomFactor);
    }
    
    /**
     * Gets the current zoom factor.
     *
     * @return The current zoom factor (e.g., 1.0 for 100%)
     */
    public double getZoomFactor() {
        return ZOOM_LEVELS[currentZoomLevelIndex] / 100.0;
    }
    
    /**
     * Gets the current zoom level as a percentage.
     *
     * @return The current zoom level as a percentage (e.g., 100 for 100%)
     */
    public double getZoomPercentage() {
        return ZOOM_LEVELS[currentZoomLevelIndex];
    }
    
    /**
     * Sets the zoom factor to the specified value.
     * If the specified zoom factor doesn't match any predefined zoom level,
     * the closest zoom level will be selected.
     *
     * @param zoomFactor The zoom factor to set (e.g., 1.0 for 100%)
     * @return The actual zoom factor that was set
     */
    public double setZoomFactor(double zoomFactor) {
        double percentage = zoomFactor * 100;
        
        // Find the closest zoom level
        int closestIndex = 0;
        double minDifference = Math.abs(ZOOM_LEVELS[0] - percentage);
        
        for (int i = 1; i < ZOOM_LEVELS.length; i++) {
            double difference = Math.abs(ZOOM_LEVELS[i] - percentage);
            if (difference < minDifference) {
                minDifference = difference;
                closestIndex = i;
            }
        }
        
        currentZoomLevelIndex = closestIndex;
        logger.debug("Zoom level set to {}%", getZoomPercentage());
        return getZoomFactor();
    }
    
    /**
     * Zooms in to the next predefined zoom level.
     *
     * @return The new zoom factor after zooming in
     */
    public double zoomIn() {
        if (currentZoomLevelIndex < ZOOM_LEVELS.length - 1) {
            currentZoomLevelIndex++;
            logger.debug("Zoomed in to {}%", getZoomPercentage());
        } else {
            logger.debug("Already at maximum zoom level ({}%)", getZoomPercentage());
        }
        return getZoomFactor();
    }
    
    /**
     * Zooms out to the previous predefined zoom level.
     *
     * @return The new zoom factor after zooming out
     */
    public double zoomOut() {
        if (currentZoomLevelIndex > 0) {
            currentZoomLevelIndex--;
            logger.debug("Zoomed out to {}%", getZoomPercentage());
        } else {
            logger.debug("Already at minimum zoom level ({}%)", getZoomPercentage());
        }
        return getZoomFactor();
    }
    
    /**
     * Resets the zoom level to the default (100%).
     *
     * @return The default zoom factor (1.0)
     */
    public double resetZoom() {
        currentZoomLevelIndex = DEFAULT_ZOOM_LEVEL_INDEX;
        logger.debug("Zoom level reset to {}%", getZoomPercentage());
        return getZoomFactor();
    }
    
    /**
     * Checks if zooming in is possible.
     *
     * @return true if zooming in is possible, false otherwise
     */
    public boolean canZoomIn() {
        return currentZoomLevelIndex < ZOOM_LEVELS.length - 1;
    }
    
    /**
     * Checks if zooming out is possible.
     *
     * @return true if zooming out is possible, false otherwise
     */
    public boolean canZoomOut() {
        return currentZoomLevelIndex > 0;
    }
}