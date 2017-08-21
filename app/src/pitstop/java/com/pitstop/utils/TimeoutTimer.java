package com.pitstop.utils;

import android.os.CountDownTimer;

/**
 * Created by yifan on 16/12/6.
 */

public abstract class TimeoutTimer extends CountDownTimer {

    private final int totalRetries;
    private int retriesLeft;

    /**
     * @param seconds The number of seconds in the future from the call
     *                to {@link #start()} until the countdown is done and {@link #onFinish()}
     *                is called.
     * @param retries The number of totalRetries before we trigger timeout message, pass 0 mean no reties
     */
    public TimeoutTimer(final int seconds, final int retries) {
        super(seconds * 1000, 500);
        retriesLeft = retries;
        this.totalRetries = retries;
    }

    @Override
    public void onFinish() {
        if (retriesLeft-- > 0) {
            onRetry();
            this.start();
        } else {
            retriesLeft = totalRetries; // refresh number of totalRetries
            onTimeout();
            cancel();
        }
    }

    @Override
    public void onTick(long time){
    }

    /**
     * E.g, re-request data
     * After this method call the timer will start itself
     */
    public abstract void onRetry();

    /**
     * Failed after all totalRetries <br>
     * After this method call the timer will get cancel itself
     */
    public abstract void onTimeout();
}
