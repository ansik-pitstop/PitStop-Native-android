package com.pitstop.dependency;

import android.content.Context;
import android.content.SharedPreferences;

import com.pitstop.utils.NetworkHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/2/2017.
 */

@Module (includes = {ContextModule.class, SharedPreferencesModule.class})
public class NetworkModule {

    @Singleton
    @Provides
    public NetworkHelper networkHelper(Context context, SharedPreferences sharedPreferences){
        return new NetworkHelper(context, sharedPreferences);
    }
}
