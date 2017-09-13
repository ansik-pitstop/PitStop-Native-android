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
    private View unknownErrorView;

    @BindView(R.id.no_notification_view)
    private View noNotificationsView;

    @BindView(R.id.offline_view)
    private View offlineView;

    @BindView(R.id.swiperefresh)
    private SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.loading_spinner)
    private ProgressBar loadingView;

    @BindView(R.id.notifications_recyclerview)
    private RecyclerView notificationRecyclerView;

    private AlertDialog offlineAlertDialog;
    private AlertDialog unknownErrorDialog;
    private boolean hasBeenPopulated = false;
    private NotificationsPresenter presenter;
    private TabSwitcher tabSwitcher;

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

        ButterKnife.bind(this,view);

        notificationRecyclerView = (RecyclerView) view.findViewById(R.id.notifications_recyclerview);
        loadingView = (ProgressBar) view.findViewById(R.id.loading_spinner);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        offlineView = (View) view.findViewById(R.id.offline_view);
        tabSwitcher = (TabSwitcher) getActivity();
        unknownErrorView =(View) view.findViewById(R.id.unknown_error_view);
        noNotificationsView = (View) view.findViewById(R.id.no_notification_view);
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
        presenter.subscribe(this);
        presenter.onUpdateNeeded();
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void noNotifications() {
        unknownErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        notificationRecyclerView.setVisibility(View.GONE);
        noNotificationsView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.unsubscribe();
        hasBeenPopulated = false;
    }

    public void displayNotifications(List <Notification> notifList){
        if (presenter == null) return;
        unknownErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        noNotificationsView.setVisibility(View.GONE);
        notificationRecyclerView.setVisibility(View.VISIBLE);
        notificationRecyclerView.setAdapter(new NotificationAdapter(this, notifList));
        hasBeenPopulated = true;
    }

    public void showLoading(){
        if (presenter == null){return;}
        if (swipeRefreshLayout.isRefreshing() == false){
            loadingView.setVisibility(View.VISIBLE);
        }
    }
    public void hideLoading(){
        if (presenter == null){return;}
        if (swipeRefreshLayout.isRefreshing()){
            swipeRefreshLayout.setRefreshing(false);
        }else{
            loadingView.setVisibility(View.GONE);
            swipeRefreshLayout.setEnabled(true);
        }
    }

    @Override
    public void hideRefreshing() {
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public boolean isRefreshing() {
        return swipeRefreshLayout.isRefreshing();
    }

    public boolean hasBeenPoppulated(){
        return hasBeenPopulated;
    }

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


    public void displayOfflineErrorView(){
        Log.d(TAG,"displayOfflineErrorView()");
        unknownErrorView.setVisibility(View.GONE);
        noNotificationsView.setVisibility(View.GONE);
        notificationRecyclerView.setVisibility(View.GONE);
        offlineView.setVisibility(View.VISIBLE);
    }



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
