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
import android.widget.ProgressBar;

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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ishan on 2017-09-08.
 */

public class NotificationFragment extends Fragment implements NotificationView{

    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.unknown_error_view)
    protected View unknownErrorView;

    @BindView(R.id.no_notification_view)
    protected View noNotificationsView;

    @BindView(R.id.offline_view)
    protected View offlineView;

    @BindView(R.id.swiperefresh)
    protected SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.loading_spinner)
    protected ProgressBar loadingView;

    @BindView(R.id.notifications_recyclerview)
    protected RecyclerView notificationRecyclerView;

    private AlertDialog offlineAlertDialog;
    private AlertDialog unknownErrorDialog;
    private NotificationsPresenter presenter;
    private TabSwitcher tabSwitcher;
    private NotificationAdapter notificationAdapter;
    private boolean hasBeenPopulated = false;
    private List<Notification> notificationList = new ArrayList<>();

    public static NotificationFragment newInstance(){

       NotificationFragment fragment = new NotificationFragment();
        return fragment;
    }

    public void onNotificationClicked(String title){
        Log.d(TAG, "onNotificationClicked() title: "+title);
        presenter.onNotificationClicked(title);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        View view = inflater.inflate(R.layout.activity_notifications, null);
        ButterKnife.bind(this,view);

        tabSwitcher = (TabSwitcher)getActivity();
        notificationAdapter = new NotificationAdapter(this, notificationList);
        notificationRecyclerView.setAdapter(notificationAdapter);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        if (presenter == null) {
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getContext()))
                    .build();

            MixpanelHelper mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication)getActivity().getApplicationContext());

            presenter = new NotificationsPresenter(useCaseComponent, mixpanelHelper);
        }
        swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefresh());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onViewCreated()");
        presenter.subscribe(this);
        presenter.onUpdateNeeded();
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void noNotifications() {
        Log.d(TAG,"noNotifications()");
        unknownErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        notificationRecyclerView.setVisibility(View.GONE);
        noNotificationsView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView()");
        super.onDestroyView();
        presenter.unsubscribe();
        hasBeenPopulated = false;
    }

    @Override
    public void displayNotifications(List <Notification> notifList){
        Log.d(TAG,"displayNotifications() notifList: "+notifList);
        notificationList.clear();
        notificationList.addAll(notifList);
        notificationAdapter.notifyDataSetChanged();
        hasBeenPopulated = true;
    }

    @Override
    public void showLoading(){
        Log.d(TAG,"showLoading()");
        if (!swipeRefreshLayout.isRefreshing()){
            loadingView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideLoading(){
        Log.d(TAG,"hideLoading()");
        if (swipeRefreshLayout.isRefreshing()){
            swipeRefreshLayout.setRefreshing(false);
        }else{
            loadingView.setVisibility(View.GONE);
            swipeRefreshLayout.setEnabled(true);
        }
    }

    @Override
    public void hideRefreshing() {
        Log.d(TAG,"hideRefreshing()");
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public boolean isRefreshing() {
        Log.d(TAG,"isRefreshing()");
        return swipeRefreshLayout.isRefreshing();
    }

    @Override
    public boolean hasBeenPopulated(){
        Log.d(TAG,"hasBeenPopulated() ? "+hasBeenPopulated);
        return hasBeenPopulated;
    }

    @Override
    public void displayOfflineErrorDialog(){
        Log.d(TAG,"displayOfflineErrorDialog()");
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

    @Override
    public void displayOfflineView(){
        Log.d(TAG,"displayOfflineView()");
        unknownErrorView.setVisibility(View.GONE);
        noNotificationsView.setVisibility(View.GONE);
        notificationRecyclerView.setVisibility(View.GONE);
        offlineView.setVisibility(View.VISIBLE);
    }

    @Override
    public void displayOnlineView(){
        Log.d(TAG,"displayOnlineView()");
        unknownErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        noNotificationsView.setVisibility(View.GONE);
        notificationRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void displayUnknownErrorDialog(){
        Log.d(TAG,"displayUnknownErrorDialog()");
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

    @Override
    public void displayUnknownErrorView(){
        Log.d(TAG,"displayUnknownErrorView()");
        noNotificationsView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        notificationRecyclerView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.VISIBLE);
    }

    @Override
    public void openCurrentServices() {
        Log.d(TAG,"openCurrentServices()");
        tabSwitcher.openCurrentServices();
    }

    @Override
    public void openAppointments() {
        Log.d(TAG,"openAppointments()");
        tabSwitcher.openAppointments();
    }

    @Override
    public void openScanTab() {
        Log.d(TAG,"openScanTab()");
        tabSwitcher.openScanTab();
    }

    @OnClick(R.id.try_again_btn)
    public void onTryAgainClicked() {
        Log.d(TAG, "onTryAgainClicked()");
        presenter.onRefresh();
    }
}
