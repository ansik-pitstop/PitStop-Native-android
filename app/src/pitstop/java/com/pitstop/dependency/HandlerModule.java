package com.pitstop.dependency;

import android.os.Handler;
import android.os.Looper;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

@Module
public class HandlerModule {

    @Provides
    Handler handler(){
        //Handler cannot be instantiated without an associated Looper
        if (Looper.myLooper() == null){
            Looper.prepare();
            Looper.loop();
        }

        return new Handler(Looper.myLooper());
    }

}
