package com.pitstop.dependency;

import android.content.Context;

import com.pitstop.database.LocalCarHelper;
import com.pitstop.database.LocalCarIssueHelper;
import com.pitstop.database.LocalShopHelper;
import com.pitstop.database.UserHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/2/2017.
 */

@Module(includes = ContextModule.class)
public class LocalStorageModule {

    @Singleton
    @Provides
    public LocalShopHelper localShopAdapter(Context context){
        return new LocalShopHelper(context);
    }

    @Singleton
    @Provides
    public LocalCarHelper localCarAdapter(Context context){
        return new LocalCarHelper(context);
    }

    @Singleton
    @Provides
    public LocalCarIssueHelper localCarIssueAdapter(Context context){
        return new LocalCarIssueHelper(context);
    }

    @Singleton
    @Provides
    public UserHelper userAdapter(Context context){
        return new UserHelper(context);
    }

}
