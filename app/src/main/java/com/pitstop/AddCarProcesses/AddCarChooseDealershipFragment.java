package com.pitstop.AddCarProcesses;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.DataAccessLayer.DTOs.Dealership;
import com.pitstop.DataAccessLayer.DataAdapters.LocalShopAdapter;
import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.DataAccessLayer.ServerAccess.RequestError;
import com.pitstop.R;
import com.pitstop.adapters.DealershipSelectAdapter;
import com.pitstop.utils.BSAbstractedFragmentActivity;
import com.pitstop.utils.LoadingActivityInterface;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;

import java.util.List;

/**
 * Created by David on 7/22/2016.
 */
public class AddCarChooseDealershipFragment extends Fragment implements DealershipSelectAdapter.DealershipSelectAdapterCallback {

    private Dealership shop;

    private static final String TAG = AddCarChooseDealershipFragment.class.getSimpleName();
    ViewGroup rootView;
    private DealershipSelectAdapter adapter;
    private RecyclerView recyclerView;
    private LocalShopAdapter localStore;
    private boolean hadInternetConnection;
    private NetworkHelper networkHelper;

    private LoadingActivityInterface callback;

    public AddCarChooseDealershipFragment setCallbackActivity(LoadingActivityInterface activityCallback){
        callback = activityCallback;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_add_car_dealership_xml, container, false);

        localStore = new LocalShopAdapter(getContext());
        networkHelper = new NetworkHelper(getActivity().getApplicationContext());
        setup();
        return rootView;
    }

    public Dealership getShop() {
        return shop;
    }



    private void setup() {
        recyclerView = (RecyclerView) rootView.findViewById(R.id.dealership_recycler_list);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        if(callback==null&&getActivity() instanceof AddCarActivity ){
            callback=((AddCarActivity)getActivity());
        }
        callback.showLoading("Loading");

        if(NetworkHelper.isConnected(getContext())) {
            Log.i(TAG, "Internet connection found");
            hadInternetConnection = true;

            List<Dealership> dealerships = localStore.getAllDealerships();
            if (dealerships.isEmpty()) {
                networkHelper.getShops(new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            callback.hideLoading(null);
                            Log.i(TAG, "Get shops response: " + response);
                            try {
                                List<Dealership> list = Dealership.createDealershipList(response);
                                localStore.deleteAllDealerships();
                                localStore.storeDealerships(list);
                                setUpAdapter(list);
                            } catch (JSONException e) {
                                callback.hideLoading("Failed to get dealership info");
                            }
                        } else {
                            callback.hideLoading("Failed to get dealership info");
                        }
                    }
                });

            } else {
                callback.hideLoading(null);
                setUpAdapter(dealerships);
            }
        }else{
            Log.i(TAG, "No internet");
            hadInternetConnection = false;
            if(!localStore.getAllDealerships().isEmpty()) {
                callback.hideLoading(null);
                setUpAdapter(localStore.getAllDealerships());
                callback.hideLoading("Proceed Offline");
            } else {
                callback.hideLoading("No Internet");
            }
        }
        final EditText dealershipQueryEditText = (EditText)rootView.findViewById(R.id.dealership_query);
        dealershipQueryEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                adapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void setUpAdapter(List<Dealership> list) {
        adapter = new DealershipSelectAdapter(list,this);
        recyclerView.setAdapter(adapter);
    }


    @Override
    public void dealershipSelectedCallback(Dealership shop) {
        Log.i(TAG, "Dealership selected: " + shop.getName());
        if(getActivity() instanceof AddCarActivity) {
            try {
                ((AddCarActivity) getActivity()).getMixpanelHelper().trackButtonTapped("Selected " + shop.getName(), TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        this.shop = shop;

    }
}
