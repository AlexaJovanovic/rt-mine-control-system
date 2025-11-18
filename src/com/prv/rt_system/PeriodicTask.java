package com.prv.rt_system;

// Periodic task abstraction with drift correction and priorities
public class PeriodicTask extends Thread {

    private final long periodNs;
    private final Runnable task;
    private volatile boolean running = true;
    private long maxExecTime = 0;

    public PeriodicTask(long periodMs, Runnable task, int priority) {
        this.periodNs = periodMs * 1_000_000L; // convert ms to ns
        this.task = task;

        // Clamp priority to valid Java range [Thread.MIN_PRIORITY, Thread.MAX_PRIORITY]
        if (priority < Thread.MIN_PRIORITY) {
            priority = Thread.MIN_PRIORITY;
        } else if (priority > Thread.MAX_PRIORITY) {
            priority = Thread.MAX_PRIORITY;
        }
        setPriority(priority); 
    }

    public double getMaxExecTimeMs() {
        return maxExecTime / 1_000_000.0;
    }

    @Override
    public void run() {
        long nextRelease = System.nanoTime();

        while (running) {
            // 1. Execute task
            long start = System.nanoTime();
            task.run();
            long duration = System.nanoTime() - start;
            if (duration > maxExecTime) {
                maxExecTime = duration;
            }

            // 2. Compute next release time
            nextRelease += periodNs;

            // 3. Sleep until next release
            long sleepTime = nextRelease - System.nanoTime();
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime / 1_000_000, (int)(sleepTime % 1_000_000));
                } catch (InterruptedException e) {
                    break;
                }
            } else {
                // deadline miss, reset periodic activation
                nextRelease = System.nanoTime();
            }
        }
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }
}
