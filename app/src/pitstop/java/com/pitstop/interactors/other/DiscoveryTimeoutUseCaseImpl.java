package com.pitstop.interactors.other;

import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;

import com.pitstop.models.DebugMessage;
import com.pitstop.utils.Logger;

/**
 * Created by Karol Zdebel on 9/14/2017.
 */

public class DiscoveryTimeoutUseCaseImpl implements DiscoveryTimeoutUseCase {

    private final String TAG = getClass().getSimpleName();
    final int TIMEOUT_LEN = 20000;
    final int TICK_LEN = 1000;

    private Handler useCaseHandler;
    private Handler mainHandler;
    private Callback callback;
    private int discoveryNum;

    public DiscoveryTimeoutUseCaseImpl(Handler useCaseHandler, Handler mainHandler) {
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(int discoveryNum, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case started execution: discovery num="+discoveryNum
                ,false, DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        this.discoveryNum = discoveryNum;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {
        Log.d(TAG,"run()");
        new CountDownTimer(TIMEOUT_LEN, TICK_LEN) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                Logger.getInstance().logI(TAG,"Use case finished"
                        ,false, DebugMessage.TYPE_USE_CASE);
                mainHandler.post(() -> callback.onFinish(discoveryNum));
            }
        }.start();
    }
}
