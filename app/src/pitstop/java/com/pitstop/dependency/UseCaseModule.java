package com.pitstop.dependency;

import android.os.Handler;

import com.pitstop.interactors.AddCarUseCase;
import com.pitstop.interactors.AddCarUseCaseImpl;
import com.pitstop.interactors.CheckFirstCarAddedUseCase;
import com.pitstop.interactors.CheckFirstCarAddedUseCaseImpl;
import com.pitstop.interactors.GetCarsByUserIdUseCase;
import com.pitstop.interactors.GetCarsByUserIdUseCaseImpl;
import com.pitstop.interactors.GetCurrentServicesUseCase;
import com.pitstop.interactors.GetCurrentServicesUseCaseImpl;
import com.pitstop.interactors.GetDoneServicesUseCase;
import com.pitstop.interactors.GetDoneServicesUseCaseImpl;
import com.pitstop.interactors.GetUpcomingServicesMapUseCase;
import com.pitstop.interactors.GetUpcomingServicesMapUseCaseImpl;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.interactors.GetUserCarUseCaseImpl;
import com.pitstop.interactors.MarkServiceDoneUseCase;
import com.pitstop.interactors.MarkServiceDoneUseCaseImpl;
import com.pitstop.interactors.RemoveCarUseCase;
import com.pitstop.interactors.RemoveCarUseCaseImpl;
import com.pitstop.interactors.RequestServiceUseCase;
import com.pitstop.interactors.RequestServiceUseCaseImpl;
import com.pitstop.interactors.SetFirstCarAddedUseCase;
import com.pitstop.interactors.SetFirstCarAddedUseCaseImpl;
import com.pitstop.interactors.SetUserCarUseCase;
import com.pitstop.interactors.SetUserCarUseCaseImpl;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

@Module(includes = {RepositoryModule.class, HandlerModule.class} )
public class UseCaseModule {

    @Provides
    GetCarsByUserIdUseCase getCarsByUserIdUseCase(UserRepository userRepository
            , CarRepository carRepository, Handler handler){
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
    GetUserCarUseCase getUserCarUseCase(UserRepository userRepository, Handler handler){
        return new GetUserCarUseCaseImpl(userRepository, handler);
    }

    @Provides
    MarkServiceDoneUseCase markServiceDoneUseCase(CarIssueRepository carIssueRepository
            , Handler handler){

        return new MarkServiceDoneUseCaseImpl(carIssueRepository, handler);
    }

    @Provides
    RemoveCarUseCase removeCarUseCase(CarRepository carRepository, Handler handler){
        return new RemoveCarUseCaseImpl(carRepository,handler);
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
}
