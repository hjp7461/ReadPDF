/**
 * Memory usage monitoring and management system.
 * This class monitors the JVM's memory usage and provides notifications when memory usage
 * exceeds certain thresholds, allowing the application to take appropriate actions.
 */
package com.pdfviewer.model.service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MemoryMonitor {
    private static final Logger logger = LoggerFactory.getLogger(MemoryMonitor.class);
    
    // Default memory usage thresholds
    private static final double WARNING_THRESHOLD = 0.7;  // 70% of max memory
    private static final double CRITICAL_THRESHOLD = 0.85; // 85% of max memory
    
    // Default monitoring interval in seconds
    private static final int DEFAULT_MONITORING_INTERVAL = 5;
    
    // Runtime instance for memory monitoring
    private final Runtime runtime = Runtime.getRuntime();
    
    // Scheduled executor for periodic monitoring
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "MemoryMonitor");
        t.setDaemon(true);
        return t;
    });
    
    // Memory status listeners
    private final List<MemoryStatusListener> listeners = new CopyOnWriteArrayList<>();
    
    // Current memory status
    private MemoryStatus currentStatus = MemoryStatus.NORMAL;
    
    // Configuration
    private final double warningThreshold;
    private final double criticalThreshold;
    private final int monitoringInterval;
    
    // Flag to indicate if monitoring is active
    private boolean isMonitoring = false;
    
    /**
     * Creates a new MemoryMonitor with default settings.
     */
    public MemoryMonitor() {
        this(WARNING_THRESHOLD, CRITICAL_THRESHOLD, DEFAULT_MONITORING_INTERVAL);
    }
    
    /**
     * Creates a new MemoryMonitor with custom settings.
     *
     * @param warningThreshold Memory usage ratio (0.0-1.0) for warning status
     * @param criticalThreshold Memory usage ratio (0.0-1.0) for critical status
     * @param monitoringInterval Monitoring interval in seconds
     */
    public MemoryMonitor(double warningThreshold, double criticalThreshold, int monitoringInterval) {
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        this.monitoringInterval = monitoringInterval;
        
        logger.info("Initialized MemoryMonitor with warningThreshold={}, criticalThreshold={}, interval={}s",
                warningThreshold, criticalThreshold, monitoringInterval);
    }
    
    /**
     * Starts memory monitoring.
     */
    public void startMonitoring() {
        if (isMonitoring) {
            return;
        }
        
        isMonitoring = true;
        scheduler.scheduleAtFixedRate(
                this::checkMemoryUsage,
                0,
                monitoringInterval,
                TimeUnit.SECONDS);
        
        logger.info("Memory monitoring started with interval of {} seconds", monitoringInterval);
    }
    
    /**
     * Stops memory monitoring.
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }
        
        isMonitoring = false;
        scheduler.shutdown();
        
        logger.info("Memory monitoring stopped");
    }
    
    /**
     * Adds a memory status listener.
     *
     * @param listener The listener to add
     */
    public void addListener(MemoryStatusListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a memory status listener.
     *
     * @param listener The listener to remove
     */
    public void removeListener(MemoryStatusListener listener) {
        listeners.remove(listener);
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
     * Gets the current memory status.
     *
     * @return The current memory status
     */
    public MemoryStatus getCurrentStatus() {
        return currentStatus;
    }
    
    /**
     * Checks the current memory usage and notifies listeners if thresholds are exceeded.
     */
    private void checkMemoryUsage() {
        try {
            double memoryUsage = getMemoryUsage();
            MemoryStatus newStatus;
            
            if (memoryUsage >= criticalThreshold) {
                newStatus = MemoryStatus.CRITICAL;
            } else if (memoryUsage >= warningThreshold) {
                newStatus = MemoryStatus.WARNING;
            } else {
                newStatus = MemoryStatus.NORMAL;
            }
            
            // Log memory usage periodically (every minute) or on status change
            boolean shouldLog = System.currentTimeMillis() % 60000 < monitoringInterval * 1000 || newStatus != currentStatus;
            if (shouldLog) {
                logger.info("Memory usage: {}/{} MB ({}%)", 
                        (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024),
                        runtime.maxMemory() / (1024 * 1024),
                        (int) (memoryUsage * 100));
            }
            
            // Notify listeners if status changed
            if (newStatus != currentStatus) {
                MemoryStatus oldStatus = currentStatus;
                currentStatus = newStatus;
                
                if (newStatus == MemoryStatus.WARNING) {
                    logger.warn("Memory usage warning: {}%", (int) (memoryUsage * 100));
                } else if (newStatus == MemoryStatus.CRITICAL) {
                    logger.error("Memory usage critical: {}%", (int) (memoryUsage * 100));
                } else if (oldStatus != MemoryStatus.NORMAL) {
                    logger.info("Memory usage returned to normal: {}%", (int) (memoryUsage * 100));
                }
                
                notifyListeners(oldStatus, newStatus);
            }
        } catch (Exception e) {
            logger.error("Error checking memory usage", e);
        }
    }
    
    /**
     * Notifies all listeners of a memory status change.
     *
     * @param oldStatus The previous memory status
     * @param newStatus The new memory status
     */
    private void notifyListeners(MemoryStatus oldStatus, MemoryStatus newStatus) {
        for (MemoryStatusListener listener : listeners) {
            try {
                listener.onMemoryStatusChanged(oldStatus, newStatus);
            } catch (Exception e) {
                logger.error("Error notifying memory status listener", e);
            }
        }
    }
    
    /**
     * Requests garbage collection to free memory.
     * This method should be used sparingly, as it can impact performance.
     */
    public void requestGarbageCollection() {
        logger.info("Requesting garbage collection");
        System.gc();
    }
    
    /**
     * Memory status levels.
     */
    public enum MemoryStatus {
        NORMAL,   // Memory usage is normal
        WARNING,  // Memory usage is high
        CRITICAL  // Memory usage is critically high
    }
    
    /**
     * Interface for memory status change listeners.
     */
    public interface MemoryStatusListener {
        /**
         * Called when the memory status changes.
         *
         * @param oldStatus The previous memory status
         * @param newStatus The new memory status
         */
        void onMemoryStatusChanged(MemoryStatus oldStatus, MemoryStatus newStatus);
    }
}