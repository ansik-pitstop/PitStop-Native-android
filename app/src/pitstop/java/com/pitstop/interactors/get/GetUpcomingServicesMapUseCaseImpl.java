package com.pitstop.interactors.get;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.models.issue.UpcomingIssue;
import com.pitstop.models.service.UpcomingService;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public class GetUpcomingServicesMapUseCaseImpl implements GetUpcomingServicesMapUseCase {

    private final String TAG = getClass().getSimpleName();
    private final int MAX_DEBUG_ISSUE_SIZE = 500;

    private UserRepository userRepository;
    private CarIssueRepository carIssueRepository;
    private CarRepository carRepository;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Integer userId;

    public GetUpcomingServicesMapUseCaseImpl(UserRepository userRepository
            , CarIssueRepository carIssueRepository, CarRepository carRepository
            , Handler useCaseHandler, Handler mainHandler) {
        this.userRepository = userRepository;
        this.carIssueRepository = carIssueRepository;
        this.carRepository = carRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Integer userId, Callback callback) {
        Logger.getInstance().logI(TAG, "Use case started execution"
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        this.callback = callback;
        this.userId = userId;
        useCaseHandler.post(this);
    }

    private void onGotUpcomingServicesMap(Map<Integer,List<UpcomingService>> serviceMap, boolean local){
        Logger.getInstance().logI(TAG, "Use case finished: serviceMap="+serviceMap
                , DebugMessage.TYPE_USE_CASE);
        if (!local) compositeDisposable.clear();
        mainHandler.post(() -> callback.onGotUpcomingServicesMap(serviceMap,local));
    }

    private void onNoCarAdded(){
        Logger.getInstance().logI(TAG, "Use case finished: car not added!"
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onNoCarAdded());
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        Disposable disposable = carRepository.getCarsByUserId(userId, Repository.DATABASE_TYPE.REMOTE)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io(),true)
                .subscribe(next -> {
                    //Ignore local responses
                    if (next.isLocal()){}
                    else if (next.getData() != null && next.getData().size() > 0){
                        getUpcomingCarIssues(next.getData().get(0).getId());
                    }else{
                        GetUpcomingServicesMapUseCaseImpl.this.onNoCarAdded();
                    }
                },error -> {
                    error.printStackTrace();
                    GetUpcomingServicesMapUseCaseImpl.this
                            .onError(new RequestError(error));
                });
        compositeDisposable.add(disposable);

    }

    private void getUpcomingCarIssues(int carId){
        //Use the current users car to get all the current issues
        Disposable disposable = carIssueRepository.getUpcomingCarIssues(carId, Repository.DATABASE_TYPE.REMOTE)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io(), true)
                .subscribe(next -> {
                    if (next.getData() == null) return;
                    //Return ordered upcoming services through parameter to callback
                    List<UpcomingService> list = getUpcomingServicesOrdered(next.getData());
                    Map<Integer,List<UpcomingService>> map = getUpcomingServiceMileageMap(list);

                    GetUpcomingServicesMapUseCaseImpl.this.onGotUpcomingServicesMap(map,next.isLocal());
                }, error -> {
                    Log.d(TAG,"error: "+error);
                    error.printStackTrace();
                    GetUpcomingServicesMapUseCaseImpl.this.onError(new RequestError(error));

                });
        compositeDisposable.add(disposable);
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
