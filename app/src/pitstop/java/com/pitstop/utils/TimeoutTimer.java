package com.pitstop.utils;

import android.os.CountDownTimer;

/**
 * Created by yifan on 16/12/6.
 */

public abstract class TimeoutTimer extends CountDownTimer {
    private final int RETRIES;
    int currentRetries;

    /**
     * @param seconds The number of seconds in the future from the call
     *                to {@link #start()} until the countdown is done and {@link #onFinish()}
     *                is called.
     * @param retries The number of retries before we trigger timeout message, pass 0 mean no reties
     */
    public TimeoutTimer(final int seconds, final int retries) {
        super(seconds * 1000, seconds * 1000 / 2);
        currentRetries = retries;
        RETRIES = retries;
    }

    @Override
    public void onTick(long millisUntilFinished) {}

    @Override
    public void onFinish() {
        if (currentRetries-- > 0) {
            onRetry();
            this.start();
        } else {
            currentRetries = RETRIES; // refresh number of retries
            onTimeout();
            cancel();
        }
    }

    /**
     * E.g, re-request data
     * After this method call the timer will start itself
     */
    public abstract void onRetry();

    /**
     * Failed after all retries <br>
     * After this method call the timer will get cancel itself
     */
    public abstract void onTimeout();
}
