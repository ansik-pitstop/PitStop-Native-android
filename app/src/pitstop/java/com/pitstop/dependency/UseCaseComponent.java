package com.pitstop.dependency;

import com.pitstop.interactors.AddCarUseCase;
import com.pitstop.interactors.AddShopUseCase;
import com.pitstop.interactors.CheckFirstCarAddedUseCase;
import com.pitstop.interactors.GetPrevIgnitionTimeUseCase;
import com.pitstop.interactors.HandleVinOnConnectUseCase;
import com.pitstop.interactors.GetCarByCarIdUseCase;
import com.pitstop.interactors.GetCarsByUserIdUseCase;
import com.pitstop.interactors.GetCurrentServicesUseCase;
import com.pitstop.interactors.GetCurrentUserUseCase;
import com.pitstop.interactors.GetDoneServicesUseCase;
import com.pitstop.interactors.GetGooglePlacesShopsUseCase;
import com.pitstop.interactors.GetPitstopShopsUseCase;
import com.pitstop.interactors.GetPlaceDetailsUseCase;
import com.pitstop.interactors.GetUpcomingServicesMapUseCase;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.interactors.GetUserShopsUseCase;
import com.pitstop.interactors.MarkServiceDoneUseCase;
import com.pitstop.interactors.RemoveCarUseCase;
import com.pitstop.interactors.RemoveShopUseCase;
import com.pitstop.interactors.RequestServiceUseCase;
import com.pitstop.interactors.SetFirstCarAddedUseCase;
import com.pitstop.interactors.SetUserCarUseCase;
import com.pitstop.interactors.UpdateCarDealershipUseCase;
import com.pitstop.interactors.UpdateShopUseCase;
import com.pitstop.interactors.UpdateUserNameUseCase;
import com.pitstop.interactors.UpdateUserPhoneUseCase;
import com.pitstop.interactors.Trip215EndUseCase;
import com.pitstop.interactors.Trip215StartUseCase;

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

    RequestServiceUseCase requestServiceUseCase();

    SetUserCarUseCase setUseCarUseCase();

    SetFirstCarAddedUseCase setFirstCarAddedUseCase();

    HandleVinOnConnectUseCase handleVinOnConnectUseCase();

    Trip215StartUseCase trip215StartUseCase();

    Trip215EndUseCase trip215EndUseCase();

    GetPrevIgnitionTimeUseCase getPrevIgnitionTimeUseCase();

}
