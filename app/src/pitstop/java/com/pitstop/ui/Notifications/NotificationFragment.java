package com.pitstop.ui.Notifications;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.pitstop.R;
import com.pitstop.adapters.NotificationAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Notification;
import com.pitstop.ui.main_activity.TabSwitcher;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ishan on 2017-09-08.
 */

public class NotificationFragment extends Fragment implements NotificationView, View.OnClickListener {


    // ints to classify types of errors
    private static final int NO_NOTIFICATION = 0;
    private static final int NETWORK_ERROR = 1;
    private static final int NO_NETWORK = 2;

    private NotificationsPresenter presenter;
    private RecyclerView mNotificationRecyclerView;
    private View offlineView;
    private View unknowErrorView;
    private View noNotificationsView;
    private ProgressBar mLoadingSpinner;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList <Notification> notificationList;
    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private boolean hasBeenPoppulated = false;
    private AlertDialog offlineAlertDialog;
    private AlertDialog unknownErrorDialog;
    private Button tryAgainButton;
    TabSwitcher tabSwitcher;



   public static NotificationFragment newInstance(){

       NotificationFragment fragment = new NotificationFragment();
        return fragment;
    }





    public void onNotificationClicked(String title){
        Log.d("NotificationFragment", title);
        presenter.onNotificationClicked(title);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_notifications, null);
        mNotificationRecyclerView = (RecyclerView) view.findViewById(R.id.notifications_recyclerview);
        mLoadingSpinner = (ProgressBar) view.findViewById(R.id.loading_spinner);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        mNotificationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        offlineView = (View) view.findViewById(R.id.offline_view);
        tabSwitcher = (TabSwitcher) getActivity();
        unknowErrorView =(View) view.findViewById(R.id.unknown_error_view);
        noNotificationsView = (View) view.findViewById(R.id.no_notification_view);
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
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                presenter.onRefresh();

            }
        });
        return view;
    }

    @Override
    public void noNotifications() {
        tryAgainButton = (Button) noNotificationsView.findViewById(R.id.try_again_btn);
        tryAgainButton.setOnClickListener(this);
        unknowErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        mNotificationRecyclerView.setVisibility(View.GONE);
        noNotificationsView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.unsubscribe();
        hasBeenPoppulated = false;
    }

    public void displayNotifications(List <Notification> notifList){
        if (presenter == null) return;
        unknowErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        noNotificationsView.setVisibility(View.GONE);
        mNotificationRecyclerView.setVisibility(View.VISIBLE);
        mNotificationRecyclerView.setAdapter(new NotificationAdapter(this, notifList));
        hasBeenPoppulated = true;
    }

    public void showLoading(){
        if (presenter == null){return;}
        if (swipeRefreshLayout.isRefreshing() == false){
            mLoadingSpinner.setVisibility(View.VISIBLE);
        }
    }
    public void hideLoading(){
        if (presenter == null){return;}
        if (swipeRefreshLayout.isRefreshing()){
            swipeRefreshLayout.setRefreshing(false);
        }else{
            mLoadingSpinner.setVisibility(View.GONE);
            swipeRefreshLayout.setEnabled(true);
        }
    }

    public boolean hasBeenPoppulated(){
        return hasBeenPoppulated;
    }

    public void displayOfflineErrorDialog(){
        if (offlineAlertDialog == null){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(R.string.offline_error_title);
            alertDialogBuilder
                    .setMessage(R.string.offline_error)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, (dialog, id) -> {
                        dialog.dismiss();
                    });
            offlineAlertDialog = alertDialogBuilder.create();
        }
        offlineAlertDialog.show();
    }


    public void displayOfflineErrorView(){
        tryAgainButton = (Button) offlineView.findViewById(R.id.offline_try_again) ;
        tryAgainButton.setOnClickListener(this);
        unknowErrorView.setVisibility(View.GONE);
        noNotificationsView.setVisibility(View.GONE);
        mNotificationRecyclerView.setVisibility(View.GONE);
        offlineView.setVisibility(View.VISIBLE);
    }



    public void displayUnknownErrorDialog(){
        if (unknownErrorDialog == null){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(R.string.unknown_error_title);
            alertDialogBuilder
                    .setMessage(R.string.unknown_error)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, (dialog, id) -> {
                        dialog.dismiss();
                    });
            unknownErrorDialog = alertDialogBuilder.create();
        }
        unknownErrorDialog.show();
    }
    public void displayUnknownErrorView(){
        tryAgainButton = (Button)unknowErrorView.findViewById(R.id.unknown_error_try_again);
        tryAgainButton.setOnClickListener(this);
        noNotificationsView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        mNotificationRecyclerView.setVisibility(View.GONE);
        unknowErrorView.setVisibility(View.VISIBLE);
    }

    @Override
    public void openCurrentServices() {
        tabSwitcher.openCurrentServices();
    }

    @Override
    public void openAppointments() {
        tabSwitcher.openAppointments();
    }

    @Override
    public void openScanTab() {
        tabSwitcher.openScanTab();
    }

    @Override
    public void onClick(View v) {
        Log.d("onclick", "tryagainrefresh");
        presenter.onRefresh();

    }
}
