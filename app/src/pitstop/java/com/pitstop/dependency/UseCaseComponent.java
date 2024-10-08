package com.pitstop.dependency;

import com.pitstop.interactors.MacroUseCases.FacebookLoginAuthMacroUseCase;
import com.pitstop.interactors.MacroUseCases.FacebookSignUpAuthMacroUseCase;
import com.pitstop.interactors.MacroUseCases.LoginAuthMacroUseCase;
import com.pitstop.interactors.MacroUseCases.SignUpAuthMacroUseCase;
import com.pitstop.interactors.add.AddAlarmUseCase;
import com.pitstop.interactors.add.AddCarUseCase;
import com.pitstop.interactors.add.AddCustomServiceUseCase;
import com.pitstop.interactors.add.AddDtcUseCase;
import com.pitstop.interactors.add.AddLicensePlateUseCase;
import com.pitstop.interactors.add.AddPidUseCase;
import com.pitstop.interactors.add.AddScannerUseCase;
import com.pitstop.interactors.add.AddServicesUseCase;
import com.pitstop.interactors.add.AddShopUseCase;
import com.pitstop.interactors.add.GenerateReportUseCase;
import com.pitstop.interactors.check.CheckFirstCarAddedUseCase;
import com.pitstop.interactors.check.CheckNetworkConnectionUseCase;
import com.pitstop.interactors.emissions.Post2141UseCase;
import com.pitstop.interactors.get.GetAlarmCountUseCase;
import com.pitstop.interactors.get.GetAlarmsUseCase;
import com.pitstop.interactors.get.GetAllAppointmentsUseCase;
import com.pitstop.interactors.get.GetAppointmentStateUseCase;
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
import com.pitstop.interactors.get.GetPredictedServiceUseCase;
import com.pitstop.interactors.get.GetReportsUseCase;
import com.pitstop.interactors.get.GetShopHoursUseCase;
import com.pitstop.interactors.get.GetSnapToRoadUseCase;
import com.pitstop.interactors.get.GetTripsUseCase;
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCase;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.get.GetUserNotificationUseCase;
import com.pitstop.interactors.other.ChangePasswordActivateUserUseCase;
import com.pitstop.interactors.other.DeviceClockSyncUseCase;
import com.pitstop.interactors.other.DiscoveryTimeoutUseCase;
import com.pitstop.interactors.other.FacebookSignUpUseCase;
import com.pitstop.interactors.other.HandleVinOnConnectUseCase;
import com.pitstop.interactors.other.LoginFacebookUseCase;
import com.pitstop.interactors.other.LoginUseCase;
import com.pitstop.interactors.other.MarkServiceDoneUseCase;
import com.pitstop.interactors.other.ProcessTripDataUseCase;
import com.pitstop.interactors.other.RequestServiceUseCase;
import com.pitstop.interactors.other.ResetPasswordUseCase;
import com.pitstop.interactors.other.SendFleetManagerSmsUseCase;
import com.pitstop.interactors.other.SendPendingUpdatesUseCase;
import com.pitstop.interactors.other.SignUpUseCase;
import com.pitstop.interactors.other.SmoochLoginUseCase;
import com.pitstop.interactors.other.SortReportsUseCase;
import com.pitstop.interactors.other.StartDumpingTripDataWhenConnecteUseCase;
import com.pitstop.interactors.other.StoreFuelConsumedUseCase;
import com.pitstop.interactors.remove.RemoveCarUseCase;
import com.pitstop.interactors.remove.RemoveShopUseCase;
import com.pitstop.interactors.remove.RemoveTripUseCase;
import com.pitstop.interactors.set.SetAlarmsEnabledUseCase;
import com.pitstop.interactors.set.SetFirstCarAddedUseCase;
import com.pitstop.interactors.set.SetNotificationReadUseCase;
import com.pitstop.interactors.set.SetServicesDoneUseCase;
import com.pitstop.interactors.set.SetTimezoneUseCase;
import com.pitstop.interactors.set.SetUnitOfLengthUseCase;
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

    GetUserNotificationUseCase getUserNotificationUseCase();

    UpdateCarMileageUseCase updateCarMileageUseCase();

    DiscoveryTimeoutUseCase discoveryTimeoutUseCase();

    SendFleetManagerSmsUseCase sendFleetManagerSmsUseCase();

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

    SetUnitOfLengthUseCase getSetUnitOfLengthUseCase();

    SetTimezoneUseCase setTimezoneUseCase();

    SetServicesDoneUseCase getSetServicesDoneUseCase();

    SetNotificationReadUseCase getSetNotificationReadUseCase();

    GetDealershipWithCarIssuesUseCase getDealershipWithCarIssuesUseCase();

    GetCurrentCarDealershipUseCase getGetCurrentDealershipUseCase();

    DeviceClockSyncUseCase getDeviceClockSyncUseCase();

    AddAlarmUseCase addAlarmUseCase();

    GetAlarmsUseCase getAlarmsUseCase();

    SetAlarmsEnabledUseCase getSetAlarmsEnableduseCase();

    GetAlarmCountUseCase getGetAlarmCountUseCase();

    StoreFuelConsumedUseCase getStoreFuelConsumedUseCase();

    GetFuelConsumedUseCase getGetFuelConsumedUseCase();

    GetFuelPricesUseCase getFuelPriceUseCase();

    GetFuelConsumedAndPriceUseCase getGetFuelConsumedAndPriceUseCase();

    AddScannerUseCase getAddScannerUseCase();

    GetAllAppointmentsUseCase getAllAppointmentsUseCase();

    GetPredictedServiceUseCase getPredictedServiceDateUseCase();

    GetAppointmentStateUseCase getAppointmentStateUseCase();

    SmoochLoginUseCase getSmoochLoginUseCase();

    GetTripsUseCase getTripsUseCase();

    GetSnapToRoadUseCase getSnapToRoadUseCase();

    RemoveTripUseCase removeTripUseCase();

    StartDumpingTripDataWhenConnecteUseCase getStartDumpingTripDataWhenConnectedUseCase();

    AddPidUseCase addPidUseCase();

    SendPendingUpdatesUseCase sendPendingUpdatesUseCase();

    ProcessTripDataUseCase processTripDataUseCase();

    SignUpUseCase signUpUseCase();

    LoginUseCase loginUseCase();

    LoginFacebookUseCase facebookLoginUseCase();

    FacebookSignUpUseCase facebookSignUpUseCase();

    FacebookSignUpAuthMacroUseCase facebookSignUpAuthMacroUseCase();

    FacebookLoginAuthMacroUseCase facebookLoginAuthMacroUseCase();

    LoginAuthMacroUseCase loginAuthMacroUseCase();

    SignUpAuthMacroUseCase signUpAuthMacroUseCase();

    ChangePasswordActivateUserUseCase changePasswordActivateUserUseCase();

    ResetPasswordUseCase resetPasswordUseCase();
}
