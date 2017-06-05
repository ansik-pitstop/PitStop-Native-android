package com.pitstop.dependency;

import com.pitstop.interactors.AddCarUseCase;
import com.pitstop.interactors.GetCurrentServicesUseCase;
import com.pitstop.interactors.GetDoneServicesUseCase;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.interactors.MarkServiceDoneUseCase;
import com.pitstop.interactors.RemoveCarUseCase;
import com.pitstop.interactors.RequestServiceUseCase;
import com.pitstop.interactors.SetUserCarUseCase;
import com.pitstop.ui.services.CurrentServicesFragment;

import dagger.Component;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

@ApplicationScope
@Component(modules = UseCaseModule.class)
public interface UseCaseComponent {

//    //These should be both changed to presenters down the road, once they're implemented
    void injectUseCases(CurrentServicesFragment fragment);


    AddCarUseCase addCarUseCase();

    GetCurrentServicesUseCase getCurrentServicesUseCase();

    GetDoneServicesUseCase getDoneServicesUseCase();

    GetUserCarUseCase getUserCarUseCase();

    MarkServiceDoneUseCase markServiceDoneUseCase();

    RemoveCarUseCase removeCarUseCase();

    RequestServiceUseCase requestServiceUseCase();

    SetUserCarUseCase setUseCarUseCase();

}
