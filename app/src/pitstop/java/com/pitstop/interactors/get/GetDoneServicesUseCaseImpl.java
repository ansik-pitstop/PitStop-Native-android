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

public class GetDoneServicesUseCaseImpl implements GetDoneServicesUseCase {
    private UserRepository userRepository;
    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private Handler handler;

    public GetDoneServicesUseCaseImpl(UserRepository userRepository
            , CarIssueRepository carIssueRepository, Handler handler) {
        this.userRepository = userRepository;
        this.carIssueRepository = carIssueRepository;
        this.handler = handler;
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
                    callback.onGotDoneServices(new ArrayList<CarIssue>());
                    return;
                }

                //Use the current users car to get all the current issues
                carIssueRepository.getDoneCarIssues(data.getCarId()
                        , new CarIssueRepository.CarIssueGetDoneCallback() {

                            @Override
                            public void onCarIssueGotDone(List<CarIssue> carIssueDone) {
                                callback.onGotDoneServices(carIssueDone);
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
