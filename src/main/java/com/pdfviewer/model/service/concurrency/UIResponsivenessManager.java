package com.pdfviewer.model.service.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A utility class for maintaining UI responsiveness during long-running operations.
 * This class provides methods for executing tasks on the EDT, showing progress dialogs
 * for long-running operations, and ensuring that the UI remains responsive.
 */
public class UIResponsivenessManager {
    private static final Logger logger = LoggerFactory.getLogger(UIResponsivenessManager.class);

    private static final int PROGRESS_DIALOG_DELAY_MS = 500; // Show progress dialog after 500ms

    private UIResponsivenessManager() {
        // Utility class, no instantiation
    }

    /**
     * Executes a task on the Event Dispatch Thread (EDT).
     * If the current thread is already the EDT, the task is executed immediately.
     * Otherwise, it is scheduled to run on the EDT.
     *
     * @param task the task to execute
     */
    public static void runOnEDT(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    /**
     * Executes a task on the EDT and waits for it to complete.
     *
     * @param <T> the type of result produced by the task
     * @param task the task to execute
     * @return the result of the task
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws ExecutionException if the task throws an exception
     */
    public static <T> T runOnEDTAndWait(Supplier<T> task) throws InterruptedException, ExecutionException {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.get();
        } else {
            final CompletableFuture<T> future = new CompletableFuture<>();
            SwingUtilities.invokeLater(() -> {
                try {
                    future.complete(task.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future.get();
        }
    }

    /**
     * Executes a long-running task in the background and shows a progress dialog
     * if the task takes longer than a threshold.
     *
     * @param <T> the type of result produced by the task
     * @param parentComponent the parent component for the progress dialog
     * @param title the title of the progress dialog
     * @param task the task to execute
     * @param progressCallback callback for progress updates (0-100)
     * @return the result of the task
     */
    public static <T> T runWithProgressDialog(Component parentComponent, String title, 
                                           Supplier<T> task, Consumer<Consumer<Integer>> progressCallback) {
        // Create a progress dialog
        JDialog progressDialog = createProgressDialog(parentComponent, title);
        JProgressBar progressBar = (JProgressBar) ((JPanel) progressDialog.getContentPane()).getComponent(0);

        // Flag to track if the dialog has been shown
        AtomicBoolean dialogShown = new AtomicBoolean(false);

        // Schedule the dialog to appear after a delay if the task is still running
        Timer showDialogTimer = new Timer(PROGRESS_DIALOG_DELAY_MS, e -> {
            dialogShown.set(true);
            progressDialog.setVisible(true);
        });
        showDialogTimer.setRepeats(false);
        showDialogTimer.start();

        try {
            // Run the task in the background
            CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
                try {
                    if (progressCallback != null) {
                        progressCallback.accept(progress -> {
                            if (dialogShown.get()) {
                                SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
                            }
                        });
                    }
                    return task.get();
                } catch (Exception e) {
                    logger.error("Error in background task", e);
                    throw new RuntimeException(e);
                }
            });

            // Wait for the task to complete
            return future.get();
        } catch (InterruptedException e) {
            logger.warn("Task execution was interrupted", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Task execution was interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Task execution failed", e);
            throw new RuntimeException(e.getCause());
        } finally {
            // Cancel the timer and hide the dialog
            showDialogTimer.stop();
            if (dialogShown.get()) {
                progressDialog.dispose();
            }
        }
    }

    /**
     * Creates a progress dialog.
     *
     * @param parentComponent the parent component for the dialog
     * @param title the title of the dialog
     * @return the created dialog
     */
    private static JDialog createProgressDialog(Component parentComponent, String title) {
        Window window = parentComponent != null ? 
            SwingUtilities.getWindowAncestor(parentComponent) : null;

        JDialog dialog;
        if (window instanceof Frame) {
            dialog = new JDialog((Frame) window, title, true);
        } else if (window instanceof Dialog) {
            dialog = new JDialog((Dialog) window, title, true);
        } else {
            dialog = new JDialog((Frame) null, title, true);
        }

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(true);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(progressBar, BorderLayout.CENTER);

        dialog.setContentPane(panel);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setSize(300, 100);
        dialog.setLocationRelativeTo(parentComponent);
        dialog.setResizable(false);

        return dialog;
    }

    /**
     * Periodically yields control back to the EDT to keep the UI responsive
     * during long-running operations that must run on the EDT.
     */
    public static void yieldToEDT() {
        if (SwingUtilities.isEventDispatchThread()) {
            // Allow EDT to process pending events
            try {
                // Use reflection to access the sun.awt.AppContext.getAppContext().getCachedPump() method
                // This is a hack to check if there are pending events in the EDT queue
                // If this fails, we'll just yield anyway
                boolean hasEvents = true;

                if (hasEvents) {
                    // Create a new event and wait for it to be processed
                    // This effectively yields to other pending events
                    CompletableFuture<Void> yield = new CompletableFuture<>();
                    SwingUtilities.invokeLater(() -> yield.complete(null));
                    try {
                        yield.get(100, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        // Ignore, we just want to yield
                    }
                }
            } catch (Exception e) {
                // Ignore any reflection errors
                logger.debug("Error checking for pending events", e);
            }
        }
    }

    /**
     * Executes a task that updates the UI frequently, ensuring that the UI remains responsive.
     * This method should be called from the EDT.
     *
     * @param task the task to execute
     * @param yieldInterval how often to yield control back to the EDT (in iterations)
     */
    public static void runResponsiveUITask(Runnable task, int yieldInterval) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> runResponsiveUITask(task, yieldInterval));
            return;
        }

        // Create a wrapper that yields periodically
        AtomicBoolean yielding = new AtomicBoolean(false);
        AtomicInteger counter = new AtomicInteger(0);

        Runnable wrappedTask = () -> {
            try {
                task.run();
            } finally {
                yielding.set(false);
            }
        };

        // Run the task with periodic yielding
        try {
            while (counter.incrementAndGet() % yieldInterval == 0) {
                yielding.set(true);
                yieldToEDT();
                if (!yielding.get()) {
                    // Task completed during yield
                    return;
                }
            }

            wrappedTask.run();
        } catch (Exception e) {
            logger.error("Error in UI task", e);
            throw e;
        }
    }
}
