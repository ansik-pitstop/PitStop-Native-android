package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.UserAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public class GetCurrentServicesUseCaseImpl implements GetCurrentServicesUseCase {
    private LocalCarIssueAdapter localCarIssueAdapter;
    private UserAdapter userAdapter;
    private NetworkHelper networkHelper;
    private Callback callback;

    public GetCurrentServicesUseCaseImpl(UserAdapter userAdapter
            , LocalCarIssueAdapter localCarIssueAdapter, NetworkHelper networkHelper) {
        this.userAdapter = userAdapter;
        this.localCarIssueAdapter = localCarIssueAdapter;
        this.networkHelper = networkHelper;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        new Handler().post(this);
    }

    @Override
    public void run() {

        //Get current users car
        UserRepository.getInstance(userAdapter,networkHelper).getUserCar(new UserRepository.UserGetCarCallback() {
            @Override
            public void onGotCar(Car car) {

                //Use the current users car to get all the current issues
                CarIssueRepository.getInstance(localCarIssueAdapter,networkHelper)
                        .getCurrentCarIssues(car.getId(), new CarIssueRepository.CarIssueGetCurrentCallback() {
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
