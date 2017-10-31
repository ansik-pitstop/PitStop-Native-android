package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Dealership;
import com.pitstop.models.ObdScanner;
import com.pitstop.models.Settings;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.models.Car;
import com.pitstop.utils.ModelConverter;

/**
 * Created by Matthew on 2017-06-20.
 */

public class GetCarByCarIdUseCaseImpl implements GetCarByCarIdUseCase {
    private CarRepository carRepository;
    private UserRepository userRepository;
    private ShopRepository shopRepository;
    private ScannerRepository scannerRepository;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private int carId;

    public GetCarByCarIdUseCaseImpl(CarRepository carRepository, UserRepository userRepository
            , ShopRepository shopRepository, ScannerRepository scannerRepository
            , Handler useCaseHandler, Handler mainHandler) {
        this.useCaseHandler = useCaseHandler;
        this.carRepository = carRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.scannerRepository = scannerRepository;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(int carId,Callback callback) {
        this.callback = callback;
        this.carId = carId;
        useCaseHandler.post(this);
    }

    private void onCarGot(Car car, Dealership dealership){
        mainHandler.post(() -> callback.onCarGot(car, dealership));
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
    };

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {

            @Override
            public void onSuccess(User user) {
                carRepository.get(carId)
                        .doOnError(err ->  GetCarByCarIdUseCaseImpl.this.onError(RequestError.getUnknownError()))
                        .subscribe(carResponse ->
                                carRepository.getShopId(carResponse.getResponse().get_id())
                                        .doOnError(err ->  GetCarByCarIdUseCaseImpl.this.onError(RequestError.getUnknownError()))
                                        .subscribe(shopIdResponse -> shopRepository.get(shopIdResponse.getResponse()
                                                , new Repository.Callback<Dealership>() {

                                            @Override
                                            public void onSuccess(Dealership dealership) {
                                                scannerRepository.getScanner(carResponse.getResponse().get_id()
                                                        , new Repository.Callback<ObdScanner>() {

                                                    @Override
                                                    public void onSuccess(ObdScanner scanner) {
                                                        String scannerId = scanner == null? null: scanner.getScannerId();
                                                        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
                                                            @Override
                                                            public void onSuccess(Settings settings) {
                                                                Car car = new ModelConverter()
                                                                        .generateCar(carResponse.getResponse(), settings.getCarId(), scannerId, dealership.getId()))
                                                                GetCarByCarIdUseCaseImpl.this.onCarGot(car, dealership);
                                                            }

                                                            @Override
                                                            public void onError(RequestError error) {
                                                                GetCarByCarIdUseCaseImpl.this.onError(error);
                                                            }
                                                        });


                                                    }

                                                    @Override
                                                    public void onError(RequestError error) {
                                                        GetCarByCarIdUseCaseImpl.this.onError(error);
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onError(RequestError error) {
                                                GetCarByCarIdUseCaseImpl.this.onError(error);
                                            }
                                        }));
                                });


            @Override
            public void onError(RequestError error) {
                GetCarByCarIdUseCaseImpl.this.onError(error);
            }
        });
    }
}
