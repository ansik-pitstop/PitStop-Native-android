package com.pitstop.ui.services;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetUpcomingServicesMapUseCase;
import com.pitstop.models.issue.UpcomingIssue;
import com.pitstop.models.service.UpcomingService;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.UiUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class UpcomingServicesFragment extends CarDataListenerFragment {

    public static final String CAR_BUNDLE_KEY = "car";

    public static final int DEALERSHIP_ISSUES = 0;

    @BindView(R.id.timeline_recyclerview)
    RecyclerView mTimeLineRecyclerView;

    @BindView(R.id.loading_spinner)
    ProgressBar mLoadingSpinner;

    @BindView(R.id.error_view_container)
    LinearLayout mErrorViewContainer;

    @BindView(R.id.error_text)
    TextView mErrorText;

    @BindView(R.id.try_again)
    TextView mTryAgain;

    @BindView(R.id.issue_details_view)
    FrameLayout mIssueDetailsView;

    @BindView(R.id.issue_title)
    TextView mIssueTitle;

    @BindView(R.id.description_layout)
    LinearLayout mIssueDescriptionContainer;

    @BindView(R.id.description)
    TextView mIssueDescription;

    @BindView(R.id.severity_indicator_layout)
    RelativeLayout mIssueSeverityContainer;

    @BindView(R.id.severity_text)
    TextView mIssueSeverityText;

    MixpanelHelper mMixPanelHelper;
    Map<String, List<UpcomingIssue>> mTimeLineMap; //Kilometer Section - List of  items in the section
    List<Object> mTimelineDisplayList;
    TimelineAdapter timelineAdapter;
    GlobalApplication application;
    ProgressDialog progressDialog;
    boolean mIssueDetailsViewVisible = false;
    boolean mIssueDetailsViewAnimating = false;

    @Inject
    GetUpcomingServicesMapUseCase getUpcomingServicesUseCase;

    public static UpcomingServicesFragment newInstance(){
        UpcomingServicesFragment fragment = new UpcomingServicesFragment();
        return fragment;
    }

    public UpcomingServicesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        application = (GlobalApplication)getContext().getApplicationContext();
        mMixPanelHelper = new MixpanelHelper(application);

        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        component.injectUseCases(this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_upcoming_services, container, false);
        ButterKnife.bind(this, view);
        initUI();
        return view;
    }

    public void initUI() {

        mTimeLineMap = new HashMap<>();
        mTimelineDisplayList = new ArrayList<>();
        mTimeLineRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        ObjectAnimator.ofFloat(mIssueDetailsView, View.TRANSLATION_X, 0, UiUtils.getScreenWidth(getActivity())).start();

        updateUI();
    }

    @Override
    public void updateUI() {

        mTimeLineMap.clear();
        mTimelineDisplayList.clear();

        getUpcomingServicesUseCase.execute(new GetUpcomingServicesMapUseCase.Callback() {
            @Override
            public void onGotUpcomingServicesMap(Map<Integer,List<UpcomingService>> map) {
                if (!map.isEmpty()) {
                    mTimeLineRecyclerView.setVisibility(View.VISIBLE);
                    mErrorViewContainer.setVisibility(View.INVISIBLE);
                }
                else{
                    Log.d("TAG","UpcomingServicesFragment, showNoData()");
                    showNoData();
                }

                for (Integer mileage : map.keySet()){
                    mTimelineDisplayList.add(String.valueOf(mileage));
                    mTimelineDisplayList.addAll(map.get(mileage));
                }

                timelineAdapter = new TimelineAdapter();
                mTimeLineRecyclerView.setAdapter(timelineAdapter);
            }

            @Override
            public void onError() {
                progressDialog.dismiss();
                showError();
            }
        });

    }

    private void showError() {
        mErrorText.setText(R.string.timeline_error_message);
        mErrorViewContainer.setVisibility(View.VISIBLE);
        mTryAgain.setVisibility(View.VISIBLE);
    }

    private void showNoData() {
        mErrorText.setText(R.string.no_data_timeline);
        mErrorViewContainer.setVisibility(View.VISIBLE);
        mTryAgain.setVisibility(View.GONE);
        mTimeLineRecyclerView.setVisibility(View.INVISIBLE);
    }

    @OnClick(R.id.error_view_container)
    protected void onTryAgainClicked(){
        if (mTryAgain.getVisibility() != View.VISIBLE) return;
        mErrorViewContainer.setVisibility(View.GONE);
        updateUI();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mIssueDetailsViewVisible) {
            hideIssueDetails();
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }

    private void showIssueDetails(UpcomingService upcomingService) {
        if (mIssueDetailsViewVisible || mIssueDetailsViewAnimating) return;
        mIssueTitle.setText(upcomingService.getAction() + " " + upcomingService.getItem());
        if (!TextUtils.isEmpty(upcomingService.getDescription())) {
            mIssueDescriptionContainer.setVisibility(View.VISIBLE);
            mIssueDescription.setText(upcomingService.getDescription());
        }
        switch (upcomingService.getPriority()) {
            case 1:
                mIssueSeverityContainer.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.severity_low_indicator));
                mIssueSeverityText.setText(this.getResources().getStringArray(R.array.severity_indicators)[0]);
                break;
            case 2:
                mIssueSeverityContainer.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.severity_medium_indicator));
                mIssueSeverityText.setText(this.getResources().getStringArray(R.array.severity_indicators)[1]);
                break;
            default:
                mIssueSeverityContainer.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.severity_critical_indicator));
                mIssueSeverityText.setText(this.getResources().getStringArray(R.array.severity_indicators)[3]);
                break;
        }
        ObjectAnimator showIssueDetailsAnimation = ObjectAnimator.ofFloat(mIssueDetailsView, View.TRANSLATION_X, UiUtils.getScreenWidth(getActivity()), 0)
                .setDuration(300);
        showIssueDetailsAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mIssueDetailsViewAnimating = true;
                mIssueDetailsView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mIssueDetailsViewVisible = true;
                mIssueDetailsViewAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        showIssueDetailsAnimation.start();

    }

    private void hideIssueDetails(){
        ObjectAnimator hideIssueDetailsAnimation =ObjectAnimator.ofFloat(mIssueDetailsView, View.TRANSLATION_X, 0, UiUtils.getScreenWidth(getActivity()))
                .setDuration(300);
        hideIssueDetailsAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mIssueDetailsViewAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mIssueDetailsView.setVisibility(View.GONE);
                mIssueDetailsViewVisible = false;
                mIssueDetailsViewAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        hideIssueDetailsAnimation.start();
    }

    class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        private static final int MILEAGE = 0;
        private static final int ISSUE = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int layoutId;
            switch (viewType){
                case MILEAGE:
                    layoutId = R.layout.mileage_timeline_list_item;
                    return new MileageViewHolder(inflateLayout(parent, layoutId));
                case ISSUE:
                    layoutId = R.layout.issue_timeline_list_item;
                    return new IssueViewHolder(inflateLayout(parent, layoutId));
                default:
                    return null;
            }
        }

        private View inflateLayout(ViewGroup parent, @LayoutRes int layoutResId){
            return LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof MileageViewHolder){
                ((MileageViewHolder) holder).bind((String)mTimelineDisplayList.get(position));
            } else if (holder instanceof IssueViewHolder){
                ((IssueViewHolder) holder).bind((UpcomingService)mTimelineDisplayList.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return mTimelineDisplayList.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (mTimelineDisplayList.get(position) instanceof String)
                return MILEAGE;
            else
                return ISSUE;
        }
    }

    class MileageViewHolder extends RecyclerView.ViewHolder{

        TextView mileageTextView;

        public MileageViewHolder(View itemView) {
            super(itemView);
            mileageTextView = (TextView) itemView;
        }

        public void bind(String s) {
            String mileage = s + " " + getString(R.string.kilometers_unit);
            mileageTextView.setText(mileage);
        }
    }

    class IssueViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView mTitleTextView;
        UpcomingService upcomingService;

        public IssueViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTitleTextView = (TextView) itemView.findViewById(R.id.title);
        }

        public void bind(UpcomingService upcomingService) {
            this.upcomingService = upcomingService;
            mTitleTextView.setText(upcomingService.getAction() + " " + upcomingService.getItem());
        }

        @Override
        public void onClick(View view) {
            showIssueDetails(upcomingService);
        }
    }
}
