package com.pitstop.ui.services;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
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

import com.google.gson.Gson;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.Issue;
import com.pitstop.models.Timeline;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class UpcomingServicesFragment extends SubServiceFragment{

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

    NetworkHelper mNetworkHelper;
    MixpanelHelper mMixPanelHelper;
    Timeline mTimelineData;
    Map<String, List<Issue>> mTimeLineMap; //Kilometer Section - List of  items in the section
    List<Object> mTimelineDisplayList;
    List<Issue> mIssueList;
    boolean mIssueDetailsViewVisible = false;
    boolean mIssueDetailsViewAnimating = false;

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

        mNetworkHelper = new NetworkHelper(getContext().getApplicationContext());
        mMixPanelHelper = new MixpanelHelper((GlobalApplication) getActivity().getApplicationContext());
        mTimeLineMap = new HashMap<>();
        mTimelineDisplayList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_upcoming_services, container, false);
        ButterKnife.bind(this, view);

        //This must be called so that UI elements are set for SubService
        super.onCreateView(inflater,container,savedInstanceState);

        return view;
    }

    public void onDashboardCarUpdated(){
        setUI();
    }

    @Override
    public void setUI() {
        mTimeLineRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        ObjectAnimator.ofFloat(mIssueDetailsView, View.TRANSLATION_X, 0, UiUtils.getScreenWidth(getActivity())).start();
        fetchData();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        MainServicesFragment.upcomingServicesFragment = this;
    }

    private void fetchData() {
        Log.d("TAG","UpcomingServicesFragment, fetchData()");
        mLoadingSpinner.setVisibility(View.VISIBLE);
        String carId = String.valueOf(dashboardCar.getId());
        mNetworkHelper.getCarTimeline(carId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (response != null && requestError == null) {
                    mTimelineData = new Gson().fromJson(response, Timeline.class);
                    mIssueList = mTimelineData.getResults().get(DEALERSHIP_ISSUES).getIssues();
                    if (mIssueList != null && mIssueList.size() != 0)
                        populateList();
                    else{
                        Log.d("TAG","UpcomingServicesFragment, showNoData()");
                        showNoData();
                    }
                }
                else
                    showError();
                mLoadingSpinner.setVisibility(View.GONE);
            }
        });
    }

    private void populateList() {
        prepareMap();
        prepareList();
        mTimeLineRecyclerView.setAdapter(new TimelineAdapter());

    }

    private void prepareList() {
        List<String> mileageKeys = new ArrayList<>(mTimeLineMap.keySet());
        Collections.sort(mileageKeys, new Comparator<String>() {
            @Override
            public int compare(String s, String s2) {
                int numb1 = Integer.valueOf(s);
                int numb2 = Integer.valueOf(s2);
                return numb1 > numb2 ? 1 : -1 ;
            }
        });

        for (String mileage : mileageKeys){
            mTimelineDisplayList.add(mileage);
            mTimelineDisplayList.addAll(mTimeLineMap.get(mileage));
        }
    }

    private void prepareMap() {
        List<Issue> newIssueList;
        for (Issue issue : mIssueList){
            if (mTimeLineMap.containsKey(issue.getIntervalMileage())) {
                newIssueList = mTimeLineMap.get(issue.getIntervalMileage());
            }
            else {
                newIssueList = new ArrayList<>();
            }
            newIssueList.add(issue);
            mTimeLineMap.put(issue.getIntervalMileage(), newIssueList);
        }
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
    }

    @OnClick(R.id.error_view_container)
    protected void onTryAgainClicked(){
        if (mTryAgain.getVisibility() != View.VISIBLE) return;
        mErrorViewContainer.setVisibility(View.GONE);
        fetchData();
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

    private void showIssueDetails(Issue issue) {
        if (mIssueDetailsViewVisible || mIssueDetailsViewAnimating) return;
        mIssueTitle.setText(issue.getIssueDetail().getAction() + " " + issue.getIssueDetail().getItem());
        if (!TextUtils.isEmpty(issue.getIssueDetail().getDescription())) {
            mIssueDescriptionContainer.setVisibility(View.VISIBLE);
            mIssueDescription.setText(issue.getIssueDetail().getDescription());
        }
        switch (issue.getPriority()) {
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

    /*If setUserVisibilityHint is called and the view has been created then this
    means that MainServicesTab has been created and therefore dashboard car is set
    therefore proceed with updating UI elements*/
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isViewShown()){
            mTimeLineRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            ObjectAnimator.ofFloat(mIssueDetailsView, View.TRANSLATION_X, 0, UiUtils.getScreenWidth(getActivity())).start();
            fetchData();
        }
    }

    //Called when the Main Service Tab is re-opened, update elements
    @Override
    public void onMainServiceTabReopened() {

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
                ((IssueViewHolder) holder).bind((Issue)mTimelineDisplayList.get(position));
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
        Issue issue;

        public IssueViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTitleTextView = (TextView) itemView.findViewById(R.id.title);
        }

        public void bind(Issue issue) {
            this.issue = issue;
            mTitleTextView.setText(issue.getIssueDetail().getAction() + " " +issue.getIssueDetail().getItem());
        }

        @Override
        public void onClick(View view) {
            showIssueDetails(issue);
        }
    }
}
