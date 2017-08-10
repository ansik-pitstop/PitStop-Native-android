package com.pitstop.dependency;

import android.os.Handler;

import com.pitstop.interactors.add.AddCarUseCase;
import com.pitstop.interactors.add.AddCarUseCaseImpl;
import com.pitstop.interactors.add.AddCustomServiceUseCase;
import com.pitstop.interactors.add.AddCustomServiceUseCaseImpl;
import com.pitstop.interactors.add.AddServiceUseCase;
import com.pitstop.interactors.add.AddServiceUseCaseImpl;
import com.pitstop.interactors.add.AddServicesUseCase;
import com.pitstop.interactors.add.AddServicesUseCaseImpl;
import com.pitstop.interactors.add.AddShopUseCase;
import com.pitstop.interactors.add.AddShopUseCaseImpl;
import com.pitstop.interactors.check.CheckFirstCarAddedUseCase;
import com.pitstop.interactors.check.CheckFirstCarAddedUseCaseImpl;
import com.pitstop.interactors.get.GetCarByCarIdUseCase;
import com.pitstop.interactors.get.GetCarByCarIdUseCaseImpl;
import com.pitstop.interactors.get.GetCarsByUserIdUseCase;
import com.pitstop.interactors.get.GetCarsByUserIdUseCaseImpl;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.get.GetCurrentServicesUseCaseImpl;
import com.pitstop.interactors.get.GetCurrentUserUseCase;
import com.pitstop.interactors.get.GetCurrentUserUseCaseImpl;
import com.pitstop.interactors.get.GetDoneServicesUseCase;
import com.pitstop.interactors.get.GetDoneServicesUseCaseImpl;
import com.pitstop.interactors.get.GetGooglePlacesShopsUseCase;
import com.pitstop.interactors.get.GetGooglePlacesShopsUseCaseImpl;
import com.pitstop.interactors.get.GetPitstopShopsUseCase;
import com.pitstop.interactors.get.GetPitstopShopsUseCaseImpl;
import com.pitstop.interactors.get.GetPlaceDetailsUseCase;
import com.pitstop.interactors.get.GetPlaceDetailsUseCaseImpl;
import com.pitstop.interactors.get.GetPrevIgnitionTimeUseCase;
import com.pitstop.interactors.get.GetPrevIgnitionTimeUseCaseImpl;
import com.pitstop.interactors.get.GetShopHoursUseCase;
import com.pitstop.interactors.get.GetShopHoursUseCaseImpl;
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCase;
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCaseImpl;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.get.GetUserCarUseCaseImpl;
import com.pitstop.interactors.get.GetUserShopsUseCase;
import com.pitstop.interactors.get.GetUserShopsUseCaseImpl;
import com.pitstop.interactors.other.HandleVinOnConnectUseCase;
import com.pitstop.interactors.other.HandleVinOnConnectUseCaseImpl;
import com.pitstop.interactors.other.MarkServiceDoneUseCase;
import com.pitstop.interactors.other.MarkServiceDoneUseCaseImpl;
import com.pitstop.interactors.other.RequestServiceUseCase;
import com.pitstop.interactors.other.RequestServiceUseCaseImpl;
import com.pitstop.interactors.other.Trip215EndUseCase;
import com.pitstop.interactors.other.Trip215EndUseCaseImpl;
import com.pitstop.interactors.other.Trip215StartUseCase;
import com.pitstop.interactors.other.Trip215StartUseCaseImpl;
import com.pitstop.interactors.remove.RemoveCarUseCase;
import com.pitstop.interactors.remove.RemoveCarUseCaseImpl;
import com.pitstop.interactors.remove.RemoveShopUseCase;
import com.pitstop.interactors.remove.RemoveShopUseCaseImpl;
import com.pitstop.interactors.set.SetFirstCarAddedUseCase;
import com.pitstop.interactors.set.SetFirstCarAddedUseCaseImpl;
import com.pitstop.interactors.set.SetUserCarUseCase;
import com.pitstop.interactors.set.SetUserCarUseCaseImpl;
import com.pitstop.interactors.update.UpdateCarDealershipUseCase;
import com.pitstop.interactors.update.UpdateCarDealershipUseCaseImpl;
import com.pitstop.interactors.update.UpdateShopUseCase;
import com.pitstop.interactors.update.UpdateShopUseCaseImpl;
import com.pitstop.interactors.update.UpdateUserNameUseCase;
import com.pitstop.interactors.update.UpdateUserNameUseCaseImpl;
import com.pitstop.interactors.update.UpdateUserPhoneUseCase;
import com.pitstop.interactors.update.UpdateUserPhoneUseCaseImpl;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.ScannerRepository;
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
    AddCustomServiceUseCase addCustomServiceUseCase(CarRepository carRepository, CarIssueRepository carIssueRepository, UserRepository userRepository, Handler handler){
        return new AddCustomServiceUseCaseImpl(carRepository,userRepository,carIssueRepository,handler);
    }

    @Provides
    GetShopHoursUseCase getShopHoursUseCase(ShopRepository shopRepository, UserRepository userRepository, NetworkHelper networkHelper, Handler handler){
        return new GetShopHoursUseCaseImpl(shopRepository,userRepository,networkHelper,handler);
    }

    @Provides
    AddServiceUseCase addServiceUseCase(CarIssueRepository carIssueRepository, Handler handler){
        return new AddServiceUseCaseImpl(carIssueRepository, handler);
    }

    @Provides
    RequestServiceUseCase requestServiceUseCase(CarIssueRepository carIssueRepository, UserRepository userRepository, CarRepository carRepository, Handler handler){
        return new RequestServiceUseCaseImpl(carIssueRepository,userRepository,carRepository,handler);
    }

    @Provides
    AddServicesUseCase addServicesUseCase(CarIssueRepository carIssueRepository, UserRepository userRepository, Handler handler){
        return new AddServicesUseCaseImpl(carIssueRepository,userRepository,handler);
    }

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
    AddCarUseCase addCarUseCase(CarRepository carRepository, ScannerRepository scannerRepository
            , UserRepository userRepository, Handler handler){

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
    GetUserCarUseCase getUserCarUseCase(UserRepository userRepository,CarRepository carRepository, Handler handler){
        return new GetUserCarUseCaseImpl(userRepository,carRepository, handler);
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
            , CarRepository carRepository, UserRepository userRepository, Handler handler){

        return new HandleVinOnConnectUseCaseImpl(scannerRepository, carRepository
                , userRepository,  handler);
    }

    @Provides
    GetPrevIgnitionTimeUseCase getPreviousIgnitionTimeUseCase(
            Device215TripRepository device215TripRepository, Handler handler){

        return new GetPrevIgnitionTimeUseCaseImpl(device215TripRepository, handler);
    }

}
