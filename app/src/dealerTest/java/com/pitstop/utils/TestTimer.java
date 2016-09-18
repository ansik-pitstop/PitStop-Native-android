package com.pitstop.utils;

import android.os.CountDownTimer;

/**
 * Created by Ben Wu on 2016-09-18.
 */
public abstract class TestTimer extends CountDownTimer {

    public TestTimer(int millisInFuture) {
        super(millisInFuture, millisInFuture);
    }

    @Override
    public void onTick(long millisUntilFinished) {

    }
}
