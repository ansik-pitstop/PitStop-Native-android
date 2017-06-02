package com.pitstop.dependency;

import android.content.Context;

import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.UserAdapter;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/2/2017.
 */

@Module(includes = ContextModule.class)
public class LocalStorageModule {

    @ApplicationScope
    @Provides
    public LocalCarAdapter localCarAdapter(Context context){
        return new LocalCarAdapter(context);
    }

    @ApplicationScope
    @Provides
    public LocalCarIssueAdapter localCarIssueAdapter(Context context){
        return new LocalCarIssueAdapter(context);
    }

    @ApplicationScope
    @Provides
    public UserAdapter userAdapter(Context context){
        return new UserAdapter(context);
    }

}
