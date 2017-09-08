package com.pitstop.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.pitstop.R;
import com.pitstop.adapters.NotificationListAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.models.Notification;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by zohaibhussain on 2016-12-19.
 */

public class NotificationsFragment extends Fragment {

    private static final int NO_NOTIFICATION = 0;
    private static final int NETWORK_ERROR = 1;
    public static final int NO_NETWORK = 2;

    private boolean notificationsLoaded;

    @BindView(R.id.notifications_recyclerview)
    RecyclerView mNotificationsRecyclerView;

    @BindView(R.id.no_notification_container)
    LinearLayout mNoNotificationsContainer;

    @BindView(R.id.no_notifications_textview)
    TextView mNoNotificationsTextView;

    @BindView(R.id.loading_spinner)
    ProgressBar mLoadingSpinner;

    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swipeRefreshLayout;


    NetworkHelper mNetworkHelper;
    List<Notification> mNotificationList;
    MixpanelHelper mMixPanelHelper;
    private boolean updating;

    public static NotificationsFragment newInstance() {
        NotificationsFragment fragment = new NotificationsFragment();
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootview = inflater.inflate(R.layout.activity_notifications,null);
        ButterKnife.bind(this,rootview);

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(getContext()))
                .build();

        mMixPanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());
        mNotificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mNetworkHelper = tempNetworkComponent.networkHelper();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (updating) swipeRefreshLayout.setRefreshing(false);
                else fetchNotifications();
            }
        });


        fetchNotifications();
        return rootview;
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && getView() != null && !notificationsLoaded){
            fetchNotifications();
        }
    }



    private void fetchNotifications() {
        if (updating) return;
        if(!mNetworkHelper.isConnected(getActivity())
                || ((GlobalApplication) getApplicationContext()).getCurrentUser() == null) {
            showErrorMessage(NO_NETWORK);
            notificationsLoaded = false;
            if (swipeRefreshLayout.isRefreshing()){
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        notificationsLoaded = true;
        updating = true;
        showLoading();
        mNetworkHelper.getUserInstallationId(((GlobalApplication) getApplicationContext()).getCurrentUserId(), new RequestCallback() {
            @Override
            public void done(final String response, final RequestError requestError) {
                if (response != null && requestError == null) {
                    List<String> userInstallationIds;
                    userInstallationIds = new Gson().fromJson(response, new TypeToken<List<String>>() {
                    }.getType());
                    ParseQuery<Notification> parseQuery = ParseQuery.getQuery("Notification");
                    parseQuery.whereContainedIn("recipients", userInstallationIds);
                    parseQuery.findInBackground(new FindCallback<Notification>() {
                        @Override
                        public void done(List<Notification> notificationsList, ParseException e) {
                            updating = false;
                            if (notificationsList == null) {
                                showFetchError();
                                hideLoading();
                                updating = false;
                                return;
                            }

                            mNotificationList = notificationsList;
                            Collections.sort(mNotificationList, new Comparator<Notification>() {
                                    @Override
                                    public int compare(Notification notification1, Notification notification2) {
                                        return notification2.getCreatedAt().getTime() > notification1.getCreatedAt().getTime() ? 1 : -1;
                                    }
                            });
                            if (requestError != null || e != null)
                                showFetchError();
                            else if (response != null) {
                                if (mNotificationList.size() == 0)

                                {
                                    Notification notification  = new Notification();
                                    Notification newNot = new Notification();
                                    mNotificationList.add(newNot);
                                    mNotificationList.add(notification);


                                    showNotifications();
                                    //showEmptyListView();
                                }

                                else
                                    showNotifications();
                            }
                            updating = false;

                            hideLoading();
                        }
                    });
                }
                else {
                    updating = false;
                    showFetchError();
                    hideLoading();
                }
            }

        });
    }

    private void hideLoading(){
        if (swipeRefreshLayout.isRefreshing()){
            swipeRefreshLayout.setRefreshing(false);
        }else{
            mLoadingSpinner.setVisibility(View.GONE);
            swipeRefreshLayout.setEnabled(true);
        }
    }

    private void showLoading(){
        if (!swipeRefreshLayout.isRefreshing()){
            mLoadingSpinner.setVisibility(View.VISIBLE);
        }
    }

    private void showNotifications() {

        mNotificationsRecyclerView.setAdapter(new com.pitstop.adapters.NotificationListAdapter(mNotificationList));
        mMixPanelHelper.trackViewAppeared(MixpanelHelper.NOTIFICATION_DISPLAYED);


    }


    private void showEmptyListView() {
        showErrorMessage(NO_NOTIFICATION);
        mMixPanelHelper.trackViewAppeared(MixpanelHelper.NO_NOTIFICATION_DISPLAYED);
    }

    private void showFetchError() {
        showErrorMessage(NETWORK_ERROR);
        mMixPanelHelper.trackViewAppeared(MixpanelHelper.NOTIFICATION_FETCH_ERROR);
    }

    private void showErrorMessage(int errorType){
        mNotificationsRecyclerView.setVisibility(View.GONE);
        mNoNotificationsContainer.setVisibility(View.VISIBLE);
        String message;
        if (errorType == NO_NOTIFICATION)
            message = "You don't have any Notifications";
        else if (errorType == NETWORK_ERROR)
            message = "An error occurred";
        else
         message = "Please connect to the Internet";
        mNoNotificationsTextView.setText(message);

    }

    @OnClick(R.id.no_notification_container)
    protected void onTryAgainClicked(){
        mNoNotificationsContainer.setVisibility(View.GONE);
        mNotificationsRecyclerView.setVisibility(View.VISIBLE);
        fetchNotifications();
    }

    private class NotificationListAdapter extends RecyclerView.Adapter<NotificationListViewHolder>{

        @Override
        public NotificationListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.notification_list_item, parent, false);
            return new NotificationListViewHolder(v);
        }

        @Override
        public void onBindViewHolder(NotificationListViewHolder holder, int position) {
            holder.bind(position);
        }

        @Override
        public int getItemCount() {
            return mNotificationList.size();
        }
    }

    private class NotificationListViewHolder extends RecyclerView.ViewHolder{
        TextView notificationTitle;
        TextView notificationDate;
        TextView notificationContent;

        NotificationListViewHolder(View itemView) {
            super(itemView);
            notificationTitle = (TextView) itemView.findViewById(R.id.title);
            notificationDate = (TextView) itemView.findViewById(R.id.date);
            notificationContent = (TextView) itemView.findViewById(R.id.content);
        }

        void bind(int position) {
            Notification notification = mNotificationList.get(position);
            notificationTitle.setText(notification.getTitle());
            notificationContent.setText(notification.getContent());
            notificationDate.setText(notification.getDateCreated());
        }
    }
}
