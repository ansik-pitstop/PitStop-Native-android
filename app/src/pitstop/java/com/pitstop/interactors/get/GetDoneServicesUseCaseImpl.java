package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Settings;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public class GetDoneServicesUseCaseImpl implements GetDoneServicesUseCase {
    private UserRepository userRepository;
    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;

    public GetDoneServicesUseCaseImpl(UserRepository userRepository
            , CarIssueRepository carIssueRepository, Handler useCaseHandler, Handler mainHandler) {
        this.userRepository = userRepository;
        this.carIssueRepository = carIssueRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onGotDoneServices(List<CarIssue> doneServices){
        mainHandler.post(() -> callback.onGotDoneServices(doneServices));
    }

    private void onNoCarAdded(){
        mainHandler.post(() -> callback.onNoCarAdded());
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {

        //Get current users car
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {

                if (!data.hasMainCar()) {
                    GetDoneServicesUseCaseImpl.this.onNoCarAdded();
                    return;
                }

                //Use the current users car to get all the current issues
                carIssueRepository.getDoneCarIssues(data.getCarId()
                        , new CarIssueRepository.Callback<List<CarIssue>>() {

                            @Override
                            public void onSuccess(List<CarIssue> carIssueDone) {
                                GetDoneServicesUseCaseImpl.this.onGotDoneServices(carIssueDone);
                            }

                            @Override
                            public void onError(RequestError error) {
                                GetDoneServicesUseCaseImpl.this.onError(error);
                            }
                        });
            }

            @Override
            public void onError(RequestError error) {
                GetDoneServicesUseCaseImpl.this.onError(error);
            }
        });
    }
}
