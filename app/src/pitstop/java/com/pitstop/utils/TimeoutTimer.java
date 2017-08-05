package com.pitstop.utils;

import android.os.CountDownTimer;

/**
 * Created by yifan on 16/12/6.
 */

public abstract class TimeoutTimer extends CountDownTimer {

    private final String TAG = getClass().getSimpleName();

    private final int totalRetries;
    private int retriesLeft;
    private int retryTime;
    private int progress = 0;

    /**
     * @param seconds The number of seconds in the future from the call
     *                to {@link #start()} until the countdown is done and {@link #onFinish()}
     *                is called.
     * @param retries The number of totalRetries before we trigger timeout message, pass 0 mean no reties
     */
    public TimeoutTimer(final int seconds, final int retries) {
        super(seconds * 1000, seconds * 1000 / 2);
        retriesLeft = retries;
        this.totalRetries = retries;
        this.retryTime = seconds*1000;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        long otherRetriesTime = retriesLeft*retryTime;
        long totalTimeLeft = otherRetriesTime+millisUntilFinished;
        long totalTime = totalRetries*retryTime;
        progress = (int)(totalTimeLeft/totalTime);
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
            progress = 0;
        }
    }

    public int getProgress(){
        return progress;
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
