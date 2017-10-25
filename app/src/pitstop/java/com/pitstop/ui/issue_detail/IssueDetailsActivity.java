package com.pitstop.ui.issue_detail;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.pitstop.R;
import com.pitstop.adapters.UpcomingServicePagerAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.service.UpcomingService;
import com.pitstop.ui.issue_detail.view_fragments.IssuePagerAdapter;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.service_request.RequestServiceActivity;
import com.pitstop.ui.services.upcoming.UpcomingServicesFragment;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.UiUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class IssueDetailsActivity extends AppCompatActivity {

    private static final String TAG = IssueDetailsActivity.class.getSimpleName();
    private List<CarIssue> allIssues;
    public static final String SOURCE = "source";
    private int positionClicked;
    ArrayList<UpcomingService> upcomingServicesList;
    Car dashboardCar;

    private boolean fromHistory; // opened from history (no request service)

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    private boolean needToRefresh = false;

    private View rootView;
    private String source;

    @BindView(R.id.issues_vp)
    ViewPager issuesPager;

    @BindView(R.id.request_service_bn)
    Button requestServicebutton;

    private IssuePagerAdapter issueAdapter;
    private UpcomingServicePagerAdapter upcomingServicePagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = getLayoutInflater().inflate(R.layout.activity_issue_details, null);
        setContentView(rootView);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ButterKnife.bind(this);

        application     = (GlobalApplication) getApplicationContext();
        mixpanelHelper  = new MixpanelHelper(application);

        Intent intent   = getIntent();
        source = intent.getExtras().getString(SOURCE);

        if (source != null && source.equalsIgnoreCase(UpcomingServicesFragment.UPCOMING_SERVICE_SOURCE)) {
            upcomingServicesList = intent.getParcelableArrayListExtra(UpcomingServicesFragment.UPCOMING_SERVICE_KEY);
            positionClicked = intent.getExtras().getInt(UpcomingServicesFragment.UPCOMING_SERVICE_POSITION);

            upcomingServicePagerAdapter = new UpcomingServicePagerAdapter(upcomingServicesList, this);
            issuesPager.setAdapter(upcomingServicePagerAdapter);
            issuesPager.setOffscreenPageLimit(5);
            issuesPager.setPageMargin(-(int) (1.5 * UiUtils.convertDpToPixel(24, this)));
            issuesPager.setCurrentItem(positionClicked);
            requestServicebutton.setVisibility(View.INVISIBLE);
        }
        else {
            positionClicked = intent.getExtras().getInt(MainActivity.CAR_ISSUE_POSITION);
            allIssues = intent.getParcelableArrayListExtra(MainActivity.CAR_ISSUE_KEY);
            //allIssues = fromHistory ? dashboardCar.getDoneIssues() : dashboardCar.getActiveIssues();
            issueAdapter = new IssuePagerAdapter(this, allIssues);
            if (fromHistory) {
                findViewById(R.id.request_service_bn).setVisibility(View.INVISIBLE);
            }
            dashboardCar = intent.getExtras().getParcelable(MainActivity.CAR_KEY);
            issuesPager.setAdapter(issueAdapter);
            issuesPager.setOffscreenPageLimit(5);
            issuesPager.setPageMargin(-(int) (1.5 * UiUtils.convertDpToPixel(24, this)));
           issuesPager.setCurrentItem(positionClicked);
        }

    }
    @Override
    protected void onResume() {
        super.onResume();

        if (source != null && !(source.equalsIgnoreCase(UpcomingServicesFragment.UPCOMING_SERVICE_SOURCE))){
            // Track view appeared
            try {
                JSONObject properties = new JSONObject();
                properties.put("View", MixpanelHelper.ISSUE_DETAIL_VIEW);
                properties.put("Issue", allIssues.get(positionClicked).getAction() + " " + allIssues.get(positionClicked).getItem());
                mixpanelHelper.trackCustom(MixpanelHelper.EVENT_VIEW_APPEARED, properties);
            } catch (JSONException e) {
                e.printStackTrace();
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        application.getMixpanelAPI().flush();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.REFRESH_FROM_SERVER, needToRefresh);
        setResult(MainActivity.RESULT_OK, intent);
        super.finish();
    }

    @Override
    public void onBackPressed() {
        mixpanelHelper.trackButtonTapped("Back", MixpanelHelper.ISSUE_DETAIL_VIEW);
        finish();
    }

    @OnClick(R.id.request_service_bn)
    public void requestServiceButtonClicked() {
        Log.d(TAG,"requestServiceButtonClicked()");
        if (isFinishing()) return;
        mixpanelHelper.trackButtonTapped("Request Service", MixpanelHelper.ISSUE_DETAIL_VIEW);
        startRequestServiceActivity();
    }

    @OnClick(R.id.move_history_bn)
    public void moveHistoryButtonClicked() {
        Log.d(TAG,"moveHistoryButtonClicked()");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data != null){
            switch (requestCode) {
                case MainActivity.RC_REQUEST_SERVICE:
                    needToRefresh = data.getBooleanExtra(MainActivity.REFRESH_FROM_SERVER, false);
                    break;
            }
        }else{
            needToRefresh = false;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startRequestServiceActivity() {
        final Intent intent = new Intent(this, RequestServiceActivity.class);
        intent.putExtra(RequestServiceActivity.EXTRA_FIRST_BOOKING, false);
        intent.putExtra(MainActivity.CAR_ISSUE_EXTRA,allIssues.get(positionClicked));
        startActivityForResult(intent, MainActivity.RC_REQUEST_SERVICE);
    }

}
