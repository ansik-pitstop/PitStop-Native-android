package com.pitstop.dependency;

import android.os.Handler;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

@Module
public class HandlerModule {

    @Singleton
    @Provides
    @Singleton
    Handler handler(){
        return new Handler();
    }

}
