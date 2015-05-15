package org.dfhu.clicknrecord;

import android.app.Application;
import android.test.ApplicationTestCase;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void testSelfStoppingIntervalRunnable () {

        Runnable r = new Runnable() {
            public AtomicInteger count = new AtomicInteger(0);

            @Override
            public void run() {
                count.incrementAndGet();
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        SelfStoppingIntervalRunnable ssir = new SelfStoppingIntervalRunnable(r);

        ssir.scheduleAtFixedRate(executor, 0, 1, TimeUnit.SECONDS);

        ssir.setShouldStop(true);

    }
}