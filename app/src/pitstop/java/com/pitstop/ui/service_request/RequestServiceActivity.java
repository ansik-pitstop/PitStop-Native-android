package com.pitstop.ui.service_request;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.ui.service_request.view_fragment.main_from_view.ServiceFormFragment;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Matthew on 2017-07-11.
 */

public class RequestServiceActivity extends AppCompatActivity implements RequestServiceView,RequestServiceCallback {

    private final String TAG = RequestServiceActivity.class.getSimpleName();

    public interface activityResult{
        int RESULT_SUCCESS = 1;
        int RESULT_FAILURE = 0;
        String EXTRA_FIRST_BOOKING = "is_first_booking";
        String STATE_TENTATIVE = "tentative";
        String STATE_REQUESTED = "requested";
    }

    private ServiceFormFragment serviceFormFragment;

    private RequestServicePresenter presenter;

    private FragmentManager fragmentManager;

    private boolean isFirstBooking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_service);
        isFirstBooking = getIntent().getExtras().getBoolean(activityResult.EXTRA_FIRST_BOOKING);

        fragmentManager = getSupportFragmentManager();
        MixpanelHelper mixpanelHelper = new MixpanelHelper((GlobalApplication)getApplication());

        presenter = new RequestServicePresenter(this,mixpanelHelper);

        serviceFormFragment = new ServiceFormFragment();
        serviceFormFragment.setActivityCallback(this);


    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.subscribe(this);
    }

    @Override
    public void setViewMainForm() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.request_service_fragment_holder,serviceFormFragment);
        fragmentTransaction.commit();
    }

    @Override
    protected void onDestroy() {
        presenter.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void finishActivity(boolean success) {
        Log.d(TAG,"finishActivity() success? "+success);
        setResult(success? activityResult.RESULT_SUCCESS: activityResult.RESULT_FAILURE);
        finish();
    }

    @Override
    public String checkTentative() {
        if(isFirstBooking){
            return activityResult.STATE_TENTATIVE;
        }else{
            return activityResult.STATE_REQUESTED;
        }
    }
}
