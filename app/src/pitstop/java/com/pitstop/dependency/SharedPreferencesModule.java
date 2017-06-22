package com.pitstop.dependency;

import android.content.Context;
import android.content.SharedPreferences;

import com.pitstop.utils.PreferenceKeys;

import dagger.Module;
import dagger.Provides;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Karol Zdebel on 6/22/2017.
 */

@Module (includes = ContextModule.class)
public class SharedPreferencesModule {

    @Provides
    SharedPreferences sharedPreferences(Context context){
        return context.getSharedPreferences(PreferenceKeys.NAME_CREDENTIALS, MODE_PRIVATE);
    }
}
