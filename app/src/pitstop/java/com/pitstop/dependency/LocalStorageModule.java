package com.pitstop.dependency;

import android.content.Context;

import com.pitstop.database.LocalActivityStorage;
import com.pitstop.database.LocalAlarmStorage;
import com.pitstop.database.LocalAppointmentStorage;
import com.pitstop.database.LocalCarIssueStorage;
import com.pitstop.database.LocalCarStorage;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.database.LocalFuelConsumptionStorage;
import com.pitstop.database.LocalLocationStorage;
import com.pitstop.database.LocalPendingTripStorage;
import com.pitstop.database.LocalPidStorage;
import com.pitstop.database.LocalSensorDataStorage;
import com.pitstop.database.LocalShopStorage;
import com.pitstop.database.LocalSpecsStorage;
import com.pitstop.database.LocalTripStorage;
import com.pitstop.database.LocalUserStorage;

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
    public LocalShopStorage localShopStorage(LocalDatabaseHelper localDatabaseHelper) {
        return new LocalShopStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    public LocalCarStorage localCarStorage(LocalDatabaseHelper localDatabaseHelper) {
        return new LocalCarStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    public LocalCarIssueStorage localCarIssueStorage(LocalDatabaseHelper localDatabaseHelper) {
        return new LocalCarIssueStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    public LocalUserStorage userStorage(LocalDatabaseHelper localDatabaseHelper) {
        return new LocalUserStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    LocalPidStorage localPidStorage(LocalDatabaseHelper localDatabaseHelper) {
        return new LocalPidStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    LocalSpecsStorage localSpecsStorage(LocalDatabaseHelper localDatabaseHelper) {
        return new LocalSpecsStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    LocalAlarmStorage localAlarmStorage(LocalDatabaseHelper localDatabaseHelper) {
        return new LocalAlarmStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    LocalFuelConsumptionStorage localFuelConsumptionStorage(LocalDatabaseHelper localDatabaseHelper) {
        return new LocalFuelConsumptionStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    LocalAppointmentStorage localAppointmentStorage(LocalDatabaseHelper localDatabaseHelper) {
        return new LocalAppointmentStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    LocalTripStorage localTripStorage(LocalDatabaseHelper localDatabaseHelper){
        return  new LocalTripStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    LocalPendingTripStorage localPendingTripStorage(LocalDatabaseHelper localDatabaseHelper){
        return  new LocalPendingTripStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    public LocalSensorDataStorage localSensorDataStorage(LocalDatabaseHelper localDatabaseHelper) {
        return new LocalSensorDataStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    public LocalLocationStorage localLocationStorage(LocalDatabaseHelper localDatabaseHelper){
        return new LocalLocationStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    public LocalActivityStorage localActivityStorage(LocalDatabaseHelper localDatabaseHelper){
        return new LocalActivityStorage(localDatabaseHelper);
    }

    @Singleton
    @Provides
    public LocalDatabaseHelper localDatabaseHelper(Context context){
        return LocalDatabaseHelper.getInstance(context);
    }
}
