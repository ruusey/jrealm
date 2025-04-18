package com.jrealm.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkerThread {
    private static final int THREAD_POOL_COUNT = 40;
    private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
            .newFixedThreadPool(WorkerThread.THREAD_POOL_COUNT, Executors.privilegedThreadFactory());

    public static CompletableFuture<?> submit(Runnable runnable) {
        if (runnable == null)
            return null;
        return CompletableFuture.runAsync(runnable, WorkerThread.executor);
    }
    
    public static void runLater(Runnable runnable, long ms) {
    	final Runnable wrappedTask = () ->{
    		try{
    			Thread.sleep(ms);
    			runnable.run();
    		}catch(Exception e) {
    			log.error("Failed to execute runnable in the future. Reason: {}", e.getMessage());
    		}
    	};
    	submitAndRun(wrappedTask);		
    }

    public static CompletableFuture<Void> doAsync(Runnable task) {
        final CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, executor);
        return cf;
    }

    public static void submit(Thread runnable) {
        if (runnable == null)
            return;

        WorkerThread.executor.execute(runnable);
    }

    public static void submitRunnable(Runnable runnable) {
        if (runnable == null)
            return;

        WorkerThread.executor.execute(runnable);
    }

    public static void allOf(CompletableFuture<?>... futures) {
        if (futures == null)
            return;

        CompletableFuture<Void> cf = CompletableFuture.allOf(futures);
        try {
            // WorkerThread.log.info("Completing {} asynchronous tasks",
            // futures.length);
            cf.join();
        } catch (Exception e) {
            WorkerThread.log.error("Failed to complete async tasks {}", e);
        }
    }

    public static void submitAndRun(Runnable... runnables) {
        if (runnables == null)
            return;
        CompletableFuture<?>[] futures = new CompletableFuture[runnables.length];
        for (int i = 0; i < runnables.length; i++) {
            futures[i] = WorkerThread.submit(runnables[i]);
        }
        WorkerThread.allOf(futures);
    }
    
    public static void submitAndRun(List<Runnable> runnables) {
        if (runnables == null)
            return;
        CompletableFuture<?>[] futures = new CompletableFuture[runnables.size()];
        for (int i = 0; i < runnables.size(); i++) {
            futures[i] = WorkerThread.submit(runnables.get(i));
        }
        WorkerThread.allOf(futures);
    }

    /*
     * Submits runnables that execute in an newly forked thread (good for long
     * running tasks)
     */
    public static CompletableFuture<?>[] submitAndForkRun(Runnable... runnables) {
        if (runnables == null)
            return null;
        
        final CompletableFuture<?>[] futures = new CompletableFuture[runnables.length];

        for (int i = 0; i < runnables.length; i++) {
           futures[i] = WorkerThread.submit(runnables[i]);
        }
        return futures;
    }
}
