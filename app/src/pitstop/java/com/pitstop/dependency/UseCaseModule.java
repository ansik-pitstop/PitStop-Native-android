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
import com.pitstop.interactors.check.CheckTripEndedUseCase;
import com.pitstop.interactors.check.CheckTripEndedUseCaseImpl;
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
import com.pitstop.interactors.other.HandlePidDataUseCase;
import com.pitstop.interactors.other.HandlePidDataUseCaseImpl;
import com.pitstop.interactors.other.HandleVinOnConnectUseCase;
import com.pitstop.interactors.other.HandleVinOnConnectUseCaseImpl;
import com.pitstop.interactors.other.MarkServiceDoneUseCase;
import com.pitstop.interactors.other.MarkServiceDoneUseCaseImpl;
import com.pitstop.interactors.other.PeriodicCachedTripSendUseCase;
import com.pitstop.interactors.other.PeriodicCachedTripSendUseCaseImpl;
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
import com.pitstop.repositories.PidRepository;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.ConnectionChecker;
import com.pitstop.utils.NetworkHelper;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

@Module(includes = {RepositoryModule.class, ConnectionCheckerModule.class
        , HandlerModule.class} )
public class UseCaseModule {

    @Provides
    AddCustomServiceUseCase addCustomServiceUseCase(CarRepository carRepository
            , CarIssueRepository carIssueRepository, UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new AddCustomServiceUseCaseImpl(carRepository,userRepository,carIssueRepository
                ,useCaseHandler, mainHandler);
    }

    @Provides
    GetShopHoursUseCase getShopHoursUseCase(ShopRepository shopRepository, UserRepository userRepository
            , NetworkHelper networkHelper, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new GetShopHoursUseCaseImpl(shopRepository,userRepository,networkHelper,useCaseHandler);
    }

    @Provides
    AddServiceUseCase addServiceUseCase(CarIssueRepository carIssueRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new AddServiceUseCaseImpl(carIssueRepository, useCaseHandler);
    }

    @Provides
    RequestServiceUseCase requestServiceUseCase(CarIssueRepository carIssueRepository
            , UserRepository userRepository, CarRepository carRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new RequestServiceUseCaseImpl(carIssueRepository,userRepository,carRepository
                ,useCaseHandler);
    }

    @Provides
    AddServicesUseCase addServicesUseCase(CarIssueRepository carIssueRepository
            , UserRepository userRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new AddServicesUseCaseImpl(carIssueRepository,userRepository,useCaseHandler);
    }

    @Provides
    RemoveShopUseCase removeShopUseCase(ShopRepository shopRepository,CarRepository carRepository
            ,UserRepository userRepository,NetworkHelper networkHelper
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new RemoveShopUseCaseImpl(shopRepository,carRepository,userRepository,networkHelper,useCaseHandler);
    }

    @Provides
    UpdateUserPhoneUseCase updateUserPhoneUseCase(UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new UpdateUserPhoneUseCaseImpl(userRepository, useCaseHandler);
    }


    @Provides
    UpdateUserNameUseCase updateUserNameUseCase(UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new UpdateUserNameUseCaseImpl(userRepository, useCaseHandler);
    }


    @Provides
    GetCurrentUserUseCase getCurrentUserUseCase(UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new GetCurrentUserUseCaseImpl(userRepository,useCaseHandler);
    }


    @Provides
    GetCarByCarIdUseCase getCarByCarIdUseCase(UserRepository userRepository
            , CarRepository carRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return  new GetCarByCarIdUseCaseImpl(carRepository, userRepository, useCaseHandler);
    }

    @Provides
    GetPlaceDetailsUseCase getPlaceDetailsUseCase(NetworkHelper networkHelper
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
       return new GetPlaceDetailsUseCaseImpl(networkHelper, useCaseHandler);
    }

    @Provides
    GetGooglePlacesShopsUseCase getGooglePlacesShopsUseCase(NetworkHelper networkHelper
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new GetGooglePlacesShopsUseCaseImpl(networkHelper, useCaseHandler);
    }

    @Provides
    GetUserShopsUseCase getUserShopsUseCase(ShopRepository shopRepository
            ,UserRepository userRepository,NetworkHelper networkHelper
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new GetUserShopsUseCaseImpl(shopRepository,userRepository,networkHelper,useCaseHandler);
    }

    @Provides
    UpdateShopUseCase updateShopUseCase(ShopRepository shopRepository,UserRepository userRepository
            ,CarRepository carRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new UpdateShopUseCaseImpl(shopRepository,userRepository,carRepository,useCaseHandler);
    }

    @Provides
    AddShopUseCase addShopUseCase(ShopRepository shopRepository, UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new AddShopUseCaseImpl(shopRepository,userRepository,useCaseHandler);
    }


    @Provides
    UpdateCarDealershipUseCase updateCarDealershipUseCase(CarRepository carRepository
            , UserRepository userRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new UpdateCarDealershipUseCaseImpl(carRepository,userRepository,useCaseHandler);
    }

    @Provides
    GetPitstopShopsUseCase getPitstopShopsUseCase(ShopRepository shopRepository
            , NetworkHelper networkHelper, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new GetPitstopShopsUseCaseImpl(shopRepository,networkHelper,useCaseHandler);
    }

    @Provides
    GetCarsByUserIdUseCase getCarsByUserIdUseCase(UserRepository userRepository
            , CarRepository carRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new GetCarsByUserIdUseCaseImpl(userRepository, carRepository,useCaseHandler);
    }

    @Provides
    CheckFirstCarAddedUseCase getCheckFirstCarAddedUseCase(UserRepository userRepository
            , CarRepository carRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new CheckFirstCarAddedUseCaseImpl(userRepository, carRepository, useCaseHandler);
    }

    @Provides
    AddCarUseCase addCarUseCase(CarRepository carRepository, ScannerRepository scannerRepository
            , UserRepository userRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new AddCarUseCaseImpl(carRepository, scannerRepository, userRepository, useCaseHandler);
    }

    @Provides
    GetCurrentServicesUseCase getCurrentServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new GetCurrentServicesUseCaseImpl(userRepository, carIssueRepository, useCaseHandler);
    }

    @Provides
    GetDoneServicesUseCase getDoneServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new GetDoneServicesUseCaseImpl(userRepository, carIssueRepository, useCaseHandler);
    }

    @Provides
    GetUpcomingServicesMapUseCase getUpcomingServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new GetUpcomingServicesMapUseCaseImpl(userRepository, carIssueRepository, useCaseHandler);
    }

    @Provides
    GetUserCarUseCase getUserCarUseCase(UserRepository userRepository,CarRepository carRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new GetUserCarUseCaseImpl(userRepository,carRepository, useCaseHandler);
    }

    @Provides
    MarkServiceDoneUseCase markServiceDoneUseCase(CarIssueRepository carIssueRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new MarkServiceDoneUseCaseImpl(carIssueRepository, useCaseHandler);
    }

    @Provides
    RemoveCarUseCase removeCarUseCase(UserRepository userRepository, CarRepository carRepository
            ,NetworkHelper networkHelper, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new RemoveCarUseCaseImpl(userRepository,carRepository,networkHelper,useCaseHandler);
    }


    @Provides
    SetUserCarUseCase setUseCarUseCase(UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new SetUserCarUseCaseImpl(userRepository, useCaseHandler);
    }

    @Provides
    SetFirstCarAddedUseCase setFirstCarAddedUseCase(UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new SetFirstCarAddedUseCaseImpl(userRepository);
    }

    @Provides
    Trip215StartUseCase trip215StartUseCase(Device215TripRepository device215TripRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new Trip215StartUseCaseImpl(device215TripRepository, useCaseHandler);
    }

    @Provides
    Trip215EndUseCase trip215EndUseCase(Device215TripRepository device215TripRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new Trip215EndUseCaseImpl(device215TripRepository, useCaseHandler);
    }

    @Provides
    PeriodicCachedTripSendUseCase periodicCachedTripSendUseCase(Device215TripRepository device215TripRepository
            , ConnectionChecker connectionChecker, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new PeriodicCachedTripSendUseCaseImpl(device215TripRepository
                , connectionChecker, useCaseHandler);
    }

    @Provides
    HandleVinOnConnectUseCase handleVinOnConnectUseCase(ScannerRepository scannerRepository
            , CarRepository carRepository, UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new HandleVinOnConnectUseCaseImpl(scannerRepository, carRepository
                , userRepository,  useCaseHandler);
    }

    @Provides
    GetPrevIgnitionTimeUseCase getPreviousIgnitionTimeUseCase(
            Device215TripRepository device215TripRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new GetPrevIgnitionTimeUseCaseImpl(device215TripRepository, useCaseHandler);
    }

    @Provides
    HandlePidDataUseCase handlePidDataUseCase(PidRepository pidRepository
            , Device215TripRepository device215TripRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new HandlePidDataUseCaseImpl(pidRepository,device215TripRepository, useCaseHandler);
    }

    @Provides
    CheckTripEndedUseCase checkTripEndedUseCase(Device215TripRepository device215TripRepository
            , Handler handler){
        return new CheckTripEndedUseCaseImpl(device215TripRepository, handler);
    }

    @Provides
    CheckTripEndedUseCase checkTripEndedUseCase(Device215TripRepository device215TripRepository
            , Handler handler){
        return new CheckTripEndedUseCaseImpl(device215TripRepository, handler);
    }

    @Provides
    CheckTripEndedUseCase checkTripEndedUseCase(Device215TripRepository device215TripRepository
            , Handler handler){
        return new CheckTripEndedUseCaseImpl(device215TripRepository, handler);
    }
}
