package com.pitstop.dependency;

import android.os.Handler;

import com.pitstop.database.LocalActivityStorage;
import com.pitstop.database.LocalAlarmStorage;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.database.LocalFuelConsumptionStorage;
import com.pitstop.database.LocalLocationStorage;
import com.pitstop.database.LocalSpecsStorage;
import com.pitstop.interactors.MacroUseCases.FacebookLoginAuthMacroUseCase;
import com.pitstop.interactors.MacroUseCases.FacebookLoginAuthMacroUseCaseImpl;
import com.pitstop.interactors.MacroUseCases.FacebookSignUpAuthMacroUseCase;
import com.pitstop.interactors.MacroUseCases.FacebookSignUpAuthMacroUseCaseImpl;
import com.pitstop.interactors.MacroUseCases.LoginAuthMacroUseCase;
import com.pitstop.interactors.MacroUseCases.LoginAuthMacroUseCaseImpl;
import com.pitstop.interactors.MacroUseCases.SignUpAuthMacroUseCase;
import com.pitstop.interactors.MacroUseCases.SignUpAuthMacroUseCaseImpl;
import com.pitstop.interactors.add.AddAlarmUseCase;
import com.pitstop.interactors.add.AddAlarmUseCaseImpl;
import com.pitstop.interactors.add.AddCarUseCase;
import com.pitstop.interactors.add.AddCarUseCaseImpl;
import com.pitstop.interactors.add.AddCustomServiceUseCase;
import com.pitstop.interactors.add.AddCustomServiceUseCaseImpl;
import com.pitstop.interactors.add.AddDtcUseCase;
import com.pitstop.interactors.add.AddDtcUseCaseImpl;
import com.pitstop.interactors.add.AddLicensePlateUseCase;
import com.pitstop.interactors.add.AddLicensePlateUseCaseImpl;
import com.pitstop.interactors.add.AddPidUseCase;
import com.pitstop.interactors.add.AddPidUseCaseImpl;
import com.pitstop.interactors.add.AddScannerUseCase;
import com.pitstop.interactors.add.AddScannerUseCaseImpl;
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
import com.pitstop.interactors.check.CheckNetworkConnectionUseCase;
import com.pitstop.interactors.check.CheckNetworkConnectionUseCaseImpl;
import com.pitstop.interactors.emissions.Post2141UseCase;
import com.pitstop.interactors.emissions.Post2141UseCaseImpl;
import com.pitstop.interactors.get.GetAlarmCountUseCase;
import com.pitstop.interactors.get.GetAlarmCountUseCaseImpl;
import com.pitstop.interactors.get.GetAlarmsUseCase;
import com.pitstop.interactors.get.GetAlarmsUseCaseImpl;
import com.pitstop.interactors.get.GetAllAppointmentsUseCase;
import com.pitstop.interactors.get.GetAllAppointmentsUseCaseImpl;
import com.pitstop.interactors.get.GetAppointmentStateUseCase;
import com.pitstop.interactors.get.GetAppointmentStateUseCaseImpl;
import com.pitstop.interactors.get.GetCarByCarIdUseCase;
import com.pitstop.interactors.get.GetCarByCarIdUseCaseImpl;
import com.pitstop.interactors.get.GetCarByVinUseCase;
import com.pitstop.interactors.get.GetCarByVinUseCaseImpl;
import com.pitstop.interactors.get.GetCarImagesArrayUseCase;
import com.pitstop.interactors.get.GetCarImagesArrayUseCaseImpl;
import com.pitstop.interactors.get.GetCarStyleIDUSeCaseImpl;
import com.pitstop.interactors.get.GetCarStyleIDUseCase;
import com.pitstop.interactors.get.GetCarsByUserIdUseCase;
import com.pitstop.interactors.get.GetCarsByUserIdUseCaseImpl;
import com.pitstop.interactors.get.GetCarsWithDealershipsUseCase;
import com.pitstop.interactors.get.GetCarsWithDealershipsUseCaseImpl;
import com.pitstop.interactors.get.GetCurrentCarDealershipUseCase;
import com.pitstop.interactors.get.GetCurrentCarDealershipUseCaseImpl;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.get.GetCurrentServicesUseCaseImpl;
import com.pitstop.interactors.get.GetCurrentUserUseCase;
import com.pitstop.interactors.get.GetCurrentUserUseCaseImpl;
import com.pitstop.interactors.get.GetDTCUseCase;
import com.pitstop.interactors.get.GetDTCUseCaseImpl;
import com.pitstop.interactors.get.GetDealershipWithCarIssuesUseCase;
import com.pitstop.interactors.get.GetDealershipWithCarIssuesUseCaseImpl;
import com.pitstop.interactors.get.GetDoneServicesUseCase;
import com.pitstop.interactors.get.GetDoneServicesUseCaseImpl;
import com.pitstop.interactors.get.GetFuelConsumedAndPriceUseCase;
import com.pitstop.interactors.get.GetFuelConsumedAndPriceUseCaseImpl;
import com.pitstop.interactors.get.GetFuelConsumedUseCase;
import com.pitstop.interactors.get.GetFuelConsumedUseCaseImpl;
import com.pitstop.interactors.get.GetFuelPricesUseCase;
import com.pitstop.interactors.get.GetFuelPricesUseCaseImpl;
import com.pitstop.interactors.get.GetGooglePlacesShopsUseCase;
import com.pitstop.interactors.get.GetGooglePlacesShopsUseCaseImpl;
import com.pitstop.interactors.get.GetLicensePlateUseCase;
import com.pitstop.interactors.get.GetLicensePlateUseCaseImpl;
import com.pitstop.interactors.get.GetPIDUseCase;
import com.pitstop.interactors.get.GetPIDUseCaseImpl;
import com.pitstop.interactors.get.GetPitstopShopsUseCase;
import com.pitstop.interactors.get.GetPitstopShopsUseCaseImpl;
import com.pitstop.interactors.get.GetPlaceDetailsUseCase;
import com.pitstop.interactors.get.GetPlaceDetailsUseCaseImpl;
import com.pitstop.interactors.get.GetPredictedServiceUseCase;
import com.pitstop.interactors.get.GetPredictedServiceUseCaseImpl;
import com.pitstop.interactors.get.GetReportUseCaseImpl;
import com.pitstop.interactors.get.GetReportsUseCase;
import com.pitstop.interactors.get.GetShopHoursUseCase;
import com.pitstop.interactors.get.GetShopHoursUseCaseImpl;
import com.pitstop.interactors.get.GetSnapToRoadUseCase;
import com.pitstop.interactors.get.GetSnapToRoadUseCaseImpl;
import com.pitstop.interactors.get.GetTripsUseCase;
import com.pitstop.interactors.get.GetTripsUseCaseImpl;
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCase;
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCaseImpl;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.get.GetUserCarUseCaseImpl;
import com.pitstop.interactors.get.GetUserNotificationUseCase;
import com.pitstop.interactors.get.GetUserNotificationUseCaseImpl;
import com.pitstop.interactors.other.ChangePasswordUseCase;
import com.pitstop.interactors.other.ChangePasswordUseCaseImpl;
import com.pitstop.interactors.other.DeviceClockSyncUseCase;
import com.pitstop.interactors.other.DeviceClockSyncUseCaseImpl;
import com.pitstop.interactors.other.DiscoveryTimeoutUseCase;
import com.pitstop.interactors.other.DiscoveryTimeoutUseCaseImpl;
import com.pitstop.interactors.other.FacebookSignUpUseCase;
import com.pitstop.interactors.other.FacebookSignUpUseCaseImpl;
import com.pitstop.interactors.other.HandleVinOnConnectUseCase;
import com.pitstop.interactors.other.HandleVinOnConnectUseCaseImpl;
import com.pitstop.interactors.other.LoginFacebookUseCase;
import com.pitstop.interactors.other.LoginFacebookUseCaseImpl;
import com.pitstop.interactors.other.LoginUseCase;
import com.pitstop.interactors.other.LoginUseCaseImpl;
import com.pitstop.interactors.other.MarkServiceDoneUseCase;
import com.pitstop.interactors.other.MarkServiceDoneUseCaseImpl;
import com.pitstop.interactors.other.ProcessTripDataUseCase;
import com.pitstop.interactors.other.ProcessTripDataUseCaseImpl;
import com.pitstop.interactors.other.RequestServiceUseCase;
import com.pitstop.interactors.other.RequestServiceUseCaseImpl;
import com.pitstop.interactors.other.SendPendingUpdatesUseCase;
import com.pitstop.interactors.other.SendPendingUpdatesUseCaseImpl;
import com.pitstop.interactors.other.SignUpUseCase;
import com.pitstop.interactors.other.SignUpUseCaseImpl;
import com.pitstop.interactors.other.SmoochLoginUseCase;
import com.pitstop.interactors.other.SmoochLoginUseCaseImpl;
import com.pitstop.interactors.other.SortReportsUseCase;
import com.pitstop.interactors.other.SortReportsUseCaseImpl;
import com.pitstop.interactors.other.StartDumpingTripDataWhenConnecteUseCase;
import com.pitstop.interactors.other.StartDumpingTripDataWhenConnectedUseCaseImpl;
import com.pitstop.interactors.other.StoreFuelConsumedUseCase;
import com.pitstop.interactors.other.StoreFuelConsumedUseCaseImpl;
import com.pitstop.interactors.remove.RemoveCarUseCase;
import com.pitstop.interactors.remove.RemoveCarUseCaseImpl;
import com.pitstop.interactors.remove.RemoveShopUseCase;
import com.pitstop.interactors.remove.RemoveShopUseCaseImpl;
import com.pitstop.interactors.remove.RemoveTripUseCase;
import com.pitstop.interactors.remove.RemoveTripUseCaseImpl;
import com.pitstop.interactors.set.SetAlarmsEnabledUseCase;
import com.pitstop.interactors.set.SetAlarmsEnabledUseCaseImpl;
import com.pitstop.interactors.set.SetFirstCarAddedUseCase;
import com.pitstop.interactors.set.SetFirstCarAddedUseCaseImpl;
import com.pitstop.interactors.set.SetNotificationReadUseCase;
import com.pitstop.interactors.set.SetNotificationReadUseCaseImpl;
import com.pitstop.interactors.set.SetServicesDoneUseCase;
import com.pitstop.interactors.set.SetServicesDoneUseCaseImpl;
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
import com.pitstop.repositories.AppointmentRepository;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.ReportRepository;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.SensorDataRepository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.SnapToRoadRepository;
import com.pitstop.repositories.TripRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.retrofit.PitstopSmoochApi;
import com.pitstop.utils.LoginManager;
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
    Post2141UseCase getPost2141UseCase(NetworkHelper networkHelper, @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){

        return new Post2141UseCaseImpl(networkHelper, useCaseHandler, mainHandler);
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
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler") Handler mainHandler){

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
    RemoveShopUseCase removeShopUseCase(ShopRepository shopRepository, CarRepository carRepository
            , UserRepository userRepository, NetworkHelper networkHelper
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler") Handler mainHandler){

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
            , CarRepository carRepository, ShopRepository shopRepository
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler") Handler mainHandler){

        return  new GetCarByCarIdUseCaseImpl(carRepository, userRepository, shopRepository
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
    UpdateShopUseCase updateShopUseCase(ShopRepository shopRepository, UserRepository userRepository
            , CarRepository carRepository, @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){
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
            , @Named("mainHandler") Handler mainHandler){
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
            , @Named("mainHandler") Handler mainHandler){
        return new GetCarsByUserIdUseCaseImpl(userRepository, carRepository
                ,useCaseHandler, mainHandler);
    }

    @Provides
    CheckFirstCarAddedUseCase getCheckFirstCarAddedUseCase(UserRepository userRepository
            , CarRepository carRepository, @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){
        return new CheckFirstCarAddedUseCaseImpl(userRepository, carRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    AddCarUseCase addCarUseCase(CarRepository carRepository, ScannerRepository scannerRepository
            , UserRepository userRepository, @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){

        return new AddCarUseCaseImpl(carRepository, scannerRepository, userRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    GetCurrentServicesUseCase getCurrentServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository, CarRepository carRepository
            , @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new GetCurrentServicesUseCaseImpl(userRepository, carIssueRepository, carRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    GetDoneServicesUseCase getDoneServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository, CarRepository carRepository
            , @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new GetDoneServicesUseCaseImpl(userRepository, carIssueRepository, carRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    GetUpcomingServicesMapUseCase getUpcomingServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository, CarRepository carRepository
            , @Named("useCaseHandler")Handler useCaseHandler
            ,@Named("mainHandler") Handler mainHandler){

        return new GetUpcomingServicesMapUseCaseImpl(userRepository, carIssueRepository
                ,carRepository, useCaseHandler, mainHandler);
    }

    @Provides
    GetUserCarUseCase getUserCarUseCase(UserRepository userRepository, CarRepository carRepository
            , ShopRepository shopRepository, @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){

        return new GetUserCarUseCaseImpl(userRepository,carRepository, shopRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    MarkServiceDoneUseCase markServiceDoneUseCase(CarIssueRepository carIssueRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new MarkServiceDoneUseCaseImpl(carIssueRepository, useCaseHandler, mainHandler);
    }

    @Provides
    RemoveCarUseCase removeCarUseCase(UserRepository userRepository, CarRepository carRepository
            ,TripRepository tripRepository, NetworkHelper networkHelper, @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){

        return new RemoveCarUseCaseImpl(userRepository,carRepository,tripRepository
                ,networkHelper,useCaseHandler, mainHandler);
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
    HandleVinOnConnectUseCase handleVinOnConnectUseCase(ScannerRepository scannerRepository
            , CarRepository carRepository, UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler") Handler mainHandler){

        return new HandleVinOnConnectUseCaseImpl(scannerRepository, carRepository
                , userRepository,  useCaseHandler, mainHandler);
    }

    @Provides
    GetUserNotificationUseCase getUserNotificationUseCase(
            UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler,@Named("mainHandler") Handler mainHandler){

        return new GetUserNotificationUseCaseImpl(userRepository, mainHandler, useCaseHandler);
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
    AddLicensePlateUseCase addLicensePlateUseCase( @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler, LocalSpecsStorage storage){
        return new AddLicensePlateUseCaseImpl( mainHandler, useCaseHandler, storage);
    }

    @Provides
    AddAlarmUseCase addAlarmUseCase(UserRepository userRepository, CarRepository carRepository, LocalAlarmStorage localAlarmStorage,
                                    @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler") Handler mainHandler){
        return new AddAlarmUseCaseImpl(userRepository, carRepository, localAlarmStorage, useCaseHandler, mainHandler);
    }

    @Provides
    GetAlarmsUseCase getAlarmsUseCase(CarRepository carRepository, UserRepository userRepository, LocalAlarmStorage localAlarmStorage,
                                      @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler") Handler mainHandler){
        return new GetAlarmsUseCaseImpl(carRepository, userRepository, localAlarmStorage, useCaseHandler, mainHandler);
    }

    @Provides
    GetLicensePlateUseCase getLicensePlateUseCase( @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler, LocalSpecsStorage storage){
        return new GetLicensePlateUseCaseImpl( mainHandler, useCaseHandler, storage);
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
    SortReportsUseCase sortVehicleHealthReportsUseCase(
            @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){

        return new SortReportsUseCaseImpl(useCaseHandler, mainHandler);
    }

    @Provides
    GetCarStyleIDUseCase getCarStyleIDUseCase( @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler,NetworkHelper networkHelper){
        return new GetCarStyleIDUSeCaseImpl(useCaseHandler, mainHandler, networkHelper);
    }

    @Provides
    GetCarImagesArrayUseCase getCarImagesArrayUseCase (@Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler, NetworkHelper networkHelper){
        return new GetCarImagesArrayUseCaseImpl(useCaseHandler, mainHandler, networkHelper);
    }

    @Provides
    AddDtcUseCase getAddDtcUseCase(UserRepository userRepository, CarIssueRepository carIssueRepository
            , CarRepository carRepository, @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler) {

        return new AddDtcUseCaseImpl(userRepository, carIssueRepository, carRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    GetCarsWithDealershipsUseCase getGetCarsWithDealershipsUseCase(UserRepository userRepository
            , CarRepository carRepository, ShopRepository shopRepository, @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){

        return new GetCarsWithDealershipsUseCaseImpl(userRepository, carRepository, shopRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    CheckNetworkConnectionUseCase checkNetworkConnectionUseCase(NetworkHelper networkHelper
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler") Handler mainHandler){

        return new CheckNetworkConnectionUseCaseImpl(networkHelper, useCaseHandler, mainHandler);
    }

    @Provides
    SetServicesDoneUseCase setServicesDoneUseCase(CarIssueRepository carIssueRepository
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler") Handler mainHandler){

        return new SetServicesDoneUseCaseImpl(carIssueRepository, useCaseHandler, mainHandler);
    }

    @Provides
    SetNotificationReadUseCase setNotificationReadUseCase(@Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){

        return new SetNotificationReadUseCaseImpl(useCaseHandler, mainHandler);
    }

    @Provides
    GetDealershipWithCarIssuesUseCase getDealershipWithCarIssuesUseCase(UserRepository userRepository
            , CarRepository carRepository, CarIssueRepository carIssueRepository
            , ShopRepository shopRepository
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler")Handler mainHandler){

        return new GetDealershipWithCarIssuesUseCaseImpl(userRepository, carRepository
                , carIssueRepository, shopRepository, useCaseHandler, mainHandler);
    }

    @Provides
    GetCurrentCarDealershipUseCase getCurrentCarDealershipUseCase(UserRepository userRepository
            , CarRepository carRepository, ShopRepository shopRepository
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler")Handler mainHandler){

        return new GetCurrentCarDealershipUseCaseImpl(userRepository, carRepository
                , shopRepository, useCaseHandler, mainHandler);
    }
    @Provides
    SetAlarmsEnabledUseCase getSetAlarmsEnableduseCase(UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler")Handler mainHandler){

        return new SetAlarmsEnabledUseCaseImpl(userRepository,useCaseHandler, mainHandler);
    }

    @Provides
    DeviceClockSyncUseCase getDeviceClockSyncUseCase(ScannerRepository scannerRepository
            , UserRepository userRepository, CarRepository carRepository
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler")Handler mainHandler){

        return new DeviceClockSyncUseCaseImpl(scannerRepository, userRepository
                , carRepository, useCaseHandler, mainHandler);
    }

    @Provides
    GetAlarmCountUseCase getAlarmCountUseCase(LocalAlarmStorage localAlarmStorage, UserRepository userRepository
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler")Handler mainHandler){

        return new GetAlarmCountUseCaseImpl(localAlarmStorage, userRepository, useCaseHandler, mainHandler);
    }

    @Provides
    StoreFuelConsumedUseCase getStoreFuelConsumedUseCase(UserRepository userRepository, LocalFuelConsumptionStorage localFuelConsumptionStorage
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler")Handler mainHandler){
        return new StoreFuelConsumedUseCaseImpl(userRepository, mainHandler, useCaseHandler, localFuelConsumptionStorage);
    }

    @Provides
    GetFuelConsumedUseCase getGetFuelConsumedUseCase(LocalFuelConsumptionStorage localFuelConsumptionStorage
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler")Handler mainHandler){
        return new GetFuelConsumedUseCaseImpl(mainHandler, useCaseHandler, localFuelConsumptionStorage);
    }

    @Provides
    GetFuelPricesUseCase getFuelPriceUseCase( @Named("useCaseHandler")Handler useCaseHandler,
                                              @Named("mainHandler")Handler mainHandler,NetworkHelper networkHelper){
        return new GetFuelPricesUseCaseImpl(useCaseHandler, mainHandler, networkHelper);
    }

    @Provides
    GetFuelConsumedAndPriceUseCase getGetFuelConsumedAndPriceUseCase(@Named("useCaseHandler")Handler useCaseHandler,
                                                                     @Named("mainHandler")Handler mainHandler,NetworkHelper networkHelper,
                                                                     LocalFuelConsumptionStorage localFuelConsumptionStorage){
        return new GetFuelConsumedAndPriceUseCaseImpl(useCaseHandler, mainHandler, networkHelper, localFuelConsumptionStorage);
    }

    @Provides
    AddScannerUseCase getAddScannerUseCase(@Named("useCaseHandler")Handler useCaseHandler,
                                           @Named("mainHandler")Handler mainHandler, ScannerRepository scannerRepository){

        return new AddScannerUseCaseImpl(useCaseHandler, mainHandler, scannerRepository);
    }

    @Provides
    GetAllAppointmentsUseCase getAllAppointmentsUseCase(AppointmentRepository appointmentRepository
                    , UserRepository userRepository, @Named("useCaseHandler")Handler useCaseHandler
                    , @Named("mainHandler")Handler mainHandler){

        return new GetAllAppointmentsUseCaseImpl(appointmentRepository, userRepository, useCaseHandler, mainHandler);
    }

    @Provides
    GetPredictedServiceUseCase getPredictedServiceUseCase(UserRepository userRepository
            , AppointmentRepository appointmentRepository, @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler")Handler mainHandler){

        return new GetPredictedServiceUseCaseImpl(userRepository, appointmentRepository, useCaseHandler, mainHandler);
    }

    @Provides
    GetAppointmentStateUseCase getAppointmentStateUseCase(UserRepository userRepository
            , AppointmentRepository appointmentRepository, ShopRepository shopRepository
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler")Handler mainHandler){

        return new GetAppointmentStateUseCaseImpl(userRepository, appointmentRepository
                , shopRepository, useCaseHandler, mainHandler);
    }

    @Provides
    GetTripsUseCase getTripsUseCase(UserRepository userRepository,
                                    CarRepository carRepository,
                                    TripRepository tripRepository,
                                    @Named("useCaseHandler") Handler useCaseHandler,
                                    @Named("mainHandler") Handler mainHandler) {

        return new GetTripsUseCaseImpl(userRepository, carRepository, tripRepository, useCaseHandler, mainHandler);

    }

    @Provides
    GetSnapToRoadUseCase getSnapToRoadUseCase(SnapToRoadRepository snapToRoadRepository,
                                              @Named("useCaseHandler") Handler useCaseHandler,
                                              @Named("mainHandler") Handler mainHandler) {

        return new GetSnapToRoadUseCaseImpl(snapToRoadRepository, useCaseHandler, mainHandler);

    }

    @Provides
    RemoveTripUseCase removeTripUseCase(TripRepository tripRepository,
                                        @Named("useCaseHandler") Handler useCaseHandler,
                                        @Named("mainHandler") Handler mainHandler) {

        return new RemoveTripUseCaseImpl(tripRepository, useCaseHandler, mainHandler);

    }

    @Provides
    SmoochLoginUseCase getSmoochLoginUseCase(PitstopSmoochApi pitstopSmoochApi
            , UserRepository userRepository, CarRepository carRepository
            , @Named("useCaseHandler")Handler useCaseHandler, @Named("mainHandler")Handler mainHandler){

        return new SmoochLoginUseCaseImpl(pitstopSmoochApi, userRepository, carRepository, useCaseHandler, mainHandler);
    }

    @Provides
    StartDumpingTripDataWhenConnecteUseCase startDumpingTripDataWhenConnecteUseCase(
            TripRepository tripRepository, SensorDataRepository sensorDataRepository
            , @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler")Handler mainHandler){

        return new StartDumpingTripDataWhenConnectedUseCaseImpl(tripRepository
                , sensorDataRepository, useCaseHandler,mainHandler);
    }

    @Provides
    AddPidUseCase addPidUseCase(SensorDataRepository sensorDataRepository
            , UserRepository userRepository, CarRepository carRepository
            , @Named("useCaseHandler")Handler useCaseHandler
            , @Named("mainHandler")Handler mainHandler){

        return new AddPidUseCaseImpl(sensorDataRepository, userRepository, carRepository
                , useCaseHandler, mainHandler);
    }

    @Provides
    SendPendingUpdatesUseCase sendPendingUpdatesUseCase(CarRepository carRepository
            , @Named("useCaseHandler") Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){
        return new SendPendingUpdatesUseCaseImpl(carRepository,useCaseHandler,mainHandler);
    }

    @Provides
    ProcessTripDataUseCase processTripDataUseCase(LocalLocationStorage localLocationStorage
            , LocalActivityStorage localActivityStorage
            , TripRepository tripRepository
            , @Named("useCaseHandler") Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){
        return new ProcessTripDataUseCaseImpl(localLocationStorage,localActivityStorage
                ,tripRepository,useCaseHandler,mainHandler);
    }

    @Provides
    LoginUseCase loginUseCase(UserRepository userRepository, LoginManager loginManager
            , LocalDatabaseHelper localDatabaseHelper , @Named("useCaseHandler") Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){
        return new LoginUseCaseImpl(userRepository, loginManager
                , localDatabaseHelper, useCaseHandler, mainHandler);

    }

    @Provides
    SignUpUseCase signUpUseCase(UserRepository userRepository
            , LocalDatabaseHelper localDatabaseHelper, @Named("useCaseHandler") Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){
        return new SignUpUseCaseImpl(userRepository, localDatabaseHelper, useCaseHandler, mainHandler);

    }

    @Provides
    LoginFacebookUseCase loginFacebookUseCase(UserRepository userRepository, LoginManager loginManager
            , LocalDatabaseHelper localDatabaseHelper, @Named("useCaseHandler") Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){
        return new LoginFacebookUseCaseImpl(loginManager, userRepository , localDatabaseHelper
                , useCaseHandler, mainHandler);

    }

    @Provides
    FacebookSignUpUseCase facebookSignUpUseCase(UserRepository userRepository
            , LocalDatabaseHelper localDatabaseHelper, @Named("useCaseHandler") Handler useCaseHandler
            , @Named("mainHandler") Handler mainHandler){
        return new FacebookSignUpUseCaseImpl(userRepository , localDatabaseHelper
                , useCaseHandler, mainHandler);

    }

    @Provides
    FacebookLoginAuthMacroUseCase facebookLoginAuthMacroUseCase(LoginFacebookUseCase loginFacebookUseCase
            , SmoochLoginUseCase smoochLoginUseCase
            , @Named("mainHandler") Handler mainHandler){
        return new FacebookLoginAuthMacroUseCaseImpl(loginFacebookUseCase, smoochLoginUseCase, mainHandler);
    }

    @Provides
    FacebookSignUpAuthMacroUseCase facebookSignUpAuthMacroUseCase(FacebookSignUpUseCase facebookSignUpUseCase
            , LoginFacebookUseCase loginFacebookUseCase, SmoochLoginUseCase smoochLoginUseCase
            , @Named("mainHandler") Handler mainHandler){
        return new FacebookSignUpAuthMacroUseCaseImpl(facebookSignUpUseCase, loginFacebookUseCase, smoochLoginUseCase, mainHandler);
    }

    @Provides
    SignUpAuthMacroUseCase signUpAuthMacroUseCase(SignUpUseCase signUpUseCase
            , LoginUseCase loginUseCase, SmoochLoginUseCase smoochLoginUseCase
            , @Named("mainHandler") Handler mainHandler){
        return new SignUpAuthMacroUseCaseImpl(signUpUseCase, loginUseCase, smoochLoginUseCase, mainHandler);
    }

    @Provides
    LoginAuthMacroUseCase loginAuthMacroUseCase(LoginUseCase loginUseCase
            , SmoochLoginUseCase smoochLoginUseCase
            , @Named("mainHandler") Handler mainHandler){
        return new LoginAuthMacroUseCaseImpl(loginUseCase, smoochLoginUseCase, mainHandler);
    }

    @Provides
    ChangePasswordUseCase changePasswordUseCase(UserRepository userRepository
            , @Named("useCaseHandler") Handler useCaseHandler, @Named("mainHandler") Handler mainHandler){
        return new ChangePasswordUseCaseImpl(userRepository, useCaseHandler, mainHandler);
    }
}

