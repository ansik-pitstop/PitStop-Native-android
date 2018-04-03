package com.pitstop.interactors.remove;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.utils.NetworkHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by David C. on 2/4/18.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RemoveTripUseCaseTest {

    private final String TAG = RemoveTripUseCaseTest.class.getSimpleName();
    private UseCaseComponent useCaseComponent;
    private boolean connected = true;

    @Before
    public void setup() {
        Log.i(TAG, "running setup()");
        Context context = InstrumentationRegistry.getTargetContext();
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
        connected = NetworkHelper.isConnected(InstrumentationRegistry.getTargetContext());

    }

    @Test
    public void removeTripUseCaseTest() {



    }

}
