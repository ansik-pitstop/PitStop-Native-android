package com.pitstop.interactors;

import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.test.mock.MockContext;

import com.pitstop.interactors.add.AddTripUseCase;
import com.pitstop.interactors.add.AddTripUseCaseImpl;
import com.pitstop.models.Car;
import com.pitstop.models.Settings;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.TripRepository;
import com.pitstop.repositories.UserRepository;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.reactivex.Observable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Created by Karol Zdebel on 3/16/2018.
 */

public class AddTripUseCaseTest {

    private static final String PROVIDER = "flp";

    @Test
    public void addTripUseCaseTest(){
        System.out.println("starting addTripUseCaseTest");

        Car dummyCar = new Car();
        dummyCar.setVin("1GB0CVCL7BF147611");
        dummyCar.setId(6014);

        //Mock repositories
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        doAnswer(invocation -> {
            Repository.Callback<Settings> callback = invocation.getArgument(0);
            callback.onSuccess(new Settings(1559,6014,true,true));
            return null;
        }).when(userRepository).getCurrentUserSettings(any(Repository.Callback.class));
        CarRepository carRepository = Mockito.mock(CarRepository.class);
        Mockito.when(carRepository.get(1559)).thenReturn(Observable.just(new RepositoryResponse<>(dummyCar,false)));
        TripRepository tripRepository = Mockito.mock(TripRepository.class);

        //Mock location objects
        List<Location> trip = new ArrayList<>();
        for (int i=0;i<10;i++){
            trip.add(getRandomLocation());
        }

        System.out.println("addTripUseCaseTest: trip = "+trip);

        //Mock geocoder
        Geocoder geocoder = new Geocoder(new MockContext());
        AddTripUseCaseImpl addTripUseCase = new AddTripUseCaseImpl(geocoder,tripRepository
                ,userRepository,carRepository,new Handler(), new Handler());
        addTripUseCase.execute(trip, new AddTripUseCase.Callback() {
            @Override
            public void onAddedTrip() {
                System.out.println("addTripUseCaseTest: onAddedTrip()");
            }

            @Override
            public void onError(@NotNull RequestError err) {
                System.out.println("addTripUseCaseTest: onError()");
            }
        });
    }

    private Location getRandomLocation(){
        Random r = new Random();
        Location location = new Location(PROVIDER);
        location.setLatitude(r.nextDouble()*100);
        location.setLongitude(r.nextDouble()*100);
        location.setAccuracy(3.0f);
        location.setTime(1521200114 + Math.abs(r.nextInt()*1000000));
        return location;
    }
}
