package org.dfhu.clicknrecord;


public interface ISelfStoppingRunnable extends Runnable {
    void setInterval(SelfStoppingIntervalRunnable ir);
}
