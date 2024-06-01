package com.jrealm.game.util;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@Slf4j
@EqualsAndHashCode(callSuper = false)
public class TimedWorkerThread implements Runnable {
    private boolean shutdown = false;
    private Runnable runnable;
    private double targetFps;

    private long lastUpdateTime;
    private long now;
    private long lastRenderTime;
    private long lastSecondTime;

    private int oldFrameCount;
    private int oldTickCount;
    private int tickCount;

    public TimedWorkerThread(Runnable runnable, double targetFps) {
	this.runnable = runnable;
	this.targetFps = targetFps;
    }

    @SuppressWarnings("unused")
    @Override
    public void run() {

	log.info("TimedThread processing start...");

	double rate = targetFps;

	long lastTime = System.nanoTime();
	double amountOfTicks = rate;
	double ns = 1000000000 / amountOfTicks;
	double delta = 0;
	long timer = System.currentTimeMillis();
	int frames = 0;
	while (!this.shutdown) {
	    long now = System.nanoTime();
	    delta += (now - lastTime) / ns;
	    lastTime = now;
	    while (delta >= 1) {
		WorkerThread.submitAndRun(this.runnable);
		delta--;
	    }
	    frames++;

	    if (System.currentTimeMillis() - timer > 1000) {
		timer += 1000;
		//log.info("Timed worker thread {} frames={}",runnable.);
		frames = 0;
		// updates = 0;
	    }
	}

    }
}
