package com.pitstop.ui.services;

import android.app.Activity;
import android.support.design.widget.FloatingActionButton;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

/**
 * Created by Karol Zdebel on 10/17/2017.
 */

public class ExpandingFabMenu {

    private final String TAG = getClass().getSimpleName();
    private final int FAB_DELAY = 50;

    private boolean isFabOpen = false;

    interface OnFabClickedListener{
        void onFabClicked(int position);
    }

    public ExpandingFabMenu(List<FloatingActionButton> expandingFabs, MixpanelHelper mixpanelHelper
            , GlobalApplication application, Activity activity, OnFabClickedListener onFabClickedListener) {

        ButterKnife.bind(this,activity);

        final ArrayList<Animation> open_anims = new ArrayList<>();
        final ArrayList<Animation> close_anims = new ArrayList<>();

        //Add delay between animations of each FAB to avoid performance decrease
        for (int i = 1; i< expandingFabs.size(); i++){
            Animation fab_open = AnimationUtils.loadAnimation(application, R.anim.fab_open);
            fab_open.setStartOffset((4-i)*FAB_DELAY);
            open_anims.add(fab_open);

            Animation fab_close = AnimationUtils.loadAnimation(application, R.anim.fab_close);
            fab_close.setStartOffset(i*FAB_DELAY);
            close_anims.add(fab_close);
        }

        final Animation rotate_forward = AnimationUtils.loadAnimation(application,R.anim.rotate_forward);
        final Animation rotate_backward = AnimationUtils.loadAnimation(application,R.anim.rotate_backward);

        expandingFabs.get(0).setOnClickListener(view -> {

            if(isFabOpen){

                expandingFabs.get(0).startAnimation(rotate_backward);
                //Begin closing animation
                for (int i=1;i<expandingFabs.size();i++){
                    FloatingActionButton fab = expandingFabs.get(i);
                    fab.startAnimation(close_anims.get(i));
                    fab.setClickable(false);
                }
                isFabOpen = false;

            } else {

                //Begin opening animation
                expandingFabs.get(0).startAnimation(rotate_forward);
                for (int i=1;i<expandingFabs.size();i++){
                    FloatingActionButton fab = expandingFabs.get(i);
                    fab.startAnimation(open_anims.get(i));
                    fab.setClickable(true);
                }

                isFabOpen = true;

            }
        });

        for (int i=0;i<expandingFabs.size();i++){
            FloatingActionButton fab = expandingFabs.get(i);
            final int pos = i;
            fab.setOnClickListener(view -> onFabClickedListener.onFabClicked(pos));
        }
    }

}
