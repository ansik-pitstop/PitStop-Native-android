package com.pitstop.dependency;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/2/2017.
 */

@Module
public class ContextModule {

    private final Context context;

    public ContextModule(Context context){
        this.context = context;
    }

    @Singleton
    @Provides
    public Context context(){
        return context;
    }
}
