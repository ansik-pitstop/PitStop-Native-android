package com.pitstop.ui.trip;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;

import butterknife.ButterKnife;

/**
 * Created by David C. on 10/3/18.
 */

public class TripListFragment extends Fragment /*implements TripListView*/ {

    private final String TAG = getClass().getSimpleName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        View view = inflater.inflate(R.layout.fragment_trip_list, null);
        ButterKnife.bind(this,view);

//        tabSwitcher = (TabSwitcher)getActivity();
//        notificationAdapter = new NotificationAdapter(this, notificationList);
//        notificationRecyclerView.setAdapter(notificationAdapter);
//        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
//        if (presenter == null) {
//            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
//                    .contextModule(new ContextModule(getContext()))
//                    .build();
//
//            MixpanelHelper mixpanelHelper = new MixpanelHelper(
//                    (GlobalApplication)getActivity().getApplicationContext());
//
//            presenter = new NotificationsPresenter(useCaseComponent, mixpanelHelper);
//        }
//        swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefresh());

        return view;
    }

}
