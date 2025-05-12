package com.pdfviewer.model.service.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A utility class for organizing related concurrent tasks and properly handling
 * their results or failures. This provides similar functionality to JDK 21's
 * StructuredTaskScope but uses standard Java concurrency APIs.
 */
public class TaskGroup<T> implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(TaskGroup.class);
    
    private final ExecutorService executor;
    private final List<Future<T>> futures = new ArrayList<>();
    private final boolean shutdownExecutorOnClose;
    
    /**
     * Creates a new TaskGroup with a virtual thread per task executor.
     */
    public TaskGroup() {
        this(Executors.newVirtualThreadPerTaskExecutor(), true);
    }
    
    /**
     * Creates a new TaskGroup with the specified executor.
     *
     * @param executor the executor to use for running tasks
     * @param shutdownExecutorOnClose whether to shutdown the executor when this group is closed
     */
    public TaskGroup(ExecutorService executor, boolean shutdownExecutorOnClose) {
        this.executor = executor;
        this.shutdownExecutorOnClose = shutdownExecutorOnClose;
    }
    
    /**
     * Submits a task to be executed by this group.
     *
     * @param task the task to execute
     * @return a Future representing the result of the task
     */
    public Future<T> submit(Callable<T> task) {
        Future<T> future = executor.submit(task);
        futures.add(future);
        return future;
    }
    
    /**
     * Waits for all tasks to complete and returns their results.
     * If any task fails, its exception is logged but other tasks continue.
     *
     * @param timeout maximum time to wait for all tasks to complete
     * @param timeUnit the time unit of the timeout
     * @return a list of successful results
     */
    public List<T> joinAll(long timeout, TimeUnit timeUnit) {
        List<T> results = new ArrayList<>();
        Instant deadline = Instant.now().plus(Duration.ofMillis(timeUnit.toMillis(timeout)));
        
        for (Future<T> future : futures) {
            try {
                long remainingMillis = Duration.between(Instant.now(), deadline).toMillis();
                if (remainingMillis <= 0) {
                    logger.warn("Timeout reached while waiting for tasks to complete");
                    break;
                }
                
                T result = future.get(remainingMillis, TimeUnit.MILLISECONDS);
                results.add(result);
            } catch (InterruptedException e) {
                logger.warn("Task execution was interrupted", e);
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                logger.error("Task execution failed", e);
                // Continue with other tasks
            } catch (TimeoutException e) {
                logger.warn("Timeout reached while waiting for task to complete", e);
                break;
            }
        }
        
        return results;
    }
    
    /**
     * Waits for the first task to complete successfully and returns its result.
     * If all tasks fail, an exception is thrown.
     *
     * @param timeout maximum time to wait for a successful result
     * @param timeUnit the time unit of the timeout
     * @return the first successful result
     * @throws ExecutionException if all tasks fail
     * @throws TimeoutException if the timeout is reached before any task completes successfully
     */
    public T joinAny(long timeout, TimeUnit timeUnit) throws ExecutionException, TimeoutException {
        ExecutionException lastException = null;
        Instant deadline = Instant.now().plus(Duration.ofMillis(timeUnit.toMillis(timeout)));
        
        while (!futures.isEmpty() && Instant.now().isBefore(deadline)) {
            List<Future<T>> incompleteFutures = new ArrayList<>();
            
            for (Future<T> future : futures) {
                try {
                    long remainingMillis = Duration.between(Instant.now(), deadline).toMillis();
                    if (remainingMillis <= 0) {
                        throw new TimeoutException("Timeout reached while waiting for tasks to complete");
                    }
                    
                    return future.get(remainingMillis, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    logger.warn("Task execution was interrupted", e);
                    Thread.currentThread().interrupt();
                    throw new ExecutionException("Task execution was interrupted", e);
                } catch (ExecutionException e) {
                    lastException = e;
                    // Try next task
                } catch (TimeoutException e) {
                    if (!future.isDone()) {
                        incompleteFutures.add(future);
                    }
                }
            }
            
            futures.clear();
            futures.addAll(incompleteFutures);
            
            if (futures.isEmpty() && lastException != null) {
                throw lastException;
            }
            
            // Small delay to avoid busy waiting
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ExecutionException("Task execution was interrupted", e);
            }
        }
        
        throw new TimeoutException("Timeout reached while waiting for tasks to complete");
    }
    
    /**
     * Cancels all running tasks.
     */
    public void cancelAll() {
        for (Future<T> future : futures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        futures.clear();
    }
    
    @Override
    public void close() {
        cancelAll();
        if (shutdownExecutorOnClose) {
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
    }
    
    /**
     * Utility method to run multiple tasks concurrently and return all successful results.
     *
     * @param <R> the type of result produced by the tasks
     * @param tasks the tasks to execute
     * @param timeout maximum time to wait for all tasks to complete
     * @param timeUnit the time unit of the timeout
     * @return a list of successful results
     */
    public static <R> List<R> runAllConcurrently(List<Callable<R>> tasks, long timeout, TimeUnit timeUnit) {
        try (TaskGroup<R> group = new TaskGroup<>()) {
            for (Callable<R> task : tasks) {
                group.submit(task);
            }
            return group.joinAll(timeout, timeUnit);
        }
    }
    
    /**
     * Utility method to run multiple tasks concurrently with progress tracking.
     *
     * @param <R> the type of result produced by the tasks
     * @param tasks the tasks to execute
     * @param timeout maximum time to wait for all tasks to complete
     * @param timeUnit the time unit of the timeout
     * @param progressCallback callback for progress updates (0-100)
     * @return a list of successful results
     */
    public static <R> List<R> runAllConcurrently(List<Callable<R>> tasks, long timeout, TimeUnit timeUnit,
                                              Consumer<Integer> progressCallback) {
        int totalTasks = tasks.size();
        AtomicInteger completedTasks = new AtomicInteger(0);
        
        List<Callable<R>> wrappedTasks = new ArrayList<>();
        for (Callable<R> task : tasks) {
            wrappedTasks.add(() -> {
                try {
                    R result = task.call();
                    
                    // Update progress on EDT
                    int completed = completedTasks.incrementAndGet();
                    int progressPercent = (completed * 100) / totalTasks;
                    SwingUtilities.invokeLater(() -> progressCallback.accept(progressPercent));
                    
                    return result;
                } catch (Exception e) {
                    completedTasks.incrementAndGet();
                    throw e;
                }
            });
        }
        
        return runAllConcurrently(wrappedTasks, timeout, timeUnit);
    }
    
    /**
     * Utility method to run a task with a timeout.
     *
     * @param <R> the type of result produced by the task
     * @param task the task to execute
     * @param timeout maximum time to wait for the task to complete
     * @param timeUnit the time unit of the timeout
     * @return the result of the task
     * @throws ExecutionException if the task fails
     * @throws TimeoutException if the timeout is reached before the task completes
     */
    public static <R> R runWithTimeout(Callable<R> task, long timeout, TimeUnit timeUnit) 
            throws ExecutionException, TimeoutException {
        try (TaskGroup<R> group = new TaskGroup<>()) {
            group.submit(task);
            List<R> results = group.joinAll(timeout, timeUnit);
            if (results.isEmpty()) {
                throw new TimeoutException("Task did not complete within the specified timeout");
            }
            return results.get(0);
        }
    }
}