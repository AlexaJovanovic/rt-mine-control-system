package com.prv.rt_system;


// Periodic task abstraction with drift correction 
public class PeriodicTask extends Thread {
    public PeriodicTask(long periodMs, Runnable task) {
        this.periodNs = periodMs * 1_000_000L; // convert ms to ns
        this.task = task;
    }

    @Override
    public void run() {
        long nextRelease = System.nanoTime();

        while (running) {
            // 1. Execute task
            task.run();

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
                System.out.println("Deadline miss!");
                nextRelease = System.nanoTime(); // reset periodic activation
            }
        }
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }
    
    private final long periodNs;
    private final Runnable task;
    private volatile boolean running = true;
}
