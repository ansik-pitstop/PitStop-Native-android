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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public class GetUpcomingServicesMapUseCaseImpl implements GetUpcomingServicesMapUseCase {

    private final int MAX_DEBUG_ISSUE_SIZE = 500;

    private UserRepository userRepository;
    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private Handler handler;

    public GetUpcomingServicesMapUseCaseImpl(UserRepository userRepository
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
                                List<UpcomingService> list = getUpcomingServicesOrdered(carIssueUpcoming);
                                Map<Integer,List<UpcomingService>> map = getUpcomingServiceMileageMap(list);

                                callback.onGotUpcomingServicesMap(map);
                            }

                            @Override
                            public void onError() {
                                callback.onError();
                            }

                        });
            }

            @Override
            public void onNoCarSet() {
                Map<Integer,List<UpcomingService>> map = new LinkedHashMap<Integer, List<UpcomingService>>();
                callback.onGotUpcomingServicesMap(map);
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
                return leftService.getMileage() - rightService.getMileage();
            }
        });

        return upcomingServices;
    }

    private Map<Integer,List<UpcomingService>> getUpcomingServiceMileageMap(
            List<UpcomingService> upcomingServices){

        Map<Integer,List<UpcomingService>> map = new LinkedHashMap<>();

        List<UpcomingService> services;

        for (UpcomingService u: upcomingServices){
            if (map.containsKey(u.getMileage())){
                services = map.get(u.getMileage());
            }
            else{
                services = new ArrayList<>();
            }

            services.add(u);
            map.put(u.getMileage(),services);
        }

        return map;
    }
}
