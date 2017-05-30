package com.pitstop.interactors;

import com.pitstop.database.LocalCarAdapter;
import com.pitstop.models.Car;
import com.pitstop.repositories.CarRepository;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class AddCarUseCaseImpl implements AddCarUseCase {

    private LocalCarAdapter localCarAdapter;
    private NetworkHelper networkHelper;
    private Car car;
    private Callback callback;

    public AddCarUseCaseImpl(LocalCarAdapter localCarAdapter, NetworkHelper networkHelper){
        this.localCarAdapter = localCarAdapter;
        this.networkHelper = networkHelper;
    }

    @Override
    public void execute(Car car, Callback callback) {
        this.car = car;
        this.callback = callback;
        new Thread(this).start();
    }

    @Override
    public void run() {
        CarRepository.getInstance(localCarAdapter,networkHelper).insert(car, new CarRepository.CarInsertCallback() {
            @Override
            public void onCarAdded() {
                callback.onCarAdded();
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }
}
