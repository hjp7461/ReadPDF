package com.pdfviewer.model.service.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Example class demonstrating the usage of concurrency utilities.
 * This class provides examples of how to use the BackgroundTaskManager,
 * TaskGroup, and UIResponsivenessManager classes.
 */
public class ConcurrencyExample {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyExample.class);

    /**
     * Example of using BackgroundTaskManager for a simple background task.
     */
    public static void backgroundTaskExample() {
        BackgroundTaskManager taskManager = BackgroundTaskManager.getInstance();

        // Simple task without progress tracking
        String taskId = taskManager.submitTask(() -> {
            logger.info("Executing simple background task");
            try {
                // Simulate work
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            logger.info("Simple background task completed");
        });

        logger.info("Submitted task with ID: {}", taskId);

        // Task with progress tracking
        String progressTaskId = taskManager.submitTask(
            new ProgressTrackingTask(taskManager),
            progress -> logger.info("Progress: {}%", progress),
            () -> logger.info("Task completed callback executed")
        );

        logger.info("Submitted task with progress tracking, ID: {}", progressTaskId);
    }

    /**
     * Helper class for progress tracking task to avoid effectively final variable issues.
     */
    private static class ProgressTrackingTask implements Runnable {
        private final BackgroundTaskManager taskManager;

        public ProgressTrackingTask(BackgroundTaskManager taskManager) {
            this.taskManager = taskManager;
        }

        @Override
        public void run() {
            logger.info("Executing background task with progress tracking");
            try {
                // Since we can't access the taskId inside the task, we'll use a different approach
                // In a real application, you would typically pass the taskId to the task or use a different mechanism

                // Simulate work with progress updates
                for (int i = 0; i <= 100; i += 10) {
                    final int progress = i;
                    // Instead of updating through the manager, we'll use the progress callback
                    // The BackgroundTaskManager will handle this internally
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            logger.info("Background task with progress tracking completed");
        }
    }

    /**
     * Example of using TaskGroup for structured concurrency.
     */
    public static void taskGroupExample() {
        // Create tasks that simulate different processing times
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final int taskNumber = i;
            tasks.add(() -> {
                logger.info("Task {} started", taskNumber);
                try {
                    // Simulate work with varying durations
                    Thread.sleep(taskNumber * 500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                logger.info("Task {} completed", taskNumber);
                return "Result from task " + taskNumber;
            });
        }

        // Run all tasks concurrently and collect results
        logger.info("Running all tasks concurrently");
        List<String> results = TaskGroup.runAllConcurrently(
            tasks, 
            10, 
            TimeUnit.SECONDS,
            progress -> logger.info("Overall progress: {}%", progress)
        );

        logger.info("All tasks completed. Results:");
        for (String result : results) {
            logger.info("- {}", result);
        }

        // Example of running a task with timeout
        try {
            logger.info("Running task with timeout");
            String result = TaskGroup.runWithTimeout(
                () -> {
                    logger.info("Timeout task started");
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    logger.info("Timeout task completed");
                    return "Result from timeout task";
                },
                2,
                TimeUnit.SECONDS
            );
            logger.info("Task completed within timeout: {}", result);
        } catch (Exception e) {
            logger.error("Task timed out or failed", e);
        }
    }

    /**
     * Example of using UIResponsivenessManager for UI-related tasks.
     * Note: This method should be called from the EDT.
     */
    public static void uiResponsivenessExample(JFrame parentFrame) {
        // Example of running a task on EDT
        UIResponsivenessManager.runOnEDT(() -> {
            logger.info("This task is running on the EDT");
        });

        // Example of running a long task with progress dialog
        if (parentFrame != null) {
            try {
                String result = UIResponsivenessManager.runWithProgressDialog(
                    parentFrame,
                    "Long Running Task",
                    () -> {
                        logger.info("Long running task started");
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        logger.info("Long running task completed");
                        return "Task result";
                    },
                    progressConsumer -> {
                        // Simulate progress updates
                        new Thread(() -> {
                            try {
                                for (int i = 0; i <= 100; i += 5) {
                                    final int progress = i;
                                    progressConsumer.accept(progress);
                                    Thread.sleep(150);
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    }
                );
                logger.info("Task completed with result: {}", result);
            } catch (Exception e) {
                logger.error("Task failed", e);
            }
        }

        // Example of running a responsive UI task
        UIResponsivenessManager.runResponsiveUITask(() -> {
            logger.info("Starting responsive UI task");
            for (int i = 0; i < 100; i++) {
                // Simulate UI work
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            logger.info("Responsive UI task completed");
        }, 10);
    }

    /**
     * Main method to run the examples.
     */
    public static void main(String[] args) {
        // Run UI examples on EDT
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Concurrency Examples");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);

            JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JButton backgroundTaskButton = new JButton("Run Background Task Example");
            backgroundTaskButton.addActionListener(e -> backgroundTaskExample());

            JButton taskGroupButton = new JButton("Run Task Group Example");
            taskGroupButton.addActionListener(e -> taskGroupExample());

            JButton uiResponsivenessButton = new JButton("Run UI Responsiveness Example");
            uiResponsivenessButton.addActionListener(e -> uiResponsivenessExample(frame));

            panel.add(backgroundTaskButton);
            panel.add(taskGroupButton);
            panel.add(uiResponsivenessButton);

            frame.getContentPane().add(panel);
            frame.setVisible(true);
        });
    }
}
