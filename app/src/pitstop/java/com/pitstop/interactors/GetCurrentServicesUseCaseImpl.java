package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.UserRepository;

import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public class GetCurrentServicesUseCaseImpl implements GetCurrentServicesUseCase {
    private UserRepository userRepository;
    private CarIssueRepository carIssueRepository;
    private Callback callback;

    public GetCurrentServicesUseCaseImpl(UserRepository userRepository
            , CarIssueRepository carIssueRepository) {

        this.userRepository = userRepository;
        this.carIssueRepository = carIssueRepository;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        new Handler().post(this);
    }

    @Override
    public void run() {

        //Get current users car
        userRepository.getUserCar(new UserRepository.UserGetCarCallback() {
            @Override
            public void onGotCar(Car car) {
                //Use the current users car to get all the current issues
                        carIssueRepository.getCurrentCarIssues(car.getId(), new CarIssueRepository.CarIssueGetCurrentCallback() {
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
            public void onError() {
                callback.onError();
            }
        });

    }
}
