package com.pitstop.dependency;

import android.os.Handler;

import com.pitstop.interactors.add.AddCarUseCase;
import com.pitstop.interactors.add.AddCarUseCaseImpl;
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
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCase;
import com.pitstop.interactors.get.GetUpcomingServicesMapUseCaseImpl;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.get.GetUserCarUseCaseImpl;
import com.pitstop.interactors.get.GetUserShopsUseCase;
import com.pitstop.interactors.get.GetUserShopsUseCaseImpl;
import com.pitstop.interactors.update.MarkServiceDoneUseCase;
import com.pitstop.interactors.update.MarkServiceDoneUseCaseImpl;
import com.pitstop.interactors.remove.RemoveCarUseCase;
import com.pitstop.interactors.remove.RemoveCarUseCaseImpl;
import com.pitstop.interactors.remove.RemoveShopUseCase;
import com.pitstop.interactors.remove.RemoveShopUseCaseImpl;
import com.pitstop.interactors.add.AddServiceUseCase;
import com.pitstop.interactors.add.AddServiceUseCaseImpl;
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
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;

import dagger.Module;
import dagger.Provides;
import com.pitstop.utils.NetworkHelper;

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
    AddServiceUseCase requestServiceUseCase(CarIssueRepository carIssueRepository
            , Handler handler){
        return new AddServiceUseCaseImpl(carIssueRepository, handler);
    }

    @Provides
    SetUserCarUseCase setUseCarUseCase(UserRepository userRepository, Handler handler){
        return new SetUserCarUseCaseImpl(userRepository, handler);
    }

    @Provides
    SetFirstCarAddedUseCase setFirstCarAddedUseCase(UserRepository userRepository){
        return new SetFirstCarAddedUseCaseImpl(userRepository);
    }
}
