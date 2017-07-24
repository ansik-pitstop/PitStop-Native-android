package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Settings;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public class GetCurrentServicesUseCaseImpl implements GetCurrentServicesUseCase {
    private UserRepository userRepository;
    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private Handler handler;

    public GetCurrentServicesUseCaseImpl(UserRepository userRepository
            , CarIssueRepository carIssueRepository, Handler handler) {

        this.handler = handler;
        this.userRepository = userRepository;
        this.carIssueRepository = carIssueRepository;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {

        //Get current users car
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {

                if (!data.hasMainCar()){
                    callback.onGotCurrentServices(new ArrayList<>());
                    return;
                }

                carIssueRepository.getCurrentCarIssues(data.getCarId(), new CarIssueRepository.CarIssueGetCurrentCallback() {
                    @Override
                    public void onCarIssueGotCurrent(List<CarIssue> carIssueCurrent) {
                        callback.onGotCurrentServices(carIssueCurrent);
                    }

                    @Override
                    public void onError() {
                        callback.onError();
                    }
                });
            }

            @Override
            public void onError(int error) {
                callback.onError();
            }
        });
    }
}
