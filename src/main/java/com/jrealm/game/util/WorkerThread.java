package com.jrealm.game.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerThread extends Thread {

	private static final ExecutorService executor = Executors.newFixedThreadPool(10);

	public static CompletableFuture<?> submit(Runnable runnable) {
		return CompletableFuture.runAsync(runnable, WorkerThread.executor);
	}

	public static void allOf(CompletableFuture<?>... futures) {

		CompletableFuture<Void> cf = CompletableFuture.allOf(futures);
		try {
			cf.get();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
