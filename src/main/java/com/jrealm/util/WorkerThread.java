package com.jrealm.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkerThread {
    // Fixed pool: cores * 2 for mixed IO/CPU workload, capped to avoid thread explosion
    private static final int THREAD_POOL_COUNT = Math.min(Runtime.getRuntime().availableProcessors() * 2, 16);
    private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
            .newFixedThreadPool(THREAD_POOL_COUNT, Executors.privilegedThreadFactory());

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
    	submitAndForkRun(wrappedTask);		
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
        // Single task: run inline, no pool overhead
        if (runnables.length == 1) {
            runnables[0].run();
            return;
        }
        // Multiple tasks: fan out N-1 to pool, run last one on current thread
        CompletableFuture<?>[] futures = new CompletableFuture[runnables.length - 1];
        for (int i = 0; i < runnables.length - 1; i++) {
            futures[i] = WorkerThread.submit(runnables[i]);
        }
        // Run the last task on the calling thread while others execute in parallel
        runnables[runnables.length - 1].run();
        WorkerThread.allOf(futures);
    }

    public static void submitAndRun(List<Runnable> runnables) {
        if (runnables == null)
            return;
        if (runnables.size() == 1) {
            runnables.get(0).run();
            return;
        }
        CompletableFuture<?>[] futures = new CompletableFuture[runnables.size() - 1];
        for (int i = 0; i < runnables.size() - 1; i++) {
            futures[i] = WorkerThread.submit(runnables.get(i));
        }
        runnables.get(runnables.size() - 1).run();
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
