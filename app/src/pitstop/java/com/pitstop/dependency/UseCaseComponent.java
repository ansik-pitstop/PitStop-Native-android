package com.pitstop.dependency;

import com.pitstop.interactors.add.AddCarUseCase;
import com.pitstop.interactors.add.AddCustomServiceUseCase;
import com.pitstop.interactors.add.AddServicesUseCase;
import com.pitstop.interactors.add.AddShopUseCase;
import com.pitstop.interactors.check.CheckFirstCarAddedUseCase;
import com.pitstop.interactors.check.CheckTripEndedUseCase;
import com.pitstop.interactors.get.GetPrevIgnitionTimeUseCase;
import com.pitstop.interactors.other.HandlePidDataUseCase;
import com.pitstop.interactors.other.HandleVinOnConnectUseCase;
import com.pitstop.interactors.get.GetCarByCarIdUseCase;
import com.pitstop.interactors.get.GetCarsByUserIdUseCase;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.get.GetCurrentUserUseCase;
import com.pitstop.interactors.get.GetDoneServicesUseCase;
import com.pitstop.interactors.get.GetGooglePlacesShopsUseCase;
import com.pitstop.interactors.get.GetPitstopShopsUseCase;
import com.pitstop.interactors.get.GetPlaceDetailsUseCase;
import com.pitstop.interactors.get.GetShopHoursUseCase;
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCase;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.get.GetUserShopsUseCase;
import com.pitstop.interactors.other.MarkServiceDoneUseCase;
import com.pitstop.interactors.other.PeriodicCachedTripSendUseCase;
import com.pitstop.interactors.remove.RemoveCarUseCase;
import com.pitstop.interactors.remove.RemoveShopUseCase;
import com.pitstop.interactors.add.AddServiceUseCase;
import com.pitstop.interactors.other.RequestServiceUseCase;
import com.pitstop.interactors.set.SetFirstCarAddedUseCase;
import com.pitstop.interactors.set.SetUserCarUseCase;
import com.pitstop.interactors.update.UpdateCarDealershipUseCase;
import com.pitstop.interactors.update.UpdateShopUseCase;
import com.pitstop.interactors.update.UpdateUserNameUseCase;
import com.pitstop.interactors.update.UpdateUserPhoneUseCase;
import com.pitstop.interactors.other.Trip215EndUseCase;
import com.pitstop.interactors.other.Trip215StartUseCase;

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

    AddCustomServiceUseCase getAddCustomServiceUseCase();

    GetShopHoursUseCase getGetShopHoursUseCase();

    RequestServiceUseCase getRequestServiceUseCase();

    AddServicesUseCase getAddServicesUseCase();

    AddServiceUseCase getAddServiceUseCase();

    RemoveShopUseCase getRemoveShopUseCase();

    UpdateUserPhoneUseCase getUpdateUserPhoneUseCase();

    UpdateUserNameUseCase getUpdateUserNameUseCase();

    GetCurrentUserUseCase getGetCurrentUserUseCase();

    GetCarByCarIdUseCase getGetCarByCarIdUseCase();

    GetPlaceDetailsUseCase getGetPlaceDetailsUseCase();

    GetGooglePlacesShopsUseCase getGetGooglePlacesShopsUseCase();

    GetUserShopsUseCase getGetUserShopsUseCase();

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

    SetUserCarUseCase setUseCarUseCase();

    SetFirstCarAddedUseCase setFirstCarAddedUseCase();

    HandleVinOnConnectUseCase handleVinOnConnectUseCase();

    Trip215StartUseCase trip215StartUseCase();

    Trip215EndUseCase trip215EndUseCase();

    GetPrevIgnitionTimeUseCase getPrevIgnitionTimeUseCase();

    HandlePidDataUseCase handlePidDataUseCase();

    PeriodicCachedTripSendUseCase periodicCachedTripSendUseCase();

    CheckTripEndedUseCase checkTripEndedUseCase();

}
