package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.models.issue.UpcomingIssue;
import com.pitstop.models.service.UpcomingService;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public class GetUpcomingServicesUseCaseImpl implements GetUpcomingServicesUseCase {
    private UserRepository userRepository;
    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private Handler handler;

    public GetUpcomingServicesUseCaseImpl(UserRepository userRepository
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
        userRepository.getUserCar(new UserRepository.UserGetCarCallback() {
            @Override
            public void onGotCar(Car car) {

                //Use the current users car to get all the current issues
                carIssueRepository.getUpcomingCarIssues(car.getId()
                        , new CarIssueRepository.CarIssueGetUpcomingCallback() {

                            @Override
                            public void onCarIssueGotUpcoming(List<UpcomingIssue> carIssueUpcoming) {
                                //Return ordered upcoming services through parameter to callback
                                callback.onGotUpcomingServices(
                                        getUpcomingServicesOrdered(carIssueUpcoming));
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

    private List<UpcomingService> getUpcomingServicesOrdered(List<UpcomingIssue> carIssueUpcoming){

        List<UpcomingService> upcomingServices = new ArrayList<>();

        for (UpcomingIssue i: carIssueUpcoming){
            upcomingServices.add(new UpcomingService(i));
        }

        Collections.sort(upcomingServices, new Comparator<UpcomingService>() {
            @Override
            public int compare(UpcomingService leftService, UpcomingService rightService) {
                return rightService.getMileage() - leftService.getMileage();
            }
        });

        return upcomingServices;
    }
}
