package com.pitstop.dependency;

import android.content.Context;

import com.pitstop.database.LocalAlarmStorage;
import com.pitstop.database.LocalAppointmentStorage;
import com.pitstop.database.LocalCarIssueStorage;
import com.pitstop.database.LocalCarStorage;
import com.pitstop.database.LocalDeviceTripStorage;
import com.pitstop.database.LocalFuelConsumptionStorage;
import com.pitstop.database.LocalPidStorage;
import com.pitstop.database.LocalScannerStorage;
import com.pitstop.database.LocalShopStorage;
import com.pitstop.database.LocalSpecsStorage;
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
    public LocalShopStorage localShopAdapter(Context context){
        return new LocalShopStorage(context);
    }

    @Singleton
    @Provides
    public LocalCarStorage localCarAdapter(Context context){
        return new LocalCarStorage(context);
    }

    @Singleton
    @Provides
    public LocalCarIssueStorage localCarIssueAdapter(Context context){
        return new LocalCarIssueStorage(context);
    }

    @Singleton
    @Provides
    public LocalUserStorage userAdapter(Context context){
        return new LocalUserStorage(context);
    }

    @Singleton
    @Provides
    LocalScannerStorage localScannerAdapter(Context context){
        return new LocalScannerStorage(context);
    }

    @Singleton
    @Provides
    LocalPidStorage localPidStorage(Context context){
        return new LocalPidStorage(context);
    }

    @Singleton
    @Provides
    LocalDeviceTripStorage localDeviceTripStorage(Context context){
        return new LocalDeviceTripStorage(context);
    }

    @Singleton
    @Provides
    LocalSpecsStorage localSpecsStorage(Context context){
        return new LocalSpecsStorage(context);
    }

    @Singleton
    @Provides
    LocalAlarmStorage localAlarmStorage(Context context){return  new LocalAlarmStorage(context);}

    @Singleton
    @Provides
    LocalFuelConsumptionStorage localFuelConsumptionStorage(Context context){return  new LocalFuelConsumptionStorage(context);}

    @Singleton
    @Provides
    LocalAppointmentStorage localAppointmentStorage(Context context){return  new LocalAppointmentStorage(context);}


}
