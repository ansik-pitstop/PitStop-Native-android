package com.pitstop.ui.issue_detail;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.application.GlobalApplication;
import com.pitstop.ui.CarHistoryActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.issue_detail.view_fragments.IssuePagerAdapter;
import com.pitstop.ui.service_request.ServiceRequestActivity;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.UiUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class IssueDetailsActivity extends AppCompatActivity {

    private static final String TAG = IssueDetailsActivity.class.getSimpleName();

    private Car dashboardCar;
    private CarIssue carIssue;
    private List<CarIssue> allIssues;

    private boolean fromHistory; // opened from history (no request service)

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    private boolean needToRefresh = false;

    private View rootView;

    @BindView(R.id.issues_vp) ViewPager issuesPager;
    private IssuePagerAdapter issueAdapter;

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
        dashboardCar    = intent.getParcelableExtra(MainActivity.CAR_EXTRA);
        carIssue        = intent.getParcelableExtra(MainActivity.CAR_ISSUE_EXTRA);
        fromHistory     = intent.getBooleanExtra(CarHistoryActivity.ISSUE_FROM_HISTORY, false);
        allIssues       = fromHistory ? dashboardCar.getDoneIssues() : dashboardCar.getActiveIssues();
        issueAdapter    = new IssuePagerAdapter(this, allIssues);

        if (fromHistory) {
            findViewById(R.id.request_service_bn).setVisibility(View.INVISIBLE);
        }

        issuesPager.setAdapter(issueAdapter);
        issuesPager.setOffscreenPageLimit(5);
        issuesPager.setPageMargin(- (int) (1.5 * UiUtils.convertDpToPixel(24, this)));
        for (int index = 0; index < allIssues.size(); index++){
            if (carIssue.getId() == allIssues.get(index).getId()){
                issuesPager.setCurrentItem(index);
                break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Track view appeared
        try {
            JSONObject properties = new JSONObject();
            properties.put("View", MixpanelHelper.ISSUE_DETAIL_VIEW);
            properties.put("Issue", carIssue.getAction() + " " + carIssue.getItem());
            mixpanelHelper.trackCustom(MixpanelHelper.EVENT_VIEW_APPEARED, properties);
        } catch (JSONException e) {
            e.printStackTrace();
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

    public void requestService(View view) {
        if (isFinishing()) return;
        mixpanelHelper.trackButtonTapped("Request Service", MixpanelHelper.ISSUE_DETAIL_VIEW);
        startRequestServiceActivity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MainActivity.RC_REQUEST_SERVICE:
                needToRefresh = data.getBooleanExtra(MainActivity.REFRESH_FROM_SERVER, false);
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startRequestServiceActivity() {
        final Intent intent = new Intent(this, ServiceRequestActivity.class);
        intent.putExtra(ServiceRequestActivity.EXTRA_CAR, dashboardCar);
        intent.putExtra(ServiceRequestActivity.EXTRA_FIRST_BOOKING, false);
        startActivityForResult(intent, MainActivity.RC_REQUEST_SERVICE);
    }

}
