package com.pitstop.dependency;

import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.LocalShopAdapter;
import com.pitstop.database.UserAdapter;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/2/2017.
 */

@Module(includes = {NetworkModule.class,LocalStorageModule.class})
public class RepositoryModule {

    @Provides
    @Singleton
    public ShopRepository getShopRepository(LocalShopAdapter localShopAdapter
            , NetworkHelper networkHelper){
        return new ShopRepository(localShopAdapter,networkHelper);
    }

    @Provides
    @Singleton
    public UserRepository getUserRepository(UserAdapter userAdapter
            , NetworkHelper networkHelper){
        return new UserRepository(userAdapter,networkHelper);
    }

    @Provides
    @Singleton
    public CarRepository getCarRepository(LocalCarAdapter localCarAdapter
            , NetworkHelper networkHelper){
        return new CarRepository(localCarAdapter,networkHelper);
    }

    @Provides
    @Singleton
    public CarIssueRepository getCarIssueRepository(LocalCarIssueAdapter localCarIssueAdapter
            , NetworkHelper networkHelper){
        return new CarIssueRepository(localCarIssueAdapter,networkHelper);
    }
}
