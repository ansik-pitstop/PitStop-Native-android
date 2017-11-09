package com.pitstop.dependency;

import com.pitstop.database.LocalCarStorage;
import com.pitstop.database.LocalCarIssueStorage;
import com.pitstop.database.LocalDeviceTripStorage;
import com.pitstop.database.LocalPidStorage;
import com.pitstop.database.LocalScannerStorage;
import com.pitstop.database.LocalShopStorage;
import com.pitstop.database.LocalUserStorage;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.PidRepository;
import com.pitstop.repositories.ReportRepository;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.retrofit.PitstopCarApi;
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
    public ShopRepository getShopRepository(LocalShopStorage localShopStorage
            , NetworkHelper networkHelper){
        return new ShopRepository(localShopStorage,networkHelper);
    }

    @Provides
    @Singleton
    public UserRepository getUserRepository(LocalUserStorage localUserStorage
            , NetworkHelper networkHelper){
        return new UserRepository(localUserStorage,networkHelper);
    }

    @Provides
    @Singleton
    public CarRepository getCarRepository(LocalCarStorage localCarStorage
            , NetworkHelper networkHelper, PitstopCarApi pitstopCarApi){
        return new CarRepository(localCarStorage,networkHelper,pitstopCarApi);
    }

    @Provides
    @Singleton
    public CarIssueRepository getCarIssueRepository(LocalCarIssueStorage localCarIssueStorage
            , NetworkHelper networkHelper){
        return new CarIssueRepository(localCarIssueStorage,networkHelper);
    }

    @Provides
    @Singleton
    ScannerRepository getScannerRepository(NetworkHelper networkHelper
            , LocalScannerStorage localScannerStorage){

        return new ScannerRepository(networkHelper, localScannerStorage);
    }

    @Provides
    @Singleton
    Device215TripRepository getDevice215TripRepository(NetworkHelper networkHelper
            , LocalDeviceTripStorage localDeviceTripStorage){

        return new Device215TripRepository(networkHelper,localDeviceTripStorage);
    }

    @Provides
    @Singleton
    PidRepository getPidRepository(NetworkHelper networkHelper, LocalPidStorage localPidStorage){
        return new PidRepository(networkHelper,localPidStorage);
    }

    @Provides
    @Singleton
    ReportRepository getReportRepository(NetworkHelper networkHelper){
        return new ReportRepository(networkHelper);
    }
}
