package com.pitstop.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.pitstop.ui.add_car.view_fragment.AddCarChooseDealershipFragment;
import com.pitstop.ui.ILoadingActivity;

import java.util.HashMap;

/**
 * Created by david on 6/9/2016.
 */
public class AddCarViewPagerAdapter extends FragmentStatePagerAdapter {
    private final HashMap<Integer,Fragment> mFragmentList = new HashMap<>();
    private final HashMap<Integer,String> mFragmentTitleList = new HashMap<>();
    private ILoadingActivity callbackActivity;

    public AddCarViewPagerAdapter(FragmentManager fm, ILoadingActivity callbackActivity) {
        super(fm);
        this.callbackActivity = callbackActivity;
    }


    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    public void addFragment(Class fragment, String title,int location) {
        try {
            if(mFragmentTitleList.get(location)==null||!mFragmentTitleList.get(location).equals(title)){
                mFragmentList.remove(location);
                mFragmentList.put(location,(Fragment)fragment.newInstance());
                notifyDataSetChanged();
                if(fragment == AddCarChooseDealershipFragment.class){
                    ((AddCarChooseDealershipFragment)mFragmentList.get(location)).setCallbackActivity(callbackActivity);
                }
                mFragmentTitleList.put(location,title);
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}
