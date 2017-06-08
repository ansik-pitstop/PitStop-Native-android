package com.pitstop.dependency;

import android.os.Handler;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

@Module
public class HandlerModule {

    @Provides
    Handler handler(){
        return new Handler();
    }

}
