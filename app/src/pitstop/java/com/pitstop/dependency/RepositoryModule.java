package com.pitstop.dependency;

import com.pitstop.database.LocalCarHelper;
import com.pitstop.database.LocalCarIssueHelper;
import com.pitstop.database.LocalShopHelper;
import com.pitstop.database.UserHelper;
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
    public ShopRepository getShopRepository(LocalShopHelper localShopHelper
            , NetworkHelper networkHelper){
        return new ShopRepository(localShopHelper,networkHelper);
    }

    @Provides
    @Singleton
    public UserRepository getUserRepository(UserHelper userHelper
            , NetworkHelper networkHelper){
        return new UserRepository(userHelper,networkHelper);
    }

    @Provides
    @Singleton
    public CarRepository getCarRepository(LocalCarHelper localCarHelper
            , NetworkHelper networkHelper){
        return new CarRepository(localCarHelper,networkHelper);
    }

    @Provides
    @Singleton
    public CarIssueRepository getCarIssueRepository(LocalCarIssueHelper localCarIssueHelper
            , NetworkHelper networkHelper){
        return new CarIssueRepository(localCarIssueHelper,networkHelper);
    }
}
