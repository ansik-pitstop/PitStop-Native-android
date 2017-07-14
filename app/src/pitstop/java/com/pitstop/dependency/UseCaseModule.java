package com.pitstop.dependency;

import android.os.Handler;

import com.pitstop.interactors.AddCarUseCase;
import com.pitstop.interactors.AddCarUseCaseImpl;
import com.pitstop.interactors.AddShopUseCase;
import com.pitstop.interactors.AddShopUseCaseImpl;
import com.pitstop.interactors.CheckFirstCarAddedUseCase;
import com.pitstop.interactors.CheckFirstCarAddedUseCaseImpl;
import com.pitstop.interactors.HandleVinOnConnectUseCase;
import com.pitstop.interactors.HandleVinOnConnectUseCaseImpl;
import com.pitstop.interactors.FoundBluetoothDeviceUseCase;
import com.pitstop.interactors.FoundBluetoothDeviceUseCaseImpl;
import com.pitstop.interactors.GetCarByCarIdUseCase;
import com.pitstop.interactors.GetCarByCarIdUseCaseImpl;
import com.pitstop.interactors.GetCarsByUserIdUseCase;
import com.pitstop.interactors.GetCarsByUserIdUseCaseImpl;
import com.pitstop.interactors.GetCurrentServicesUseCase;
import com.pitstop.interactors.GetCurrentServicesUseCaseImpl;
import com.pitstop.interactors.GetCurrentUserUseCase;
import com.pitstop.interactors.GetCurrentUserUseCaseImpl;
import com.pitstop.interactors.GetDoneServicesUseCase;
import com.pitstop.interactors.GetDoneServicesUseCaseImpl;
import com.pitstop.interactors.GetGooglePlacesShopsUseCase;
import com.pitstop.interactors.GetGooglePlacesShopsUseCaseImpl;
import com.pitstop.interactors.GetPitstopShopsUseCase;
import com.pitstop.interactors.GetPitstopShopsUseCaseImpl;
import com.pitstop.interactors.GetPlaceDetailsUseCase;
import com.pitstop.interactors.GetPlaceDetailsUseCaseImpl;
import com.pitstop.interactors.GetUpcomingServicesMapUseCase;
import com.pitstop.interactors.GetUpcomingServicesMapUseCaseImpl;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.interactors.GetUserCarUseCaseImpl;
import com.pitstop.interactors.GetUserShopsUseCase;
import com.pitstop.interactors.GetUserShopsUseCaseImpl;
import com.pitstop.interactors.MarkServiceDoneUseCase;
import com.pitstop.interactors.MarkServiceDoneUseCaseImpl;
import com.pitstop.interactors.RemoveCarUseCase;
import com.pitstop.interactors.RemoveCarUseCaseImpl;
import com.pitstop.interactors.RemoveShopUseCase;
import com.pitstop.interactors.RemoveShopUseCaseImpl;
import com.pitstop.interactors.RequestServiceUseCase;
import com.pitstop.interactors.RequestServiceUseCaseImpl;
import com.pitstop.interactors.SetFirstCarAddedUseCase;
import com.pitstop.interactors.SetFirstCarAddedUseCaseImpl;
import com.pitstop.interactors.SetUserCarUseCase;
import com.pitstop.interactors.SetUserCarUseCaseImpl;
import com.pitstop.interactors.Trip215EndUseCase;
import com.pitstop.interactors.Trip215EndUseCaseImpl;
import com.pitstop.interactors.Trip215StartUseCase;
import com.pitstop.interactors.Trip215StartUseCaseImpl;
import com.pitstop.interactors.UpdateCarDealershipUseCase;
import com.pitstop.interactors.UpdateCarDealershipUseCaseImpl;
import com.pitstop.interactors.UpdateShopUseCase;
import com.pitstop.interactors.UpdateShopUseCaseImpl;
import com.pitstop.interactors.UpdateUserNameUseCase;
import com.pitstop.interactors.UpdateUserNameUseCaseImpl;
import com.pitstop.interactors.UpdateUserPhoneUseCase;
import com.pitstop.interactors.UpdateUserPhoneUseCaseImpl;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

@Module(includes = {RepositoryModule.class, HandlerModule.class} )
public class UseCaseModule {

    @Provides
    RemoveShopUseCase removeShopUseCase(ShopRepository shopRepository,CarRepository carRepository,UserRepository userRepository,NetworkHelper networkHelper,Handler handler){
        return new RemoveShopUseCaseImpl(shopRepository,carRepository,userRepository,networkHelper,handler);
    }

    @Provides
    UpdateUserPhoneUseCase updateUserPhoneUseCase(UserRepository userRepository, Handler handler){
        return new UpdateUserPhoneUseCaseImpl(userRepository, handler);
    }


    @Provides
    UpdateUserNameUseCase updateUserNameUseCase(UserRepository userRepository, Handler handler){
        return new UpdateUserNameUseCaseImpl(userRepository, handler);
    }


    @Provides
    GetCurrentUserUseCase getCurrentUserUseCase(UserRepository userRepository, Handler handler){
        return new GetCurrentUserUseCaseImpl(userRepository,handler);
    }


    @Provides
    GetCarByCarIdUseCase getCarByCarIdUseCase(UserRepository userRepository, CarRepository carRepository, Handler handler){
        return  new GetCarByCarIdUseCaseImpl(carRepository, userRepository, handler);
    }

    @Provides
    GetPlaceDetailsUseCase getPlaceDetailsUseCase(NetworkHelper networkHelper, Handler handler){
       return new GetPlaceDetailsUseCaseImpl(networkHelper, handler);
    }

    @Provides
    GetGooglePlacesShopsUseCase getGooglePlacesShopsUseCase(NetworkHelper networkHelper, Handler handler){
        return new GetGooglePlacesShopsUseCaseImpl(networkHelper, handler);
    }

    @Provides
    GetUserShopsUseCase getUserShopsUseCase(ShopRepository shopRepository,UserRepository userRepository,NetworkHelper networkHelper,Handler handler){
        return new GetUserShopsUseCaseImpl(shopRepository,userRepository,networkHelper,handler);
    }

    @Provides
    UpdateShopUseCase updateShopUseCase(ShopRepository shopRepository,UserRepository userRepository,CarRepository carRepository,Handler handler){
        return new UpdateShopUseCaseImpl(shopRepository,userRepository,carRepository,handler);
    }

    @Provides
    AddShopUseCase addShopUseCase(ShopRepository shopRepository, UserRepository userRepository, Handler handler){
        return new AddShopUseCaseImpl(shopRepository,userRepository,handler);
    }


    @Provides
    UpdateCarDealershipUseCase updateCarDealershipUseCase(CarRepository carRepository, UserRepository userRepository, Handler handler){
        return new UpdateCarDealershipUseCaseImpl(carRepository,userRepository,handler);
    }

    @Provides
    GetPitstopShopsUseCase getPitstopShopsUseCase(ShopRepository shopRepository, NetworkHelper networkHelper, Handler handler){
        return new GetPitstopShopsUseCaseImpl(shopRepository,networkHelper,handler);
    }

    @Provides
    GetCarsByUserIdUseCase getCarsByUserIdUseCase(UserRepository userRepository, CarRepository carRepository, Handler handler){
        return new GetCarsByUserIdUseCaseImpl(userRepository, carRepository,handler);
    }

    @Provides
    CheckFirstCarAddedUseCase getCheckFirstCarAddedUseCase(UserRepository userRepository
            , Handler handler){
        return new CheckFirstCarAddedUseCaseImpl(userRepository, handler);
    }

    @Provides
    AddCarUseCase addCarUseCase(CarRepository carRepository, Handler handler){
        return new AddCarUseCaseImpl(carRepository, handler);
    }

    @Provides
    GetCurrentServicesUseCase getCurrentServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository, Handler handler){

        return new GetCurrentServicesUseCaseImpl(userRepository, carIssueRepository, handler);
    }

    @Provides
    GetDoneServicesUseCase getDoneServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository, Handler handler){

        return new GetDoneServicesUseCaseImpl(userRepository, carIssueRepository, handler);
    }

    @Provides
    GetUpcomingServicesMapUseCase getUpcomingServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository, Handler handler){

        return new GetUpcomingServicesMapUseCaseImpl(userRepository, carIssueRepository, handler);
    }

    @Provides
    GetUserCarUseCase getUserCarUseCase(UserRepository userRepository,NetworkHelper networkHelper, Handler handler){
        return new GetUserCarUseCaseImpl(userRepository,networkHelper, handler);
    }

    @Provides
    MarkServiceDoneUseCase markServiceDoneUseCase(CarIssueRepository carIssueRepository
            , Handler handler){

        return new MarkServiceDoneUseCaseImpl(carIssueRepository, handler);
    }

    @Provides
    RemoveCarUseCase removeCarUseCase(UserRepository userRepository, CarRepository carRepository,NetworkHelper networkHelper, Handler handler){
        return new RemoveCarUseCaseImpl(userRepository,carRepository,networkHelper,handler);
    }

    @Provides
    RequestServiceUseCase requestServiceUseCase(CarIssueRepository carIssueRepository
            , Handler handler){
        return new RequestServiceUseCaseImpl(carIssueRepository, handler);
    }

    @Provides
    SetUserCarUseCase setUseCarUseCase(UserRepository userRepository, Handler handler){
        return new SetUserCarUseCaseImpl(userRepository, handler);
    }

    @Provides
    SetFirstCarAddedUseCase setFirstCarAddedUseCase(UserRepository userRepository){
        return new SetFirstCarAddedUseCaseImpl(userRepository);
    }

    @Provides
    Trip215StartUseCase trip215StartUseCase(Device215TripRepository device215TripRepository
            , UserRepository userRepository, Handler handler){

        return new Trip215StartUseCaseImpl(device215TripRepository, handler);
    }

    @Provides
    Trip215EndUseCase trip215EndUseCase(Device215TripRepository device215TripRepository
            , UserRepository userRepository, Handler handler){

        return new Trip215EndUseCaseImpl(device215TripRepository, handler);
    }

    @Provides
    HandleVinOnConnectUseCase handleVinOnConnectUseCase(ScannerRepository scannerRepository
            , UserRepository userRepository, Handler handler){

        return new HandleVinOnConnectUseCaseImpl(scannerRepository
                , userRepository,  handler);
    }

    @Provides
    FoundBluetoothDeviceUseCase foundBluetoothDeviceUseCase(UserRepository userRepository
            , Handler handler){

        return new FoundBluetoothDeviceUseCaseImpl(userRepository, handler);
    }
}
