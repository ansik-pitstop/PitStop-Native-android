package com.pitstop.ui.mainFragments;

import android.content.Intent;

/**
 * Created by Karol Zdebel on 5/9/2017.
 */

public interface MainDashboardCallback extends MainFragmentCallback{
    void activityResultCallback(int requestCode, int resultCode, Intent data);

    void setCarDetailsUI();

}
