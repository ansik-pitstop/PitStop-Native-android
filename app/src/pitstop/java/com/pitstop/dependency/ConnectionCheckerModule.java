package com.pitstop.dependency;

import com.pitstop.utils.ConnectionChecker;
import com.pitstop.utils.NetworkHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 8/18/2017.
 */
@Module(includes = {NetworkModule.class})
public class ConnectionCheckerModule {
    @Singleton
    @Provides
    public ConnectionChecker connectionChecker(NetworkHelper networkHelper){
        return new ConnectionChecker(networkHelper);
    }

}
