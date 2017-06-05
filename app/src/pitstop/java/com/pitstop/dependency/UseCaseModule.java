package com.pitstop.dependency;

import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.UserAdapter;
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
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

public class UseCaseModule {

    AddCarUseCase addCarUseCase(LocalCarAdapter localCarAdapter, NetworkHelper networkHelper){
        return new AddCarUseCaseImpl(localCarAdapter, networkHelper);
    }

    GetCurrentServicesUseCase getCurrentServicesUseCase(UserAdapter userAdapter
            , LocalCarIssueAdapter localCarIssueAdapter, NetworkHelper networkHelper){

        return new GetCurrentServicesUseCaseImpl(userAdapter, localCarIssueAdapter, networkHelper);
    }

    GetDoneServicesUseCase getDoneServicesUseCase(UserAdapter userAdapter
            , LocalCarIssueAdapter localCarIssueAdapter, NetworkHelper networkHelper){

        return new GetDoneServicesUseCaseImpl(userAdapter,localCarIssueAdapter,networkHelper);
    }

    GetUserCarUseCase getUserCarUseCase(UserAdapter userAdapter, NetworkHelper networkHelper){

        return new GetUserCarUseCaseImpl(userAdapter, networkHelper);
    }

    MarkServiceDoneUseCase markServiceDoneUseCase(LocalCarIssueAdapter localCarIssueAdapter
            , NetworkHelper networkHelper){

        return new MarkServiceDoneUseCaseImpl(localCarIssueAdapter, networkHelper);
    }

    RemoveCarUseCase removeCarUseCase(LocalCarAdapter localCarAdapter, NetworkHelper networkHelper){
        return new RemoveCarUseCaseImpl(localCarAdapter,networkHelper);
    }

    RequestServiceUseCase requestServiceUseCase(LocalCarIssueAdapter localCarIssueAdapter
            , NetworkHelper networkHelper){

        return new RequestServiceUseCaseImpl(localCarIssueAdapter, networkHelper);
    }

    SetUserCarUseCase setUseCarUseCase(UserAdapter userAdapter, NetworkHelper networkHelper){
        return new SetUserCarUseCaseImpl(userAdapter, networkHelper);
    }

}
