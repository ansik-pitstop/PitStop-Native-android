package com.pitstop.dependency;

import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.UserAdapter;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/2/2017.
 */

@Module(includes = {NetworkModule.class,LocalStorageModule.class})
public class RepositoryModule {

    @Provides
    @ApplicationScope
    public UserRepository getUserRepository(UserAdapter userAdapter
            , NetworkHelper networkHelper){
        return new UserRepository(userAdapter,networkHelper);
    }

    @Provides
    @ApplicationScope
    public CarRepository getCarRepository(LocalCarAdapter localCarAdapter
            , NetworkHelper networkHelper){
        return new CarRepository(localCarAdapter,networkHelper);
    }

    @Provides
    @ApplicationScope
    public CarIssueRepository getCarIssueRepository(LocalCarIssueAdapter localCarIssueAdapter
            , NetworkHelper networkHelper){
        return new CarIssueRepository(localCarIssueAdapter,networkHelper);
    }
}
