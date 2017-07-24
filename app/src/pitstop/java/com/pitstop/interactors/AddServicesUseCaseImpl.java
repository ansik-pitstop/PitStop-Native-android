package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Settings;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

import java.util.List;

/**
 * Created by Matthew on 2017-07-17.
 */

public class AddServicesUseCaseImpl implements AddServicesUseCase {

    private CarIssueRepository carIssueRepository;
    private UserRepository userRepository;
    private Callback callback;
    private List<CarIssue> carIssues;
    private Handler handler;

    public AddServicesUseCaseImpl(CarIssueRepository carIssueRepository, UserRepository userRepository, Handler handler) {
        this.carIssueRepository = carIssueRepository;
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void run() {
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {
                carIssueRepository.insert(data.getCarId(),carIssues,new CarIssueRepository.CarIssueInsertCallback(){

                    @Override
                    public void onCarIssueAdded() {
                        callback.onServicesAdded();
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

    @Override
    public void execute(List<CarIssue> carIssues, Callback callback) {
        this.callback = callback;
        this.carIssues = carIssues;
        handler.post(this);
    }
}
