package com.pitstop.ui.Notifications;

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
import com.pitstop.R;
import com.pitstop.adapters.NotificationListAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Notification;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ishan on 2017-09-08.
 */

public class NotificationFragment extends Fragment implements NotificationView {

    
    // ints to classify types of errors
    private static final int NO_NOTIFICATION = 0;
    private static final int NETWORK_ERROR = 1;
    private static final int NO_NETWORK = 2;

    private NotificationsPresenter presenter;
    private boolean notificationsLoaded = false;
    private boolean updating = false;
    private RecyclerView mNotificationRecyclerView;
    private LinearLayout mNotificationsContainer;
    private TextView mNoNotificationsTextView;
    private ProgressBar mLoadingSpinner;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList <Notification> notificationList;
    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;



   public static NotificationFragment newInstance(){

       NotificationFragment fragment = new NotificationFragment();
        return fragment;


    }

    public static void onNotificationClicked(String title){
        Log.d("NotificationFragment", title);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_notifications, null);
        mNotificationRecyclerView = (RecyclerView) view.findViewById(R.id.notifications_recyclerview);
        mNotificationsContainer = (LinearLayout) view.findViewById(R.id.no_notification_container);
        mNoNotificationsTextView = (TextView) view.findViewById(R.id.no_notifications_textview);
        mLoadingSpinner = (ProgressBar) view.findViewById(R.id.loading_spinner);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        mNotificationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));


        if (presenter == null) {
            useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getContext()))
                    .build();

            mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication)getActivity().getApplicationContext());

            presenter = new NotificationsPresenter(useCaseComponent, mixpanelHelper);
        }
        presenter.subscribe(this);
       presenter.onUpdateNeeded();
        Log.d("notificationFragment", "display?");

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                presenter.onRefresh();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        return view;
    }

    @Override
    public void noNotifications() {

        mNotificationRecyclerView.setVisibility(View.GONE);
        mNotificationsContainer.setVisibility(View.VISIBLE);
        mNoNotificationsTextView.setText("You don't have any Notifications");



    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.unsubscribe();
    }

    public void displayNotifications(List <Notification> notifList){
        mNotificationRecyclerView.setAdapter(new com.pitstop.adapters.NotificationListAdapter(notifList));
    }

    public void showLoading(){


    }

    public void hideLoading(){

    }
}
