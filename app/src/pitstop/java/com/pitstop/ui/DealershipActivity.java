package com.pitstop.ui;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.Car;
import com.pitstop.utils.MixpanelHelper;

import java.util.HashMap;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.smooch.core.User;
import io.smooch.ui.ConversationActivity;

public class DealershipActivity extends AppCompatActivity {

    private static final String TAG = DealershipActivity.class.getSimpleName();
    private Car dashboardCar;
    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dealership);
        ButterKnife.bind(this);
        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);
        dashboardCar = getIntent().getParcelableExtra("dashboardCar");
    }

    @OnClick(R.id.message_dealer_container)
    public void startChat(View view) {
        if (!checkDealership()) return;
/*        mixpanelHelper.trackButtonTapped("Confirm chat with " + dashboardCar.getDealership().getName(),
                viewPager.getCurrentItem() == MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD ?
                        MixpanelHelper.DASHBOARD_VIEW : MixpanelHelper.TOOLS_VIEW);*/


        final HashMap<String, Object> customProperties = new HashMap<>();
        customProperties.put("VIN", dashboardCar.getVin());
        customProperties.put("Car Make", dashboardCar.getMake());
        customProperties.put("Car Model", dashboardCar.getModel());
        customProperties.put("Car Year", dashboardCar.getYear());
        Log.i(TAG, dashboardCar.getDealership().getEmail());
        customProperties.put("Email", dashboardCar.getDealership().getEmail());
        User.getCurrentUser().addProperties(customProperties);
        if (application.getCurrentUser() != null) {
            customProperties.put("Phone", application.getCurrentUser().getPhone());
            User.getCurrentUser().setFirstName(application.getCurrentUser().getFirstName());
            User.getCurrentUser().setEmail(application.getCurrentUser().getEmail());
        }
        ConversationActivity.show(this);
    }

    /**
     * Onclick method for Navigating button in tools
     *
     * @param view
     */
    @OnClick(R.id.navigate_dealer_container)
    public void navigateToDealer(View view) {
        if (!checkDealership()) return;

        mixpanelHelper.trackButtonTapped("Directions to " + dashboardCar.getDealership().getName(),
                MixpanelHelper.TOOLS_VIEW);

        String uri = String.format(Locale.ENGLISH,
                "http://maps.google.com/maps?daddr=%s",
                dashboardCar.getDealership().getAddress());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    /**
     * Onclick method for Calling Dealer button in tools
     *
     * @param view
     */
    @OnClick(R.id.call_dealer_container)
    public void callDealer(View view) {
        if (!checkDealership()) return;

        mixpanelHelper.trackButtonTapped("Confirm call to " + dashboardCar.getDealership().getName(),
                MixpanelHelper.TOOLS_VIEW);

        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" +
                dashboardCar.getDealership().getPhone()));
        startActivity(intent);
    }

    private boolean checkDealership() {
        if (dashboardCar == null) {
            return false;
        }

        if (dashboardCar.getDealership() == null) {
            /*Snackbar.make(rootView, "Please select your dealership first!", Snackbar.LENGTH_LONG)
                    .setAction("Select", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            selectDealershipForDashboardCar();
                        }
                    })
                    .show();*/
            Toast.makeText(this, "No dealership selected, please select a dealership from settings", Toast.LENGTH_LONG).show();

            return false;
        }
        return true;
    }

}
