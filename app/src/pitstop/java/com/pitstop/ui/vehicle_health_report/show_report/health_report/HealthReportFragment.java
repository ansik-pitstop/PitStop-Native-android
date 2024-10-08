package com.pitstop.ui.vehicle_health_report.show_report.health_report;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.florent37.viewanimator.ViewAnimator;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.application.GlobalVariables;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.report.CarHealthItem;
import com.pitstop.models.report.EngineIssue;
import com.pitstop.models.report.Recall;
import com.pitstop.models.report.Service;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.ui.issue_detail.IssueDetailsActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportHolder;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matt on 2017-08-17.
 */

public class HealthReportFragment extends Fragment implements HealthReportView {

    private final String TAG = getClass().getSimpleName();

    private HealthReportPresenter presenter;
    private Context context;
    private GlobalApplication application;

    private  HeathReportIssueAdapter servicesIssueAdapter;
    private  HeathReportIssueAdapter recallIssueAdapter;
    private  HeathReportIssueAdapter engineIssueAdapter;

    private int serviceListHolderHeight;
    @BindView(R.id.service_list_holder)
    RelativeLayout serviceListHolder;
    @BindView(R.id.service_list_button)
    RelativeLayout serviceListButton;
    @BindView(R.id.services_list)
    RecyclerView servicesList;
    @BindView(R.id.service_count)
    TextView serviceCount;
    @BindView(R.id.service_red)
    ImageView serviceRed;
    @BindView(R.id.service_green)
    ImageView serviceGreen;
    @BindView(R.id.service_right_chevron)
    ImageView serviceRightChevron;

    private int recallListHolderHeight;
    @BindView(R.id.recalls_holder)
    RelativeLayout recallListHolder;
    @BindView(R.id.recall_list_button)
    RelativeLayout recallListButton;
    @BindView(R.id.recalls_list)
    RecyclerView recallList;
    @BindView(R.id.recall_list_count)
    TextView recallCount;
    @BindView(R.id.recall_red)
    ImageView recallRed;
    @BindView(R.id.recall_green)
    ImageView recallGreen;
    @BindView(R.id.recall_right_chevron)
    ImageView recallRightChevron;

    @BindView(R.id.engine_list_holder)
    RelativeLayout engineListHolder;
    @BindView(R.id.engine_list_button)
    RelativeLayout engineListButton;
    @BindView(R.id.engine_codes_list)
    RecyclerView engineList;
    @BindView(R.id.engine_list_count)
    TextView engineListCount;
    @BindView(R.id.engine_red)
    ImageView engineRed;
    @BindView(R.id.engine_green)
    ImageView engineGreen;
    @BindView(R.id.engine_issue_right_chevron)
    ImageView engineIssueRightChevron;

    @BindView(R.id.report_services_loading)
    ProgressBar servicesLoading;
    @BindView(R.id.summary)
    TextView summary;

    private int engineListHolderHeight;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;
        View view = inflater.inflate(R.layout.fragment_health_report,container,false);
        ButterKnife.bind(this,view);
        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        MixpanelHelper mixpanelHelper
                = new MixpanelHelper((GlobalApplication)getActivity().getApplicationContext());
        presenter = new HealthReportPresenter(component, mixpanelHelper, getMainCarId());

        getActivity().setTitle(getString(R.string.vehicle_health_report));

        engineIssueRightChevron.setRotation(90);
        recallRightChevron.setRotation(90);
        serviceRightChevron.setRotation(90);

        recallListHolderHeight = 0;
        engineListHolderHeight = 0;
        serviceListHolderHeight = 0;

        serviceListHolder.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if(serviceListHolder.getHeight() >= serviceListHolderHeight){
                serviceListHolderHeight = serviceListHolder.getHeight();
            }
        });
        serviceListButton.setOnClickListener(view1 -> presenter.serviceButtonClicked());

        recallListHolder.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if(recallListHolder.getHeight() >= recallListHolderHeight){
                recallListHolderHeight = recallListHolder.getHeight();
            }
        });
        recallListButton.setOnClickListener(view12 -> presenter.recallButtonClicked());

        engineListButton.setOnClickListener(view13 -> presenter.engineButtonClicked());
        engineListHolder.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if(engineListHolder.getHeight() >= engineListHolderHeight){
                engineListHolderHeight = engineListHolder.getHeight();
            }
        });

        return view;
    }

    private Integer getMainCarId() {
        return GlobalVariables.Companion.getMainCarId(context);
    }

    @Override
    public void onResume() {
        Log.d(TAG,"onResume()");
        super.onResume();
        presenter.subscribe(this);

    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy()");
        super.onDestroy();
        presenter.unsubscribe();
    }


    @Override
    public void setServicesList(List<Service> services) {
        Log.d(TAG,"setServicesList() issues: "+services);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        List<CarHealthItem> carHealthItemList = new ArrayList<>(services);
        servicesIssueAdapter = new HeathReportIssueAdapter(carHealthItemList
                ,"No Services","",presenter,context);
        servicesList.setAdapter(servicesIssueAdapter);
        servicesList.setLayoutManager(linearLayoutManager);
        if(services.size()>0){
            serviceRed.setVisibility(View.VISIBLE);
            serviceGreen.setVisibility(View.INVISIBLE);
        }
        serviceCount.setText(Integer.toString(services.size()));
    }
    @Override
    public void setRecallList(List<Recall> recalls) {
        Log.d(TAG,"setRecallList() recalls: "+recalls);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        List<CarHealthItem> carHealthItemList = new ArrayList<>(recalls);
        recallIssueAdapter = new HeathReportIssueAdapter(
                carHealthItemList,"No Recalls","",presenter,context);
        recallList.setAdapter(recallIssueAdapter);
        recallList.setLayoutManager(linearLayoutManager);
        if(recalls.size()>0){
            recallRed.setVisibility(View.VISIBLE);
            recallGreen.setVisibility(View.INVISIBLE);
        }
        recallCount.setText(Integer.toString(recalls.size()));
    }

    @Override
    public void setEngineList(List<EngineIssue> engine) {
        Log.d(TAG,"setEngineList() engineList: "+engine);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        List<CarHealthItem> carHealthItemList = new ArrayList<>(engine);
        engineIssueAdapter = new HeathReportIssueAdapter(
                carHealthItemList,"No New Engine Codes"
                ,getString(R.string.engine_code_disclaimer),presenter,context);
        engineList.setAdapter(engineIssueAdapter);
        engineList.setLayoutManager(linearLayoutManager);
        if(engine.size()>0){
            engineRed.setVisibility(View.VISIBLE);
            engineGreen.setVisibility(View.INVISIBLE);
        }
        engineListCount.setText(Integer.toString(engine.size()));
    }

    @Override
    public void toggleRecallList() {
        Log.d(TAG,"toggleRecallList()");
        if(recallListHolderHeight == 0 ){return;}
        if(recallListHolder.getHeight() == 0){
            ViewAnimator.animate(recallListHolder)
                    .height(0, recallListHolderHeight)
                    .andAnimate(recallRightChevron)
                    .rotation(0,90)
                    .duration(200)
                    .start();
        }else {
            ViewAnimator.animate(recallListHolder)
                    .height(recallListHolder.getHeight(),0)
                    .andAnimate(recallRightChevron)
                    .rotation(90,0)
                    .duration(200)
                    .start();
        }
    }

    @Override
    public void toggleServiceList() {
        Log.d(TAG,"toggleServiceList()");
        if(serviceListHolderHeight == 0){return;}
        if(serviceListHolder.getHeight() == 0){
            ViewAnimator.animate(serviceListHolder)
                    .height(0, serviceListHolderHeight)
                    .andAnimate(serviceRightChevron)
                    .rotation(0,90)
                    .duration(200)
                    .start();
        }else{
            ViewAnimator.animate(serviceListHolder)
                    .height(serviceListHolder.getHeight(),0)
                    .andAnimate(serviceRightChevron)
                    .rotation(90,0)
                    .duration(200)
                    .start();
        }
    }

    @Override
    public void toggleEngineList() {
        Log.d(TAG,"toggleEngineList()");
        if(engineListHolderHeight == 0){return;}
        if(engineListHolder.getHeight() == 0){
            ViewAnimator.animate(engineListHolder)
                    .height(0, engineListHolderHeight)
                    .andAnimate(engineIssueRightChevron)
                    .rotation(0,90)
                    .duration(200)
                    .start();
        }else{
            ViewAnimator.animate(engineListHolder)
                    .height(engineListHolder.getHeight(),0)
                    .andAnimate(engineIssueRightChevron)
                    .rotation(90,0)
                    .duration(200)
                    .start();
        }
    }

    @Override
    public void servicesLoading(boolean show) {
        Log.d(TAG,"servicesLoading() show? "+show);
        if(show){
            servicesLoading.setVisibility(View.VISIBLE);
        }else{
            servicesLoading.setVisibility(View.GONE);
        }
    }

    @Override
    public void startIssueDetails(Car car, ArrayList<CarIssue> issues, int position) {
        Log.d(TAG,"startIssueDetails() car: "+car+", issues: "+issues+", position: "+position);
        Intent intent = new Intent(getActivity(), IssueDetailsActivity.class);
        intent.putExtra(MainActivity.CAR_EXTRA, car);
        intent.putParcelableArrayListExtra(MainActivity.CAR_ISSUE_KEY, issues);
        intent.putExtra(MainActivity.CAR_ISSUE_POSITION, position);
        intent.putExtra(IssueDetailsActivity.SOURCE, "");
        startActivity(intent);

    }

    @Override
    public void setVehicleHealthSummary(State state) {
        if (state == State.GOOD){
            this.summary.setText("Good");
            this.summary.setTextColor(Color.rgb(255,165,0));
        }else if (state == State.NEEDS_WORK){
            this.summary.setText("Needs Work");
            this.summary.setTextColor(Color.RED);
        }else if (state == State.PERFECT){
            this.summary.setText("Perfect");
            this.summary.setTextColor(Color.GREEN);
        }
    }

    @Override
    public VehicleHealthReport getVehicleHealthReport() {
        if (getActivity() == null) return null;
        try {
            return ((ReportHolder) getActivity()).getVehicleHealthReport();
        }catch(ClassCastException e){
            e.printStackTrace();
            return null;
        }
    }
}
