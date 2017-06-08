package com.pitstop.dependency;

import android.content.Context;

import com.pitstop.utils.NetworkHelper;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/2/2017.
 */

@Module(includes = ContextModule.class)
public class NetworkModule {

    @Provides
    public NetworkHelper networkHelper(Context context){
        return new NetworkHelper(context);
    }
}
