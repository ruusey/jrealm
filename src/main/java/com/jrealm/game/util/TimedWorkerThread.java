package com.jrealm.game.util;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Data
@NoArgsConstructor
@Slf4j
@EqualsAndHashCode(callSuper=false)
public class TimedWorkerThread extends Thread {
	private boolean shutdown = false;
	private Runnable runnable;
	private int targetFps;
	
	
	private long lastUpdateTime;
	private long now;
	private long lastRenderTime;
	private long lastSecondTime;
	
	private int oldFrameCount;
	private int oldTickCount;
	private int tickCount;
	
	public TimedWorkerThread(Runnable runnable, int targetFps) {
		this.runnable = runnable;
		this.targetFps = targetFps;
	}
	
	@Override
	public void run() {
		final double GAME_HERTZ = this.targetFps;
		final double TBU = 1000000000 / GAME_HERTZ; // Time Before Update

		final int MUBR = 1; // Must Update before render

		this.lastUpdateTime = System.nanoTime();

		//TODO: remove and replace with an actual value: 20
		final double TARGET_FPS = this.targetFps;
		final double TTBR = 1000000000 / TARGET_FPS; // Total time before render

		int frameCount = 0;
		this.lastSecondTime = (long) (this.lastUpdateTime / 1000000000);
		this.oldFrameCount = 0;

		this.tickCount = 0;
		this.oldTickCount = 0;
		log.info("TimedThread processing start...");

		while (!this.shutdown) {
			this.now = System.nanoTime();
			int updateCount = 0;
			while (((this.now - this.lastUpdateTime) > TBU) && (updateCount < MUBR)) {
				WorkerThread.submitAndRun(runnable);
				this.lastUpdateTime += TBU;
				updateCount++;
				this.tickCount++;
//				log.info("TimedThread updateCount = {}", updateCount);
//				log.info("TimedThread tickCount = {}", this.tickCount);
			}

			if ((this.now - this.lastUpdateTime) > TBU) {
				this.lastUpdateTime = (long) (this.now - TBU);
			}
			
			int thisSecond = (int) (this.lastUpdateTime / 1000000000);
			if (thisSecond > this.lastSecondTime) {
				if (frameCount != this.oldFrameCount) {
					// System.out.println("NEW SECOND " + thisSecond + " " + frameCount);
					this.oldFrameCount = frameCount;
				}

				if (this.tickCount != this.oldTickCount) {
					this.oldTickCount = this.tickCount;
				}
				this.tickCount = 0;
				frameCount = 0;
				this.lastSecondTime = thisSecond;
			}

			while (((this.now - this.lastRenderTime) < TTBR) && ((this.now - this.lastUpdateTime) < TBU)) {
				try {
				} catch (Exception e) {
					System.out.println("ERROR: yielding thread");
				}
				this.now = System.nanoTime();
			}
		}
	}
}
