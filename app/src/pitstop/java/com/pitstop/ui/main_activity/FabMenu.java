package com.pitstop.ui.main_activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.ui.service_request.ServiceRequestActivity;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.smooch.core.User;
import io.smooch.ui.ConversationActivity;

import static com.pitstop.ui.main_activity.MainActivity.RC_REQUEST_SERVICE;

/**
 * Created by Karol Zdebel on 6/23/2017.
 */

public class FabMenu {

    private final String TAG = getClass().getSimpleName();
    private final int FAB_DELAY = 50;

    @BindView(R.id.fab_main)
    FloatingActionButton mFabMain;

    @BindView(R.id.fab_call)
    FloatingActionButton mFabCall;

    @BindView(R.id.fab_find_directions)
    FloatingActionButton mDabDirections;

    @BindView(R.id.fab_menu_request_service)
    FloatingActionButton mFabRequestService;

    @BindView(R.id.fab_menu_message)
    FloatingActionButton mFabMessage;

    private Activity mActivity;
    private GlobalApplication mApplication;
    private MixpanelHelper mMixpanelHelper;
    private UseCaseComponent mUseCaseComponent;

    private boolean isFabOpen = false;

    public FabMenu(GlobalApplication application, Activity activity
            , UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {

        mActivity = activity;
        mUseCaseComponent = useCaseComponent;
        mApplication = application;
        mMixpanelHelper = mixpanelHelper;
    }

    public void createMenu(){
        ButterKnife.bind(this,mActivity);

        setupFabMain();
        setupFabMessage();
        setupFabCall();
        setupFabRequestService();
        setupFabDirections();
    }

    private void setupFabMessage(){

        mFabMessage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mUseCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {

                    @Override
                    public void onCarRetrieved(Car car) {

                        mMixpanelHelper.trackFabClicked("Message");
                        final HashMap<String, Object> customProperties = new HashMap<>();
                        customProperties.put("VIN", car.getVin());
                        customProperties.put("Car Make", car.getMake());
                        customProperties.put("Car Model", car.getModel());
                        customProperties.put("Car Year", car.getYear());
                        Log.i(TAG, car.getDealership().getEmail());
                        customProperties.put("Email", car.getDealership().getEmail());
                        User.getCurrentUser().addProperties(customProperties);

                        if (mApplication.getCurrentUser() != null) {
                            customProperties.put("Phone", mApplication.getCurrentUser().getPhone());
                            User.getCurrentUser().setFirstName(mApplication.getCurrentUser().getFirstName());
                            User.getCurrentUser().setEmail(mApplication.getCurrentUser().getEmail());
                        }
                        ConversationActivity.show(mActivity);
                    }

                    @Override
                    public void onNoCarSet() {

                    }

                    @Override
                    public void onError() {

                    }
                });
            }
        });
    }

    private void setupFabRequestService(){

        mFabRequestService.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mUseCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {

                    @Override
                    public void onCarRetrieved(Car car) {
                        mMixpanelHelper.trackFabClicked("Request Service");
                        final Intent intent = new Intent(mActivity, ServiceRequestActivity.class);
                        intent.putExtra(ServiceRequestActivity.EXTRA_CAR, car);
                        intent.putExtra(ServiceRequestActivity.EXTRA_FIRST_BOOKING, false);
                        mActivity.startActivityForResult(intent, RC_REQUEST_SERVICE);
                    }

                    @Override
                    public void onNoCarSet() {

                    }

                    @Override
                    public void onError() {

                    }
                });
            }
        });
    }

    private void setupFabCall(){
        mFabCall.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mUseCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
                    @Override
                    public void onCarRetrieved(Car car) {
                        mMixpanelHelper.trackFabClicked("Call");
                        mMixpanelHelper.trackButtonTapped("Confirm call to " + car.getDealership().getName(),
                                MixpanelHelper.TOOLS_VIEW);
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" +
                                car.getDealership().getPhone()));
                        mActivity.startActivity(intent);
                    }

                    @Override
                    public void onNoCarSet() {

                    }

                    @Override
                    public void onError() {

                    }
                });
            }
        });
    }

    private void setupFabDirections(){

        mDabDirections.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mUseCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {

                    @Override
                    public void onCarRetrieved(Car car) {
                        mMixpanelHelper.trackFabClicked("Directions");
                        mMixpanelHelper.trackButtonTapped("Directions to " + car.getDealership().getName(),
                                MixpanelHelper.TOOLS_VIEW);

                        String uri = String.format(Locale.ENGLISH,
                                "http://maps.google.com/maps?daddr=%s",
                                car.getDealership().getAddress());
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        mActivity.startActivity(intent);
                    }

                    @Override
                    public void onNoCarSet() {

                    }

                    @Override
                    public void onError() {

                    }
                });
            }
        });
    }

    private void setupFabMain(){
        final ArrayList<Animation> open_anims = new ArrayList<>();
        final ArrayList<Animation> close_anims = new ArrayList<>();

        //Add delay between animations of each FAB to avoid performance decrease
        for (int i=0;i<4;i++){
            Animation fab_open = AnimationUtils.loadAnimation(mApplication, R.anim.fab_open);
            fab_open.setStartOffset((4-i)*FAB_DELAY);
            open_anims.add(fab_open);

            Animation fab_close = AnimationUtils.loadAnimation(mApplication, R.anim.fab_close);
            fab_close.setStartOffset(i*FAB_DELAY);
            close_anims.add(fab_close);
        }

        final Animation rotate_forward = AnimationUtils.loadAnimation(mApplication,R.anim.rotate_forward);
        final Animation rotate_backward = AnimationUtils.loadAnimation(mApplication,R.anim.rotate_backward);

        mFabMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMixpanelHelper.trackFabClicked("Main");
                if(isFabOpen){

                    mFabMain.startAnimation(rotate_backward);
                    //Begin closing animation
                    mDabDirections.startAnimation(close_anims.get(0));
                    mFabCall.startAnimation(close_anims.get(1));
                    mFabMessage.startAnimation(close_anims.get(2));
                    mFabRequestService.startAnimation(close_anims.get(3));

                    //Don't let the user click
                    mFabCall.setClickable(false);
                    mFabRequestService.setClickable(false);
                    mDabDirections.setClickable(false);
                    mFabMessage.setClickable(false);

                    isFabOpen = false;

                } else {

                    //Begin opening animation
                    mFabMain.startAnimation(rotate_forward);
                    mDabDirections.startAnimation(open_anims.get(0));
                    mFabCall.startAnimation(open_anims.get(1));
                    mFabMessage.startAnimation(open_anims.get(2));
                    mFabRequestService.startAnimation(open_anims.get(3));

                    //Let the user click fab
                    mFabCall.setClickable(true);
                    mFabRequestService.setClickable(true);
                    mDabDirections.setClickable(true);
                    mFabMessage.setClickable(true);

                    isFabOpen = true;

                }
            }
        });
    }
}
