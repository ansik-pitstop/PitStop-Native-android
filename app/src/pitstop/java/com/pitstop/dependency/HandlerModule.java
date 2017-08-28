package com.pitstop.dependency;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

@Module
public class HandlerModule {

    final String THREAD_NAME = "use_case_thread";

    @Singleton
    @Provides
    @Named("useCaseHandler")
    Handler useCaseHandler(){
        HandlerThread handlerThread = new HandlerThread(THREAD_NAME);
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        return new Handler(looper);
    }

    @Singleton
    @Provides
    @Named("mainHandler")
    Handler mainHandler(){
        return new Handler(Looper.getMainLooper());
    }

}
