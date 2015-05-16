package org.dfhu.clicknrecord.tests;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dfhu.clicknrecord.SelfStoppingIntervalRunnable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SelfStoppingIntervalRunnerTest extends TestCase {

    public SelfStoppingIntervalRunnerTest() {
        super();
    }

    @SmallTest
    public void testStoppingIntervalRunnable() throws Exception {

        class Foop implements Runnable {
            public AtomicInteger count = new AtomicInteger(0);

            @Override
            public void run() {
                count.incrementAndGet();
            }
        }

        Foop r = new Foop();

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        SelfStoppingIntervalRunnable ssir = new SelfStoppingIntervalRunnable(r);

        ssir.scheduleAtFixedRate(executor, 0, 1, TimeUnit.SECONDS);

        Thread.sleep(3000);
        ssir.setShouldStop(true);
        Thread.sleep(1000);

        int result = r.count.get();
        assertTrue(result >= 3);

        Thread.sleep(3000);
        assertEquals(result, r.count.get());

    }
}
