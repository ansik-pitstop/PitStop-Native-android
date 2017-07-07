package com.pitstop.dependency;

import android.content.Context;

import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.LocalShopAdapter;
import com.pitstop.database.UserAdapter;

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
    public LocalShopAdapter localShopAdapter(Context context){
        return new LocalShopAdapter(context);
    }

    @Singleton
    @Provides
    public LocalCarAdapter localCarAdapter(Context context){
        return new LocalCarAdapter(context);
    }

    @Singleton
    @Provides
    public LocalCarIssueAdapter localCarIssueAdapter(Context context){
        return new LocalCarIssueAdapter(context);
    }

    @Singleton
    @Provides
    public UserAdapter userAdapter(Context context){
        return new UserAdapter(context);
    }

}
