package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.models.issue.UpcomingIssue;
import com.pitstop.models.service.UpcomingService;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

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

    private final String TAG = getClass().getSimpleName();
    private final int MAX_DEBUG_ISSUE_SIZE = 500;

    private UserRepository userRepository;
    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;

    public GetUpcomingServicesMapUseCaseImpl(UserRepository userRepository
            , CarIssueRepository carIssueRepository, Handler useCaseHandler, Handler mainHandler) {
        this.userRepository = userRepository;
        this.carIssueRepository = carIssueRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Callback callback) {
        Logger.getInstance().logI(TAG, "Use case started execution"
                , false, DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onGotUpcomingServicesMap(Map<Integer,List<UpcomingService>> serviceMap){
        Logger.getInstance().logI(TAG, "Use case finished: serviceMap="+serviceMap
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onGotUpcomingServicesMap(serviceMap));
    }

    private void onNoCarAdded(){
        Logger.getInstance().logI(TAG, "Use case finished: car not added!"
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onNoCarAdded());
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {

        //Get current users car
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {

                if (!data.hasMainCar()){
                    GetUpcomingServicesMapUseCaseImpl.this.onNoCarAdded();
                    return;
                }

                //Use the current users car to get all the current issues
                carIssueRepository.getUpcomingCarIssues(data.getCarId()
                        , new CarIssueRepository.Callback<List<UpcomingIssue>>() {

                            @Override
                            public void onSuccess(List<UpcomingIssue> carIssueUpcoming) {

                                //Return ordered upcoming services through parameter to callback
                                List<UpcomingService> list = getUpcomingServicesOrdered(carIssueUpcoming);
                                Map<Integer,List<UpcomingService>> map = getUpcomingServiceMileageMap(list);

                                GetUpcomingServicesMapUseCaseImpl.this.onGotUpcomingServicesMap(map);
                            }

                            @Override
                            public void onError(RequestError error) {
                                GetUpcomingServicesMapUseCaseImpl.this.onError(error);
                            }

                        });
            }

            @Override
            public void onError(RequestError error) {
                GetUpcomingServicesMapUseCaseImpl.this.onError(error);
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
