package com.pitstop.dependency;

import android.os.Handler;

import com.pitstop.database.LocalPidStorage;
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
import com.pitstop.interactors.add.GenerateReportUseCase;
import com.pitstop.interactors.add.GenerateReportUseCaseImpl;
import com.pitstop.interactors.check.CheckFirstCarAddedUseCase;
import com.pitstop.interactors.check.CheckFirstCarAddedUseCaseImpl;
import com.pitstop.interactors.emissions.Post2141UseCase;
import com.pitstop.interactors.emissions.Post2141UseCaseImpl;
import com.pitstop.interactors.get.GetCarByCarIdUseCase;
import com.pitstop.interactors.get.GetCarByCarIdUseCaseImpl;
import com.pitstop.interactors.get.GetCarByVinUseCase;
import com.pitstop.interactors.get.GetCarByVinUseCaseImpl;
import com.pitstop.interactors.get.GetCarsByUserIdUseCase;
import com.pitstop.interactors.get.GetCarsByUserIdUseCaseImpl;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.get.GetCurrentServicesUseCaseImpl;
import com.pitstop.interactors.get.GetCurrentUserUseCase;
import com.pitstop.interactors.get.GetCurrentUserUseCaseImpl;
import com.pitstop.interactors.get.GetDTCUseCase;
import com.pitstop.interactors.get.GetDTCUseCaseImpl;
import com.pitstop.interactors.get.GetDoneServicesUseCase;
import com.pitstop.interactors.get.GetDoneServicesUseCaseImpl;
import com.pitstop.interactors.get.GetGooglePlacesShopsUseCase;
import com.pitstop.interactors.get.GetGooglePlacesShopsUseCaseImpl;
import com.pitstop.interactors.get.GetPIDUseCase;
import com.pitstop.interactors.get.GetPIDUseCaseImpl;
import com.pitstop.interactors.get.GetPitstopShopsUseCase;
import com.pitstop.interactors.get.GetPitstopShopsUseCaseImpl;
import com.pitstop.interactors.get.GetPlaceDetailsUseCase;
import com.pitstop.interactors.get.GetPlaceDetailsUseCaseImpl;
import com.pitstop.interactors.get.GetPrevIgnitionTimeUseCase;
import com.pitstop.interactors.get.GetPrevIgnitionTimeUseCaseImpl;
import com.pitstop.interactors.get.GetReportUseCaseImpl;
import com.pitstop.interactors.get.GetReportsUseCase;
import com.pitstop.interactors.get.GetShopHoursUseCase;
import com.pitstop.interactors.get.GetShopHoursUseCaseImpl;
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCase;
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCaseImpl;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.get.GetUserCarUseCaseImpl;
import com.pitstop.interactors.get.GetUserNotificationUseCase;
import com.pitstop.interactors.get.GetUserNotificationUseCaseImpl;
import com.pitstop.interactors.get.GetUserShopsUseCase;
import com.pitstop.interactors.get.GetUserShopsUseCaseImpl;
import com.pitstop.interactors.other.DiscoveryTimeoutUseCase;
import com.pitstop.interactors.other.DiscoveryTimeoutUseCaseImpl;
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
import com.pitstop.interactors.other.SortVehicleHealthReportUseCaseImpl;
import com.pitstop.interactors.other.SortVehicleHealthReportsUseCase;
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
import com.pitstop.interactors.update.UpdateCarMileageUseCase;
import com.pitstop.interactors.update.UpdateCarMileageUseCaseImpl;
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
import com.pitstop.repositories.ReportRepository;
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
    Post2141UseCase getPost2141UseCase(Device215TripRepository device215TripRepository
            , NetworkHelper networkHelper, @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){

        return new Post2141UseCaseImpl(device215TripRepository, networkHelper
                , useCaseHandler, mainHandler);
    }

    @Provides
    GetPIDUseCase getPIDUseCase(@Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new GetPIDUseCaseImpl(useCaseHandler, mainHandler);
    }

    @Provides
    GetDTCUseCase getDTCUseCase(@Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new GetDTCUseCaseImpl(useCaseHandler, mainHandler);
    }

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

        return new GetShopHoursUseCaseImpl(shopRepository,userRepository,networkHelper
                ,useCaseHandler, mainHandler);
    }

    @Provides
    AddServiceUseCase addServiceUseCase(CarIssueRepository carIssueRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new AddServiceUseCaseImpl(carIssueRepository, useCaseHandler, mainHandler);
    }

    @Provides
    RequestServiceUseCase requestServiceUseCase(CarIssueRepository carIssueRepository
            , UserRepository userRepository, CarRepository carRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new RequestServiceUseCaseImpl(carIssueRepository,userRepository,carRepository
                ,useCaseHandler, mainHandler);
    }

    @Provides
    AddServicesUseCase addServicesUseCase(CarIssueRepository carIssueRepository
            , UserRepository userRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new AddServicesUseCaseImpl(carIssueRepository,userRepository
                ,useCaseHandler, mainHandler);
    }

    @Provides
    RemoveShopUseCase removeShopUseCase(ShopRepository shopRepository,CarRepository carRepository
            ,UserRepository userRepository,NetworkHelper networkHelper
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new RemoveShopUseCaseImpl(shopRepository,carRepository,userRepository
                ,networkHelper,useCaseHandler, mainHandler);
    }

    @Provides
    UpdateUserPhoneUseCase updateUserPhoneUseCase(UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new UpdateUserPhoneUseCaseImpl(userRepository, useCaseHandler, mainHandler);
    }


    @Provides
    UpdateUserNameUseCase updateUserNameUseCase(UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new UpdateUserNameUseCaseImpl(userRepository, useCaseHandler, mainHandler);
    }


    @Provides
    GetCurrentUserUseCase getCurrentUserUseCase(UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new GetCurrentUserUseCaseImpl(userRepository,useCaseHandler,mainHandler);
    }


    @Provides
    GetCarByCarIdUseCase getCarByCarIdUseCase(UserRepository userRepository
            , CarRepository carRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return  new GetCarByCarIdUseCaseImpl(carRepository, userRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    GetPlaceDetailsUseCase getPlaceDetailsUseCase(NetworkHelper networkHelper
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

       return new GetPlaceDetailsUseCaseImpl(networkHelper, useCaseHandler, mainHandler);
    }

    @Provides
    GetGooglePlacesShopsUseCase getGooglePlacesShopsUseCase(NetworkHelper networkHelper
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new GetGooglePlacesShopsUseCaseImpl(networkHelper, useCaseHandler, mainHandler);
    }

    @Provides
    GetUserShopsUseCase getUserShopsUseCase(ShopRepository shopRepository
            ,UserRepository userRepository,NetworkHelper networkHelper
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new GetUserShopsUseCaseImpl(shopRepository,userRepository,networkHelper
                ,useCaseHandler, mainHandler);
    }

    @Provides
    UpdateShopUseCase updateShopUseCase(ShopRepository shopRepository,UserRepository userRepository
            ,CarRepository carRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new UpdateShopUseCaseImpl(shopRepository,userRepository,carRepository
                ,useCaseHandler, mainHandler);
    }

    @Provides
    AddShopUseCase addShopUseCase(ShopRepository shopRepository, UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new AddShopUseCaseImpl(shopRepository,userRepository,useCaseHandler, mainHandler);
    }


    @Provides
    UpdateCarDealershipUseCase updateCarDealershipUseCase(CarRepository carRepository
            , UserRepository userRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new UpdateCarDealershipUseCaseImpl(carRepository,userRepository
                ,useCaseHandler, mainHandler);
    }

    @Provides
    GetPitstopShopsUseCase getPitstopShopsUseCase(ShopRepository shopRepository
            , NetworkHelper networkHelper, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new GetPitstopShopsUseCaseImpl(shopRepository,networkHelper
                ,useCaseHandler, mainHandler);
    }

    @Provides
    GetCarsByUserIdUseCase getCarsByUserIdUseCase(UserRepository userRepository
            , CarRepository carRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new GetCarsByUserIdUseCaseImpl(userRepository, carRepository
                ,useCaseHandler, mainHandler);
    }

    @Provides
    CheckFirstCarAddedUseCase getCheckFirstCarAddedUseCase(UserRepository userRepository
            , CarRepository carRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){
        return new CheckFirstCarAddedUseCaseImpl(userRepository, carRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    AddCarUseCase addCarUseCase(CarRepository carRepository, ScannerRepository scannerRepository
            , UserRepository userRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new AddCarUseCaseImpl(carRepository, scannerRepository, userRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    GetCurrentServicesUseCase getCurrentServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new GetCurrentServicesUseCaseImpl(userRepository, carIssueRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    GetDoneServicesUseCase getDoneServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new GetDoneServicesUseCaseImpl(userRepository, carIssueRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    GetUpcomingServicesMapUseCase getUpcomingServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new GetUpcomingServicesMapUseCaseImpl(userRepository, carIssueRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    GetUserCarUseCase getUserCarUseCase(UserRepository userRepository,CarRepository carRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new GetUserCarUseCaseImpl(userRepository,carRepository, useCaseHandler, mainHandler);
    }

    @Provides
    MarkServiceDoneUseCase markServiceDoneUseCase(CarIssueRepository carIssueRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new MarkServiceDoneUseCaseImpl(carIssueRepository, useCaseHandler, mainHandler);
    }

    @Provides
    RemoveCarUseCase removeCarUseCase(UserRepository userRepository, CarRepository carRepository
            ,NetworkHelper networkHelper, @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new RemoveCarUseCaseImpl(userRepository,carRepository,networkHelper
                ,useCaseHandler, mainHandler);
    }


    @Provides
    SetUserCarUseCase setUseCarUseCase(UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new SetUserCarUseCaseImpl(userRepository, useCaseHandler, mainHandler);
    }

    @Provides
    SetFirstCarAddedUseCase setFirstCarAddedUseCase(UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){
        return new SetFirstCarAddedUseCaseImpl(userRepository,useCaseHandler,mainHandler);
    }

    @Provides
    Trip215StartUseCase trip215StartUseCase(Device215TripRepository device215TripRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new Trip215StartUseCaseImpl(device215TripRepository, useCaseHandler, mainHandler);
    }

    @Provides
    Trip215EndUseCase trip215EndUseCase(Device215TripRepository device215TripRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new Trip215EndUseCaseImpl(device215TripRepository, useCaseHandler,mainHandler);
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
                , userRepository,  useCaseHandler, mainHandler);
    }

    @Provides
    GetPrevIgnitionTimeUseCase getPreviousIgnitionTimeUseCase(
            Device215TripRepository device215TripRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new GetPrevIgnitionTimeUseCaseImpl(device215TripRepository, useCaseHandler,mainHandler);
    }

    @Provides
    GetUserNotificationUseCase getUserNotificationUseCase(
            UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new GetUserNotificationUseCaseImpl(userRepository, mainHandler, useCaseHandler);
    }



    @Provides
    HandlePidDataUseCase handlePidDataUseCase(PidRepository pidRepository
            , Device215TripRepository device215TripRepository, LocalPidStorage localPidStorage
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new HandlePidDataUseCaseImpl(pidRepository,device215TripRepository
                , localPidStorage, useCaseHandler, mainHandler);
    }

    @Provides
    UpdateCarMileageUseCase updateCarMileageUseCase(CarRepository carRepository
            , UserRepository userRepository, @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){

        return new UpdateCarMileageUseCaseImpl(carRepository, userRepository, useCaseHandler
                , mainHandler);
    }

    @Provides
    DiscoveryTimeoutUseCase discoveryTimeoutUseCase(@Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){

        return new DiscoveryTimeoutUseCaseImpl(useCaseHandler,mainHandler);
    }

    @Provides
    GetCarByVinUseCase getCarByVinUseCase(@Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler, CarRepository carRepository){
        return new GetCarByVinUseCaseImpl(useCaseHandler,mainHandler,carRepository);
    }

    @Provides
    GenerateReportUseCase addVehicleHealthReportUseCase(ReportRepository reportRepository
            , UserRepository userRepository, @Named("mainHandler") Handler mainHandler
            , @Named("useCaseHandler")Handler useCaseHandler){

        return new GenerateReportUseCaseImpl(reportRepository,userRepository
                ,mainHandler,useCaseHandler);
    }

    @Provides
    GetReportsUseCase getReportsUseCase(ReportRepository reportRepository
            , UserRepository userRepository, @Named("mainHandler") Handler mainHandler
            , @Named("useCaseHandler")Handler useCaseHandler){

        return new GetReportUseCaseImpl(userRepository,reportRepository
                ,useCaseHandler,mainHandler);
    }

    @Provides
    SortVehicleHealthReportsUseCase sortVehicleHealthReportsUseCase(
            @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){

        return new SortVehicleHealthReportUseCaseImpl(useCaseHandler, mainHandler);
    }
}
