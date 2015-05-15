package org.dfhu.clicknrecord;


import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SelfStoppingIntervalRunnable implements Runnable {
    private final Runnable delegated;
    private volatile ScheduledFuture<?> todo;
    public AtomicBoolean shouldStop = new AtomicBoolean(false);

    public SelfStoppingIntervalRunnable (Runnable r) {
        delegated = r;
    }

    @Override
    public void run() {
        if (shouldStop()) {
            todo.cancel(true);
        }
        delegated.run();
    }

    public void setShouldStop(boolean shouldStop) {
        this.shouldStop.set(shouldStop);
    }

    private boolean shouldStop() {
        return shouldStop.get();
    }

    public void scheduleAtFixedRate(
            ScheduledExecutorService executor, long delay, long period, TimeUnit unit) {
        todo = executor.scheduleAtFixedRate(this, delay, period, unit);
    }

}
