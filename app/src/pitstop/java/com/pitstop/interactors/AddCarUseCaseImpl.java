package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.models.Car;
import com.pitstop.repositories.CarRepository;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class AddCarUseCaseImpl implements AddCarUseCase {

    private CarRepository carRepository;
    private Handler handler;
    private Car car;
    private Callback callback;

    private EventSource eventSource;

    public AddCarUseCaseImpl(CarRepository carRepository, Handler handler){
        this.carRepository = carRepository;
        this.handler = handler;
    }

    @Override
    public void execute(Car car,String eventSource, Callback callback) {
        this.eventSource = new EventSourceImpl(eventSource);
        this.car = car;
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {
        carRepository.insert(car, new CarRepository.CarInsertCallback() {
            @Override
            public void onCarAdded() {
                EventType eventType = new EventTypeImpl(EventType.EVENT_CAR_ID);
                EventBus.getDefault().post(new CarDataChangedEvent(eventType
                        ,eventSource));

                callback.onCarAdded();
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }
}
