package com.pitstop.repositories;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.pitstop.database.LocalCarStorage;
import com.pitstop.retrofit.PitstopAuthApi;
import com.pitstop.utils.NetworkHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by Karol Zdebel on 5/29/2018.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CarRepositoryTest {

    private final String TAG = CarRepositoryTest.class.getSimpleName();
    private CarRepository carRepository;

    @Before
    public void setup(){
        Context context = InstrumentationRegistry.getTargetContext();
        LocalCarStorage localCarStorage = new LocalCarStorage(context);
        PitstopAuthApi pitstopAuthApi = RetrofitTestUtil.Companion.getAuthApi();
        NetworkHelper networkHelper = new NetworkHelper(context,pitstopAuthApi, PreferenceManager.getDefaultSharedPreferences(context));
        carRepository = new CarRepository(localCarStorage,networkHelper
                ,RetrofitTestUtil.Companion.getCarApi());
    }

    @Test
    public void sendPendingUpdatesTest(){

    }

}
