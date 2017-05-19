package com.pitstop.ui.mainFragments;

import android.support.v4.app.Fragment;

import java.util.List;

/**
 * This class is responsible for implementing functionality related to
 *  relaying different types of data to fragments asynchronously.
 *
 * Created by Karol Zdebel on 5/8/2017.
 */

abstract public class FragmentCallbackManager {

    List<Fragment> fragmentList;

    public void addFragment(Fragment fragment){
        fragmentList.add(fragment);
    }
}
