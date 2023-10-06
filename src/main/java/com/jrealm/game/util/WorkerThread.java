package com.jrealm.game.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkerThread extends Thread {
	private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(25,
			Executors.privilegedThreadFactory());

	public static CompletableFuture<?> submit(Runnable runnable) {
		return CompletableFuture.runAsync(runnable, WorkerThread.executor);
	}

	public static void submit(Thread runnable) {
		WorkerThread.executor.execute(runnable);
	}

	public static void allOf(CompletableFuture<?>... futures) {

		CompletableFuture<Void> cf = CompletableFuture.allOf(futures);
		try {
			// WorkerThread.log.info("Completing {} asynchronous tasks",
			// cf.getNumberOfDependents());
			cf.get();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			WorkerThread.log.error("Failed to complete async tasks {}", e);
		}
	}

	public static void submitAndRun(Runnable... runnables) {
		CompletableFuture<?>[] futures = new CompletableFuture[runnables.length];
		for (int i = 0; i < runnables.length; i++) {
			futures[i] = WorkerThread.submit(runnables[i]);
		}

		WorkerThread.allOf(futures);
	}
}
