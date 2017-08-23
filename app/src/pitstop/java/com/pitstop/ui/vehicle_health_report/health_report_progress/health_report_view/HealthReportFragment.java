package com.pitstop.ui.vehicle_health_report.health_report_progress.health_report_view;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.florent37.viewanimator.ViewAnimator;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.issue_detail.IssueDetailsActivity;
import com.pitstop.ui.main_activity.MainActivity;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matt on 2017-08-17.
 */

public class HealthReportFragment extends Fragment implements HealthReportView {
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

    private int recallListHolderHeight;
    @BindView(R.id.recall_list_holder)
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

    private int engineListHolderHeight;
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

    @BindView(R.id.report_services_loading)
    ProgressBar servicesLoading;

    private List<CarIssue> issues;
    private List<CarIssue> recalls;

    public void inputCarIssues(List<CarIssue> issues, List<CarIssue> recalls){
        this.issues = issues;
        this.recalls = recalls;
    }

    public List<CarIssue> getIssues() {
        return issues;
    }

    public List<CarIssue> getRecalls() {
        return recalls;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;
        View view = inflater.inflate(R.layout.fragment_health_report,container,false);
        ButterKnife.bind(this,view);
        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        presenter = new HealthReportPresenter(component);

        serviceListHolder.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(serviceListHolder.getHeight() >= serviceListHolderHeight){
                    serviceListHolderHeight = serviceListHolder.getHeight();
                }
            }
        });
        serviceListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.serviceButtonClicked();

            }
        });

        recallListHolder.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(recallListHolder.getHeight() >= recallListHolderHeight){
                    recallListHolderHeight = recallListHolder.getHeight();
                }
            }
        });
        recallListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.recallButtonClicked();
            }
        });

        engineListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.engineButtonClicked();
            }
        });
        engineListHolder.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(engineListHolder.getHeight() >= engineListHolderHeight){
                    engineListHolderHeight = engineListHolder.getHeight();
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }


    @Override
    public void setServicesList(List<CarIssue> issues) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        servicesIssueAdapter = new HeathReportIssueAdapter(issues,"No Services",presenter,context);
        servicesList.setAdapter(servicesIssueAdapter);
        servicesList.setLayoutManager(linearLayoutManager);
        if(issues.size()>0){
            serviceRed.setVisibility(View.VISIBLE);
            serviceGreen.setVisibility(View.INVISIBLE);
        }
        serviceCount.setText(Integer.toString(issues.size()));
    }
    @Override
    public void setRecallList(List<CarIssue> recalls) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recallIssueAdapter = new HeathReportIssueAdapter(recalls,"No Recalls",presenter,context);
        recallList.setAdapter(recallIssueAdapter);
        recallList.setLayoutManager(linearLayoutManager);
        if(recalls.size()>0){
            recallRed.setVisibility(View.VISIBLE);
            recallGreen.setVisibility(View.INVISIBLE);
        }
        recallCount.setText(Integer.toString(recalls.size()));
    }

    @Override
    public void setEngineList(List<CarIssue> engine) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        engineIssueAdapter = new HeathReportIssueAdapter(engine,"No New Engine Codes",presenter,context);
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
        if(recallListHolderHeight == 0 ){return;}
        if(recallListHolder.getHeight() == 0){
            ViewAnimator.animate(recallListHolder)
                    .height(0, recallListHolderHeight)
                    .duration(200)
                    .start();
        }else {
            ViewAnimator.animate(recallListHolder)
                    .height(recallListHolder.getHeight(),0)
                    .duration(200)
                    .start();
        }
    }

    @Override
    public void toggleServiceList() {
        if(serviceListHolderHeight == 0){return;}
        if(serviceListHolder.getHeight() == 0){
            ViewAnimator.animate(serviceListHolder)
                    .height(0, serviceListHolderHeight)
                    .duration(200)
                    .start();
        }else{
            ViewAnimator.animate(serviceListHolder)
                    .height(serviceListHolder.getHeight(),0)
                    .duration(200)
                    .start();
        }
    }

    @Override
    public void toggleEngineList() {
        if(engineListHolderHeight == 0){return;}
        if(engineListHolder.getHeight() == 0){
            ViewAnimator.animate(engineListHolder)
                    .height(0, engineListHolderHeight)
                    .duration(200)
                    .start();
        }else{
            ViewAnimator.animate(engineListHolder)
                    .height(engineListHolder.getHeight(),0)
                    .duration(200)
                    .start();
        }
    }

    @Override
    public void servicesLoading(boolean show) {
        if(show){
            servicesLoading.setVisibility(View.VISIBLE);
        }else{
            servicesLoading.setVisibility(View.GONE);
        }
    }

    @Override
    public void startIssueDetails(Car car, CarIssue issue) {
        Intent intent = new Intent(getActivity(), IssueDetailsActivity.class);
        intent.putExtra(MainActivity.CAR_EXTRA, car);
        intent.putExtra(MainActivity.CAR_ISSUE_EXTRA, issue);
        startActivity(intent);

    }
}
