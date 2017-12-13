package com.pitstop.dependency;

import com.pitstop.interactors.add.AddAlarmUseCase;
import com.pitstop.interactors.add.AddCarUseCase;
import com.pitstop.interactors.add.AddCustomServiceUseCase;
import com.pitstop.interactors.add.AddDtcUseCase;
import com.pitstop.interactors.add.AddLicensePlateUseCase;
import com.pitstop.interactors.add.AddScannerUseCase;
import com.pitstop.interactors.add.AddServicesUseCase;
import com.pitstop.interactors.add.AddShopUseCase;
import com.pitstop.interactors.add.GenerateReportUseCase;
import com.pitstop.interactors.check.CheckAlarmsEnabledUse;
import com.pitstop.interactors.check.CheckFirstCarAddedUseCase;
import com.pitstop.interactors.check.CheckNetworkConnectionUseCase;
import com.pitstop.interactors.emissions.Post2141UseCase;
import com.pitstop.interactors.get.GetAlarmCountUseCase;
import com.pitstop.interactors.get.GetAlarmsUseCase;
import com.pitstop.interactors.get.GetCarByCarIdUseCase;
import com.pitstop.interactors.get.GetCarByVinUseCase;
import com.pitstop.interactors.get.GetCarImagesArrayUseCase;
import com.pitstop.interactors.get.GetCarStyleIDUseCase;
import com.pitstop.interactors.get.GetCarsByUserIdUseCase;
import com.pitstop.interactors.get.GetCarsWithDealershipsUseCase;
import com.pitstop.interactors.get.GetCurrentCarDealershipUseCase;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.get.GetCurrentUserUseCase;
import com.pitstop.interactors.get.GetDTCUseCase;
import com.pitstop.interactors.get.GetDealershipWithCarIssuesUseCase;
import com.pitstop.interactors.get.GetDoneServicesUseCase;
import com.pitstop.interactors.get.GetFuelConsumedAndPriceUseCase;
import com.pitstop.interactors.get.GetFuelConsumedUseCase;
import com.pitstop.interactors.get.GetFuelPricesUseCase;
import com.pitstop.interactors.get.GetGooglePlacesShopsUseCase;
import com.pitstop.interactors.get.GetLicensePlateUseCase;
import com.pitstop.interactors.get.GetPIDUseCase;
import com.pitstop.interactors.get.GetPitstopShopsUseCase;
import com.pitstop.interactors.get.GetPlaceDetailsUseCase;
import com.pitstop.interactors.get.GetPrevIgnitionTimeUseCase;
import com.pitstop.interactors.get.GetReportsUseCase;
import com.pitstop.interactors.get.GetShopHoursUseCase;
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCase;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.get.GetUserNotificationUseCase;
import com.pitstop.interactors.other.DeviceClockSyncUseCase;
import com.pitstop.interactors.other.DiscoveryTimeoutUseCase;
import com.pitstop.interactors.other.HandlePidDataUseCase;
import com.pitstop.interactors.other.HandleVinOnConnectUseCase;
import com.pitstop.interactors.other.MarkServiceDoneUseCase;
import com.pitstop.interactors.other.PeriodicCachedTripSendUseCase;
import com.pitstop.interactors.other.RequestServiceUseCase;
import com.pitstop.interactors.other.SortReportsUseCase;
import com.pitstop.interactors.other.StoreFuelConsumedUseCase;
import com.pitstop.interactors.other.Trip215EndUseCase;
import com.pitstop.interactors.other.Trip215StartUseCase;
import com.pitstop.interactors.remove.RemoveCarUseCase;
import com.pitstop.interactors.remove.RemoveShopUseCase;
import com.pitstop.interactors.set.SetAlarmsEnabledUseCase;
import com.pitstop.interactors.set.SetFirstCarAddedUseCase;
import com.pitstop.interactors.set.SetNotificationReadUseCase;
import com.pitstop.interactors.set.SetServicesDoneUseCase;
import com.pitstop.interactors.set.SetUserCarUseCase;
import com.pitstop.interactors.update.UpdateCarDealershipUseCase;
import com.pitstop.interactors.update.UpdateCarMileageUseCase;
import com.pitstop.interactors.update.UpdateShopUseCase;
import com.pitstop.interactors.update.UpdateUserNameUseCase;
import com.pitstop.interactors.update.UpdateUserPhoneUseCase;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Use this for the retrieval of UseCase instances, each method call results in a
 * NEW instance of that use case.
 *
 * Created by Karol Zdebel on 6/5/2017.
 */

@Singleton
@Component(modules = UseCaseModule.class)
public interface UseCaseComponent {

    Post2141UseCase getPost2141UseCase();

    GetPIDUseCase getGetPIDUseCase();

    GetDTCUseCase getGetDTCUseCase();

    AddCustomServiceUseCase getAddCustomServiceUseCase();

    GetShopHoursUseCase getGetShopHoursUseCase();

    RequestServiceUseCase getRequestServiceUseCase();

    AddServicesUseCase getAddServicesUseCase();

    RemoveShopUseCase getRemoveShopUseCase();

    UpdateUserPhoneUseCase getUpdateUserPhoneUseCase();

    UpdateUserNameUseCase getUpdateUserNameUseCase();

    GetCurrentUserUseCase getGetCurrentUserUseCase();

    GetCarByCarIdUseCase getGetCarByCarIdUseCase();

    GetPlaceDetailsUseCase getGetPlaceDetailsUseCase();

    GetGooglePlacesShopsUseCase getGetGooglePlacesShopsUseCase();

    UpdateShopUseCase getUpdateShopUseCase();

    AddShopUseCase getAddShopUseCase();

    UpdateCarDealershipUseCase getUpdateCarDealershipUseCase();

    GetPitstopShopsUseCase getGetPitstopShopsUseCase();

    GetCarsByUserIdUseCase getCarsByUserIdUseCase();

    CheckFirstCarAddedUseCase checkFirstCarAddedUseCase();

    AddCarUseCase addCarUseCase();

    GetCurrentServicesUseCase getCurrentServicesUseCase();

    GetDoneServicesUseCase getDoneServicesUseCase();

    GetUpcomingServicesMapUseCase getUpcomingServicesUseCase();

    GetUserCarUseCase getUserCarUseCase();

    MarkServiceDoneUseCase markServiceDoneUseCase();

    RemoveCarUseCase removeCarUseCase();

    SetUserCarUseCase setUserCarUseCase();

    SetFirstCarAddedUseCase setFirstCarAddedUseCase();

    HandleVinOnConnectUseCase handleVinOnConnectUseCase();

    Trip215StartUseCase trip215StartUseCase();

    Trip215EndUseCase trip215EndUseCase();

    GetPrevIgnitionTimeUseCase getPrevIgnitionTimeUseCase();

    HandlePidDataUseCase handlePidDataUseCase();

    PeriodicCachedTripSendUseCase periodicCachedTripSendUseCase();

    GetUserNotificationUseCase getUserNotificationUseCase();

    UpdateCarMileageUseCase updateCarMileageUseCase();

    DiscoveryTimeoutUseCase discoveryTimeoutUseCase();

    GenerateReportUseCase getGenerateReportUseCase();

    GetReportsUseCase getGetVehicleHealthReportsUseCase();

    SortReportsUseCase getSortReportsUseCase();

    GetCarByVinUseCase getGetCarByVinUseCase();

    AddLicensePlateUseCase addLicensePlateUseCase();

    GetLicensePlateUseCase getLicensePlateUseCase();

    GetCarStyleIDUseCase getCarStyleIDUseCase();

    GetCarImagesArrayUseCase getCarImagesArrayUseCase();

    AddDtcUseCase addDtcUseCase();

    GetCarsWithDealershipsUseCase getCarsWithDealershipsUseCase();

    CheckNetworkConnectionUseCase getCheckNetworkConnectionUseCase();

    SetServicesDoneUseCase getSetServicesDoneUseCase();

    SetNotificationReadUseCase getSetNotificationReadUseCase();

    GetDealershipWithCarIssuesUseCase getDealershipWithCarIssuesUseCase();

    GetCurrentCarDealershipUseCase getGetCurrentDealershipUseCase();

    DeviceClockSyncUseCase getDeviceClockSyncUseCase();

    AddAlarmUseCase addAlarmUseCase();

    GetAlarmsUseCase getAlarmsUseCase();

    SetAlarmsEnabledUseCase getSetAlarmsEnableduseCase();

    CheckAlarmsEnabledUse getCheckAlarmsEnabledUseCase();

    GetAlarmCountUseCase getGetAlarmCountUseCase();

    StoreFuelConsumedUseCase getStoreFuelConsumedUseCase();

    GetFuelConsumedUseCase getGetFuelConsumedUseCase();

    GetFuelPricesUseCase getFuelPriceUseCase();

    GetFuelConsumedAndPriceUseCase getGetFuelConsumedAndPriceUseCase();

    AddScannerUseCase getAddScannerUseCase();

}
