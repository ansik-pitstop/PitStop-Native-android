package com.pitstop.dependency;

import com.pitstop.interactors.AddCarUseCase;
import com.pitstop.interactors.AddCarUseCaseImpl;
import com.pitstop.interactors.GetCurrentServicesUseCase;
import com.pitstop.interactors.GetCurrentServicesUseCaseImpl;
import com.pitstop.interactors.GetDoneServicesUseCase;
import com.pitstop.interactors.GetDoneServicesUseCaseImpl;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.interactors.GetUserCarUseCaseImpl;
import com.pitstop.interactors.MarkServiceDoneUseCase;
import com.pitstop.interactors.MarkServiceDoneUseCaseImpl;
import com.pitstop.interactors.RemoveCarUseCase;
import com.pitstop.interactors.RemoveCarUseCaseImpl;
import com.pitstop.interactors.RequestServiceUseCase;
import com.pitstop.interactors.RequestServiceUseCaseImpl;
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

@Module(includes = RepositoryModule.class)
public class UseCaseModule {

    @Provides
    AddCarUseCase addCarUseCase(CarRepository carRepository){
        return new AddCarUseCaseImpl(carRepository);
    }

    @Provides
    GetCurrentServicesUseCase getCurrentServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository){

        return new GetCurrentServicesUseCaseImpl(userRepository, carIssueRepository);
    }

    @Provides
    GetDoneServicesUseCase getDoneServicesUseCase(UserRepository userRepository
            , CarIssueRepository carIssueRepository){

        return new GetDoneServicesUseCaseImpl(userRepository, carIssueRepository);
    }

    @Provides
    GetUserCarUseCase getUserCarUseCase(UserRepository userRepository){
        return new GetUserCarUseCaseImpl(userRepository);
    }

    @Provides
    MarkServiceDoneUseCase markServiceDoneUseCase(CarIssueRepository carIssueRepository){
        return new MarkServiceDoneUseCaseImpl(carIssueRepository);
    }

    @Provides
    RemoveCarUseCase removeCarUseCase(CarRepository carRepository){
        return new RemoveCarUseCaseImpl(carRepository);
    }

    @Provides
    RequestServiceUseCase requestServiceUseCase(CarIssueRepository carIssueRepository){
        return new RequestServiceUseCaseImpl(carIssueRepository);
    }

    @Provides
    SetUserCarUseCase setUseCarUseCase(UserRepository userRepository){
        return new SetUserCarUseCaseImpl(userRepository);
    }

}
