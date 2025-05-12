package com.pdfviewer.model.service.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * A centralized manager for handling background tasks using JDK 21 virtual threads.
 * This class provides methods for submitting tasks, tracking their progress,
 * and ensuring UI updates happen on the Event Dispatch Thread (EDT).
 */
public class BackgroundTaskManager {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundTaskManager.class);
    
    private static final BackgroundTaskManager INSTANCE = new BackgroundTaskManager();
    
    private final ExecutorService executor;
    private final Map<String, Future<?>> activeTasks;
    private final Map<String, TaskInfo> taskInfoMap;
    
    private BackgroundTaskManager() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.activeTasks = new ConcurrentHashMap<>();
        this.taskInfoMap = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets the singleton instance of the BackgroundTaskManager.
     *
     * @return the singleton instance
     */
    public static BackgroundTaskManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Submits a task to be executed in the background using virtual threads.
     *
     * @param task the task to execute
     * @return a unique ID for the submitted task
     */
    public String submitTask(Runnable task) {
        return submitTask(task, null, null);
    }
    
    /**
     * Submits a task to be executed in the background using virtual threads,
     * with progress updates and completion callback.
     *
     * @param task the task to execute
     * @param progressCallback callback for progress updates (0-100)
     * @param completionCallback callback when task completes
     * @return a unique ID for the submitted task
     */
    public String submitTask(Runnable task, Consumer<Integer> progressCallback, 
                            Runnable completionCallback) {
        String taskId = UUID.randomUUID().toString();
        
        TaskInfo taskInfo = new TaskInfo(taskId, progressCallback);
        taskInfoMap.put(taskId, taskInfo);
        
        Future<?> future = executor.submit(() -> {
            try {
                logger.debug("Starting background task: {}", taskId);
                task.run();
                logger.debug("Completed background task: {}", taskId);
                
                // Execute completion callback on EDT if provided
                if (completionCallback != null) {
                    SwingUtilities.invokeLater(completionCallback);
                }
            } catch (Exception e) {
                logger.error("Error in background task: {}", taskId, e);
            } finally {
                activeTasks.remove(taskId);
                taskInfoMap.remove(taskId);
            }
        });
        
        activeTasks.put(taskId, future);
        return taskId;
    }
    
    /**
     * Updates the progress of a task.
     * This method should be called from within the task.
     *
     * @param taskId the ID of the task
     * @param progress the progress value (0-100)
     */
    public void updateProgress(String taskId, int progress) {
        TaskInfo taskInfo = taskInfoMap.get(taskId);
        if (taskInfo != null && taskInfo.progressCallback != null) {
            int clampedProgress = Math.max(0, Math.min(100, progress));
            SwingUtilities.invokeLater(() -> taskInfo.progressCallback.accept(clampedProgress));
        }
    }
    
    /**
     * Cancels a running task.
     *
     * @param taskId the ID of the task to cancel
     * @return true if the task was cancelled, false otherwise
     */
    public boolean cancelTask(String taskId) {
        Future<?> future = activeTasks.get(taskId);
        if (future != null && !future.isDone()) {
            logger.debug("Cancelling task: {}", taskId);
            boolean result = future.cancel(true);
            if (result) {
                activeTasks.remove(taskId);
                taskInfoMap.remove(taskId);
            }
            return result;
        }
        return false;
    }
    
    /**
     * Cancels all running tasks.
     */
    public void cancelAllTasks() {
        logger.debug("Cancelling all background tasks");
        for (Map.Entry<String, Future<?>> entry : activeTasks.entrySet()) {
            entry.getValue().cancel(true);
        }
        activeTasks.clear();
        taskInfoMap.clear();
    }
    
    /**
     * Shuts down the executor service.
     * This should be called when the application is closing.
     */
    public void shutdown() {
        logger.debug("Shutting down BackgroundTaskManager");
        cancelAllTasks();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Internal class to store task information.
     */
    private static class TaskInfo {
        private final String id;
        private final Consumer<Integer> progressCallback;
        
        public TaskInfo(String id, Consumer<Integer> progressCallback) {
            this.id = id;
            this.progressCallback = progressCallback;
        }
    }
}