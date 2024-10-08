package com.pitstop.dependency;

import android.location.Geocoder;

import com.pitstop.database.LocalAppointmentStorage;
import com.pitstop.database.LocalCarIssueStorage;
import com.pitstop.database.LocalCarStorage;
import com.pitstop.database.LocalPendingTripStorage;
import com.pitstop.database.LocalPidStorage;
import com.pitstop.database.LocalSensorDataStorage;
import com.pitstop.database.LocalShopStorage;
import com.pitstop.database.LocalTripStorage;
import com.pitstop.database.LocalUserStorage;
import com.pitstop.repositories.AppointmentRepository;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.PidRepository;
import com.pitstop.repositories.ReportRepository;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.SensorDataRepository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.SnapToRoadRepository;
import com.pitstop.repositories.TripRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.retrofit.GoogleSnapToRoadApi;
import com.pitstop.retrofit.PitstopAppointmentApi;
import com.pitstop.retrofit.PitstopAuthApi;
import com.pitstop.retrofit.PitstopCarApi;
import com.pitstop.retrofit.PitstopSensorDataApi;
import com.pitstop.retrofit.PitstopServiceApi;
import com.pitstop.retrofit.PitstopTripApi;
import com.pitstop.retrofit.PitstopUserApi;
import com.pitstop.utils.NetworkHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Observable;

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
            , PitstopAuthApi pitstopAuthApi, PitstopUserApi pitstopUserApi
            , NetworkHelper networkHelper){
        return new UserRepository(localUserStorage,pitstopUserApi
                ,pitstopAuthApi,networkHelper);
    }

    @Provides
    @Singleton
    public CarRepository getCarRepository(LocalUserStorage localUserStorage, LocalCarStorage localCarStorage
            , LocalShopStorage localShopStorage
            , NetworkHelper networkHelper, PitstopCarApi pitstopCarApi){
        return new CarRepository(localUserStorage, localCarStorage, localShopStorage, networkHelper,pitstopCarApi);
    }

    @Provides
    @Singleton
    public CarIssueRepository getCarIssueRepository(LocalCarIssueStorage localCarIssueStorage
            , PitstopServiceApi pitstopServiceApi, NetworkHelper networkHelper){
        return new CarIssueRepository(localCarIssueStorage, pitstopServiceApi, networkHelper);
    }

    @Provides
    @Singleton
    ScannerRepository getScannerRepository(NetworkHelper networkHelper){
        return new ScannerRepository(networkHelper);
    }

    @Provides
    @Singleton
    PidRepository getPidRepository(LocalPidStorage localPidStorage){
        return new PidRepository(localPidStorage);
    }

    @Provides
    @Singleton
    ReportRepository getReportRepository(NetworkHelper networkHelper){
        return new ReportRepository(networkHelper);
    }

    @Provides
    @Singleton
    AppointmentRepository getAppointmentRepository(PitstopAppointmentApi pitstopAppointmentApi
            , LocalAppointmentStorage localAppointmentStorage){
        return new AppointmentRepository(localAppointmentStorage, pitstopAppointmentApi);
    }

    @Provides
    @Singleton
    TripRepository getTripRepository(PitstopTripApi pitstopTripApi
            , LocalPendingTripStorage localPendingTripStorage, LocalTripStorage localTripStorage
            ,GoogleSnapToRoadApi googleSnapToRoadApi, Geocoder geocoder
            , Observable<Boolean> connectionObservable){
        return new TripRepository(pitstopTripApi, localPendingTripStorage
                , localTripStorage, googleSnapToRoadApi, geocoder, connectionObservable);
    }

    @Provides
    @Singleton
    SensorDataRepository getSensorDataRepository(LocalSensorDataStorage localSensorDataStorage
            , PitstopSensorDataApi sensorDataApi, Observable<Boolean> connectionObservable){
        return new SensorDataRepository(localSensorDataStorage,sensorDataApi, connectionObservable);
    }

    @Provides
    @Singleton
    SnapToRoadRepository getSnapToRoadRepository(GoogleSnapToRoadApi googleSnapToRoadApi){
        return new SnapToRoadRepository(googleSnapToRoadApi);
    }
}
