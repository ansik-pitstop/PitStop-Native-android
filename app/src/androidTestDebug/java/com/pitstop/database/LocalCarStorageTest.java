package com.pitstop.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.pitstop.models.PendingUpdate;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karol Zdebel on 5/28/2018.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LocalCarStorageTest {

    private LocalCarStorage localCarStorage;

    @Before
    public void setup(){
        Context context = InstrumentationRegistry.getTargetContext();
        localCarStorage = new LocalCarStorage(context);
    }

    @Test
    public void pendingUpdateMileageTest(){
        int carId = 6164;
        String mileage1 = "50000.5";
        String mileage2 = "50001.6";

        PendingUpdate pendingUpdate1 = new PendingUpdate(carId,PendingUpdate.CAR_MILEAGE_UPDATE
                ,mileage1, System.currentTimeMillis());
        PendingUpdate pendingUpdate2 = new PendingUpdate(carId,PendingUpdate.CAR_MILEAGE_UPDATE
                ,mileage2, System.currentTimeMillis()+500);
        List<PendingUpdate> pendingUpdates = new ArrayList<>();
        pendingUpdates.add(pendingUpdate1);
        pendingUpdates.add(pendingUpdate2);

        localCarStorage.removeAllPendingUpdates();
        localCarStorage.storePendingUpdate(pendingUpdate1);
        localCarStorage.storePendingUpdate(pendingUpdate2);
        Assert.assertTrue(localCarStorage.getPendingUpdates().size() == 2);
        Assert.assertEquals(localCarStorage.getPendingUpdates(),pendingUpdates);

        localCarStorage.removePendingUpdate(pendingUpdate1);
        pendingUpdates.remove(pendingUpdate1);

        Assert.assertEquals(localCarStorage.getPendingUpdates(),pendingUpdates);

        localCarStorage.storePendingUpdate(pendingUpdate1);
        localCarStorage.removeAllPendingUpdates();
        Assert.assertTrue(localCarStorage.getPendingUpdates().isEmpty());
    }

}
